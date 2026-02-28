package com.jboy.emulator.core

import android.content.Context
import android.util.Log

class EmulatorCore private constructor() {

    data class NetplayLinkSession(
        val protocol: String,
        val serverAddress: String,
        val roomId: String,
        val nickname: String,
        val connectedPeers: Int,
        val readyPeers: Int,
        val canStartLink: Boolean
    )

    companion object {
        private const val TAG = "EmulatorCore"
        
        @Volatile
        private var instance: EmulatorCore? = null
        
        fun getInstance(): EmulatorCore {
            return instance ?: synchronized(this) {
                instance ?: EmulatorCore().also { instance = it }
            }
        }
        
        init {
            try {
                System.loadLibrary("jboy-core")
                Log.d(TAG, "Native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library: ${e.message}")
            }
        }
    }

    // Native methods - matching JNI interface
    external fun nativeInit(): Boolean
    external fun nativeLoadRom(romPath: String): Boolean
    external fun nativeRunFrame()
    external fun nativeSetInput(buttons: Int)
    external fun nativeSetAudioConfig(sampleRate: Int, bufferSize: Int)
    external fun nativeSetGameOptions(
        frameSkipEnabled: Boolean,
        frameSkipThrottlePercent: Int,
        frameSkipInterval: Int,
        interframeBlending: Boolean,
        idleLoopRemoval: String,
        gbControllerRumble: Boolean
    )
    external fun nativeSaveState(slot: Int): Boolean
    external fun nativeLoadState(slot: Int): Boolean
    external fun nativeHasSaveState(slot: Int): Boolean
    external fun nativeCleanup()
    external fun nativeIsPaused(): Boolean
    external fun nativePause()
    external fun nativeResume()
    external fun nativeReset()
    external fun nativeGetRomTitle(): String
    external fun nativeGetAudioSampleRate(): Int
    external fun nativeGetVideoFrame(): ByteArray?
    external fun nativeGetAudioFrame(): ShortArray?
    external fun nativeClearCheats()
    external fun nativeAddCheatCode(code: String): Boolean

    // State callback interface
    interface StateCallback {
        fun onFrameRendered(frameBuffer: ByteArray)
        fun onAudioReady(audioBuffer: ShortArray)
        fun onError(error: String)
    }
    
    private var stateCallback: StateCallback? = null
    private var isInitialized = false
    private var isRomLoaded = false
    private var isPaused = false
    private var currentButtons = 0
    private var pendingAudioSampleRate = 44100
    private var pendingAudioBufferSize = 8192
    private var activeNetplayLinkSession: NetplayLinkSession? = null

    fun init(): Boolean {
        if (isInitialized) {
            Log.w(TAG, "Emulator already initialized")
            return true
        }
        
        isInitialized = nativeInit()
        if (isInitialized) {
            nativeSetAudioConfig(pendingAudioSampleRate, pendingAudioBufferSize)
        }
        Log.d(TAG, "Emulator initialization result: $isInitialized")
        return isInitialized
    }
    
    fun loadRom(romPath: String): Boolean {
        if (!isInitialized) {
            Log.e(TAG, "Cannot load ROM - emulator not initialized")
            return false
        }
        
        isRomLoaded = nativeLoadRom(romPath)
        if (isRomLoaded) {
            activeNetplayLinkSession?.let { session ->
                Log.i(
                    TAG,
                    "Applying netplay session protocol=${session.protocol} room=${session.roomId} player=${session.nickname} peers=${session.connectedPeers} ready=${session.readyPeers}"
                )
            }
            Log.d(TAG, "ROM loaded: $romPath")
            return true
        }
        Log.e(TAG, "ROM load failed: $romPath")
        return false
    }
    
    fun runFrame() {
        if (!isInitialized || !isRomLoaded || isPaused) {
            return
        }
        
        nativeRunFrame()
    }
    
    fun setInput(buttons: Int) {
        if (isInitialized) {
            nativeSetInput(buttons)
        }
    }

    fun setInputState(buttons: Int) {
        setInput(buttons)
    }

    fun setAudioConfig(sampleRate: Int, bufferSize: Int) {
        pendingAudioSampleRate = sampleRate.coerceIn(8000, 96000)
        pendingAudioBufferSize = bufferSize.coerceIn(1024, 65536)
        if (isInitialized) {
            nativeSetAudioConfig(pendingAudioSampleRate, pendingAudioBufferSize)
        }
    }

