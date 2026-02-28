package com.jboy.emulator.core

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import android.util.Log
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class AudioOutput private constructor() {

    companion object {
        private const val TAG = "AudioOutput"
        const val SAMPLE_RATE = 44100
        const val CHANNELS = 2
        const val BUFFER_SIZE_FRAMES = 8192
        private const val QUEUE_CAPACITY = 32
        private const val WRITE_CHUNK_FRAMES = 640
        private const val TARGET_QUEUE_LATENCY_MS = 85
        private const val HARD_QUEUE_LATENCY_MS = 145
        private const val EDGE_SMOOTH_FRAMES = 24
        private const val EDGE_SMOOTH_THRESHOLD = 8000

        @Volatile
        private var instance: AudioOutput? = null

        fun getInstance(): AudioOutput {
            return instance ?: synchronized(this) {
                instance ?: AudioOutput().also { instance = it }
            }
        }
    }

    private var audioTrack: AudioTrack? = null
    private var initialized = false
    private var playing = false
    private var volume = 1.0f
    private var audioEnabled = true
    private var audioThread: Thread? = null
    private val audioQueue: BlockingQueue<ShortArray> = ArrayBlockingQueue(QUEUE_CAPACITY)
    private val queuedSamples = AtomicInteger(0)
    private var resamplePhase = 0.0
    private var resamplePrevLeft = 0
    private var resamplePrevRight = 0
    private var resampleHasPrevFrame = false
    private var resampleInRate = 0
    private var resampleOutRate = 0
    private var hasChunkTail = false
    private var lastChunkTailLeft = 0
    private var lastChunkTailRight = 0
    private var audioFilterEnabled = true
    private var audioFilterLevel = 60
    private var filterStateLeft = 0f
    private var filterStateRight = 0f
    private var filterStateReady = false

    private var outputSampleRate: Int = SAMPLE_RATE
    private var outputBufferFrames: Int = BUFFER_SIZE_FRAMES

    @Synchronized
    fun init(sampleRate: Int = SAMPLE_RATE, bufferFrames: Int = BUFFER_SIZE_FRAMES): Boolean {
        val safeRate = sampleRate.coerceIn(8000, 96000)
        val safeFrames = bufferFrames.coerceIn(1024, 65536)

        if (initialized && outputSampleRate == safeRate && outputBufferFrames == safeFrames) {
            return true
        }
        if (initialized) {
            cleanup()
        }

        return try {
            val minBuffer = AudioTrack.getMinBufferSize(
                safeRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val targetBytes = safeFrames * CHANNELS * 2
            val finalBufferBytes = minBuffer.coerceAtLeast(targetBytes)

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(safeRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build()

            val performanceMode = if (safeFrames <= 4096) {
                AudioTrack.PERFORMANCE_MODE_LOW_LATENCY
            } else {
                AudioTrack.PERFORMANCE_MODE_NONE
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(finalBufferBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setPerformanceMode(performanceMode)
                .build()

            if (track.state != AudioTrack.STATE_INITIALIZED) {
                track.release()
                Log.e(TAG, "AudioTrack init failed: invalid state")
                return false
            }

            audioTrack = track
            initialized = true
            outputSampleRate = safeRate
            outputBufferFrames = safeFrames
            clearQueuedAudio()
            setVolume(volume)
            Log.d(TAG, "Audio initialized rate=$outputSampleRate bufferFrames=$outputBufferFrames")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Audio init failed: ${e.message}")
            false
        }
    }

    @Synchronized
    fun start() {
        if (!initialized || playing || !audioEnabled) return
        try {
            audioTrack?.play()
            playing = true
            startAudioThreadIfNeeded()
        } catch (e: Exception) {
            Log.e(TAG, "Audio start failed: ${e.message}")
        }
    }

    @Synchronized
    fun stop() {
        if (!initialized) return
        playing = false
        audioThread?.interrupt()
        audioThread = null
        clearQueuedAudio()

        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Audio stop error: ${e.message}")
        }
    }

    @Synchronized
    fun pause() {
        if (!initialized || !playing) return
        playing = false
        audioThread?.interrupt()
        audioThread = null
        try {
            audioTrack?.pause()
        } catch (e: Exception) {
            Log.e(TAG, "Audio pause error: ${e.message}")
        }
    }

    @Synchronized
    fun resume() {
        if (!initialized || playing || !audioEnabled) return
        try {
            audioTrack?.play()
            playing = true
            startAudioThreadIfNeeded()
        } catch (e: Exception) {
            Log.e(TAG, "Audio resume error: ${e.message}")
        }
    }

    @Synchronized
    fun writeAudioData(audioData: ShortArray, sourceSampleRate: Int) {
        if (!initialized || !audioEnabled || audioData.isEmpty()) return

        val shouldStartAfterEnqueue = !playing

        val volumeScaled = applyVolume(audioData)
        val processed = if (sourceSampleRate > 0 && sourceSampleRate != outputSampleRate) {
            resampleStereo(volumeScaled, sourceSampleRate, outputSampleRate)
        } else {
            resetResamplerState()
            volumeScaled
        }
        val filtered = applyAudioFilter(processed)

        val chunkSamples = WRITE_CHUNK_FRAMES * CHANNELS
        var offset = 0
        var startPending = shouldStartAfterEnqueue
        while (offset < filtered.size) {
            val size = min(chunkSamples, filtered.size - offset)
            val chunk = ShortArray(size)
            filtered.copyInto(chunk, destinationOffset = 0, startIndex = offset, endIndex = offset + size)
            enqueueChunk(chunk)
            if (startPending && !playing) {
                start()
                startPending = false
            }
            offset += size
        }
    }

    fun setVolume(vol: Float) {
        volume = vol.coerceIn(0f, 1f)
        try {
            audioTrack?.setVolume(volume)
        } catch (e: Exception) {
            Log.e(TAG, "Audio set volume error: ${e.message}")
        }
    }

    fun getVolume(): Float = volume

    @Synchronized
    fun setAudioFilterConfig(enabled: Boolean, level: Int) {
        audioFilterEnabled = enabled
        audioFilterLevel = level.coerceIn(0, 100)
        if (!audioFilterEnabled) {
            filterStateReady = false
            filterStateLeft = 0f
            filterStateRight = 0f
        }
    }

    @Synchronized
    fun setAudioEnabled(enabled: Boolean) {
        audioEnabled = enabled
        if (!enabled) {
            clearQueuedAudio()
            pause()
        }
    }

    fun isAudioEnabled(): Boolean = audioEnabled

    fun isPlaying(): Boolean = playing

    @Synchronized
    fun cleanup() {
        stop()
        try {
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Audio release error: ${e.message}")
        }
        audioTrack = null
        initialized = false
    }

    private fun startAudioThreadIfNeeded() {
        if (audioThread?.isAlive == true) return
        audioThread = thread(name = "AudioOutputThread", isDaemon = true) {
            runCatching { Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO) }
            while (!Thread.currentThread().isInterrupted) {
                if (!playing) break
                try {
                    val chunk = audioQueue.poll(2, TimeUnit.MILLISECONDS)
                    if (chunk != null) {
                        decreaseQueuedSamples(chunk.size)
                        writeChunkBlocking(chunk)
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                } catch (e: Exception) {
                    Log.e(TAG, "Audio write thread error: ${e.message}")
                }
            }
        }
    }

    private fun writeChunkBlocking(chunk: ShortArray) {
        var writtenOffset = 0
        while (writtenOffset < chunk.size && playing) {
            val written = audioTrack?.write(
                chunk,
                writtenOffset,
                chunk.size - writtenOffset,
                AudioTrack.WRITE_BLOCKING
            ) ?: 0
            if (written <= 0) {
                break
            }
            writtenOffset += written
        }
    }

    private fun enqueueChunk(chunk: ShortArray) {
        smoothChunkBoundary(chunk)
        trimQueueForLatency(chunk.size)
        try {
            var offered = audioQueue.offer(chunk, 2, TimeUnit.MILLISECONDS)
            if (!offered) {
                dropOldestChunk()
                offered = audioQueue.offer(chunk)
            }
            if (offered) {
                queuedSamples.addAndGet(chunk.size)
            }
        } catch (_: InterruptedException) {
        }
    }

    private fun trimQueueForLatency(incomingSamples: Int) {
        val hardLimit = maxQueuedSamplesHardLimit()
        while (queuedSamples.get() + incomingSamples > hardLimit) {
            if (!dropOldestChunk()) {
                break
            }
        }

        val target = targetQueuedSamples()
        if (queuedSamples.get() > target + incomingSamples) {
            dropOldestChunk()
        }
    }

    private fun targetQueuedSamples(): Int {
        val byLatency = (outputSampleRate * CHANNELS * TARGET_QUEUE_LATENCY_MS) / 1000
        return byLatency.coerceAtLeast(WRITE_CHUNK_FRAMES * CHANNELS)
    }

    private fun maxQueuedSamplesHardLimit(): Int {
        val byLatency = (outputSampleRate * CHANNELS * HARD_QUEUE_LATENCY_MS) / 1000
        return byLatency.coerceAtLeast(WRITE_CHUNK_FRAMES * CHANNELS * 2)
    }

    private fun dropOldestChunk(): Boolean {
        val dropped = audioQueue.poll() ?: return false
        decreaseQueuedSamples(dropped.size)
        return true
    }

    private fun clearQueuedAudio() {
        while (true) {
            val dropped = audioQueue.poll() ?: break
            decreaseQueuedSamples(dropped.size)
        }
        queuedSamples.set(0)
        resetResamplerState()
    }

    private fun resetResamplerState() {
        resamplePhase = 0.0
        resamplePrevLeft = 0
        resamplePrevRight = 0
        resampleHasPrevFrame = false
        resampleInRate = 0
        resampleOutRate = 0
        hasChunkTail = false
        lastChunkTailLeft = 0
        lastChunkTailRight = 0
        filterStateReady = false
        filterStateLeft = 0f
        filterStateRight = 0f
    }

    private fun smoothChunkBoundary(chunk: ShortArray) {
        if (chunk.size < 4) {
            return
        }

        if (hasChunkTail) {
            val startL = chunk[0].toInt()
            val startR = chunk[1].toInt()
            val needSmooth =
                abs(startL - lastChunkTailLeft) >= EDGE_SMOOTH_THRESHOLD ||
                    abs(startR - lastChunkTailRight) >= EDGE_SMOOTH_THRESHOLD

            if (needSmooth) {
                val frameCount = min(EDGE_SMOOTH_FRAMES, chunk.size / 2)

                for (i in 0 until frameCount) {
                    val t = (i + 1).toFloat() / frameCount.toFloat()
                    val l = (lastChunkTailLeft + (startL - lastChunkTailLeft) * t).toInt()
                    val r = (lastChunkTailRight + (startR - lastChunkTailRight) * t).toInt()
                    val sampleIndex = i * 2
                    chunk[sampleIndex] = l.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    chunk[sampleIndex + 1] = r.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }
        }

        val tailIndex = chunk.size - 2
        lastChunkTailLeft = chunk[tailIndex].toInt()
        lastChunkTailRight = chunk[tailIndex + 1].toInt()
        hasChunkTail = true
    }

    private fun applyAudioFilter(input: ShortArray): ShortArray {
        if (!audioFilterEnabled || input.isEmpty()) {
            return input
        }

        val strength = audioFilterLevel.coerceIn(0, 100) / 100f
        val alpha = (0.86f - strength * 0.62f).coerceIn(0.08f, 0.9f)
        val output = ShortArray(input.size)

        var stateL = filterStateLeft
        var stateR = filterStateRight
        var stateReady = filterStateReady

        var i = 0
        while (i + 1 < input.size) {
            val inL = input[i].toFloat()
            val inR = input[i + 1].toFloat()

            if (!stateReady) {
                stateL = inL
                stateR = inR
                stateReady = true
            } else {
                stateL += alpha * (inL - stateL)
                stateR += alpha * (inR - stateR)
            }

            output[i] = stateL.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            output[i + 1] = stateR.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            i += 2
        }

        filterStateLeft = stateL
        filterStateRight = stateR
        filterStateReady = stateReady
        return output
    }

    private fun decreaseQueuedSamples(amount: Int) {
        while (true) {
            val current = queuedSamples.get()
            val next = (current - amount).coerceAtLeast(0)
            if (queuedSamples.compareAndSet(current, next)) {
                return
            }
        }
    }

    private fun applyVolume(input: ShortArray): ShortArray {
        if (volume >= 0.999f) {
            return input
        }
        val scale = volume
        return ShortArray(input.size) { i ->
            val mixed = (input[i] * scale).toInt()
            mixed.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private fun resampleStereo(input: ShortArray, inRate: Int, outRate: Int): ShortArray {
        val inFrames = input.size / 2
        if (inFrames < 2 || inRate <= 0 || outRate <= 0) {
            return input
        }

        val usePrevFrame =
            resampleHasPrevFrame &&
                resampleInRate == inRate &&
                resampleOutRate == outRate

        val virtualFrames = inFrames + if (usePrevFrame) 1 else 0
        val step = inRate.toDouble() / outRate.toDouble()
        var srcPos = if (usePrevFrame) resamplePhase else 0.0

        val estimatedOutFrames = max(1, ((virtualFrames - srcPos) / step).toInt() + 2)
        val output = ShortArray(estimatedOutFrames * 2)

        var outFrameIndex = 0
        while (srcPos + 1.0 < virtualFrames) {
            val base = srcPos.toInt().coerceIn(0, virtualFrames - 2)
            val frac = srcPos - base

            val l0: Int
            val r0: Int
            if (usePrevFrame && base == 0) {
                l0 = resamplePrevLeft
                r0 = resamplePrevRight
            } else {
                val srcBase = if (usePrevFrame) (base - 1) * 2 else base * 2
                l0 = input[srcBase].toInt()
                r0 = input[srcBase + 1].toInt()
            }

            val nextFrame = base + 1
            val l1: Int
            val r1: Int
            if (usePrevFrame && nextFrame == 0) {
                l1 = resamplePrevLeft
                r1 = resamplePrevRight
            } else {
                val srcNext = if (usePrevFrame) (nextFrame - 1) * 2 else nextFrame * 2
                l1 = input[srcNext].toInt()
                r1 = input[srcNext + 1].toInt()
            }

            val l = (l0 + (l1 - l0) * frac).toInt()
            val r = (r0 + (r1 - r0) * frac).toInt()

            val sampleIndex = outFrameIndex * 2
            if (sampleIndex + 1 >= output.size) {
                break
            }
            output[sampleIndex] = l.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            output[sampleIndex + 1] = r.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

            srcPos += step
            outFrameIndex++
        }

        resamplePhase = (srcPos - (virtualFrames - 1)).coerceIn(0.0, 1.0)
        resamplePrevLeft = input[(inFrames - 1) * 2].toInt()
        resamplePrevRight = input[(inFrames - 1) * 2 + 1].toInt()
        resampleHasPrevFrame = true
        resampleInRate = inRate
        resampleOutRate = outRate

        return output.copyOf(outFrameIndex * 2)
    }
}