    fun setGameOptions(
        frameSkipEnabled: Boolean,
        frameSkipThrottlePercent: Int,
        frameSkipInterval: Int,
        interframeBlending: Boolean,
        idleLoopRemoval: String,
        gbControllerRumble: Boolean
    ) {
        if (!isInitialized) {
            return
        }
        nativeSetGameOptions(
            frameSkipEnabled = frameSkipEnabled,
            frameSkipThrottlePercent = frameSkipThrottlePercent.coerceIn(0, 100),
            frameSkipInterval = frameSkipInterval.coerceIn(0, 12),
            interframeBlending = interframeBlending,
            idleLoopRemoval = idleLoopRemoval,
            gbControllerRumble = gbControllerRumble
        )
    }

    fun loadGame(gamePath: String): Boolean {
        return loadRom(gamePath)
    }

    fun stopGame() {
        pause()
    }

    fun setButtonPressed(buttonIndex: Int, pressed: Boolean) {
        val mask = when (buttonIndex) {
            0 -> 0x001
            1 -> 0x002
            2 -> 0x004
            3 -> 0x008
            4 -> 0x040
            5 -> 0x080
            6 -> 0x020
            7 -> 0x010
            8 -> 0x200
            9 -> 0x100
            else -> 0
        }
        if (mask == 0) return
        currentButtons = if (pressed) {
            currentButtons or mask
        } else {
            currentButtons and mask.inv()
        }
        setInput(currentButtons)
    }
    
    fun saveState(slot: Int): Boolean {
        return if (isInitialized && isRomLoaded) {
            nativeSaveState(slot)
        } else {
            false
        }
    }
    
    fun loadState(slot: Int): Boolean {
        return if (isInitialized && isRomLoaded) {
            nativeLoadState(slot)
        } else {
            false
        }
    }
    
    fun hasSaveState(slot: Int): Boolean {
        return if (isInitialized && isRomLoaded) {
            nativeHasSaveState(slot)
        } else {
            false
        }
    }
    
    fun pause() {
        if (isInitialized && isRomLoaded) {
            nativePause()
            isPaused = true
        }
    }
    
    fun resume() {
        if (isInitialized && isRomLoaded) {
            nativeResume()
            isPaused = false
        }
    }
    
    fun reset() {
        if (isInitialized && isRomLoaded) {
            nativeReset()
        }
    }
    
    fun cleanup() {
        if (isInitialized) {
            nativeCleanup()
            isInitialized = false
            isRomLoaded = false
            isPaused = false
            stateCallback = null
            Log.d(TAG, "Emulator cleaned up")
        }
    }
    
    fun setStateCallback(callback: StateCallback?) {
        this.stateCallback = callback
    }
    
    fun isInitialized(): Boolean = isInitialized
    
    fun isRomLoaded(): Boolean = isRomLoaded
    
    fun isPaused(): Boolean = isPaused

    fun getRomTitle(): String {
        return if (isInitialized) {
            nativeGetRomTitle()
        } else {
            ""
        }
    }

    fun getVideoFrame(): ByteArray? {
        return if (isInitialized && isRomLoaded && !isPaused) {
            nativeGetVideoFrame()
        } else {
            null
        }
    }

    fun getAudioFrame(): ShortArray? {
        return if (isInitialized && isRomLoaded && !isPaused) {
            nativeGetAudioFrame()
        } else {
            null
        }
    }

    fun getAudioSampleRate(): Int {
        return if (isInitialized) {
            nativeGetAudioSampleRate()
        } else {
            0
        }
    }

    fun clearCheats() {
        if (isInitialized && isRomLoaded) {
            nativeClearCheats()
        }
    }

    fun addCheatCode(code: String): Boolean {
        if (!isInitialized || !isRomLoaded) {
            return false
        }
        val trimmed = code.trim()
        if (trimmed.isEmpty()) {
            return false
        }
        return nativeAddCheatCode(trimmed)
    }

    fun setNetplayLinkSession(session: NetplayLinkSession?) {
        activeNetplayLinkSession = session
        if (session == null) {
            Log.i(TAG, "Netplay link session cleared")
            return
        }
        Log.i(
            TAG,
            "Netplay link session updated protocol=${session.protocol} room=${session.roomId} player=${session.nickname} peers=${session.connectedPeers} ready=${session.readyPeers} canStart=${session.canStartLink}"
        )
    }

    fun getNetplayLinkSession(): NetplayLinkSession? = activeNetplayLinkSession
}
