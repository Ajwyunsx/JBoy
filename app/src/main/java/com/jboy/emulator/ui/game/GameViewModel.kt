package com.jboy.emulator.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jboy.emulator.core.AudioOutput
import com.jboy.emulator.core.EmulatorCore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelAndJoin
import javax.inject.Inject

data class GameUiState(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val isMuted: Boolean = false,
    val isFastForward: Boolean = false,
    val fastForwardSpeed: Int = 1,
    val targetFps: Int = 60,
    val currentMenu: GameMenuType? = null,
    val lastSaveSlot: Int = -1,
    val lastLoadSlot: Int = -1,
    val errorMessage: String? = null
)

enum class GameMenuType {
    MAIN,
    SAVE,
    LOAD,
    SETTINGS
}

@HiltViewModel
class GameViewModel @Inject constructor(
    private val emulatorCore: EmulatorCore
) : ViewModel() {

    private val audioOutput = AudioOutput.getInstance()

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var currentGamePath: String? = null
    private var frameLoopJob: Job? = null
    private var audioPumpJob: Job? = null
    private val _videoFrame = MutableStateFlow<ByteArray?>(null)
    val videoFrame: StateFlow<ByteArray?> = _videoFrame.asStateFlow()

    private var audioSampleRate: Int = 44100
    private var audioBufferSize: Int = 8192
    private var audioEnabledSetting: Boolean = true
    private var masterVolumeSetting: Float = 100f
    private var audioFilterEnabledSetting: Boolean = true
    private var audioFilterLevelSetting: Int = 60
    private var frameSkipEnabledSetting: Boolean = false
    private var frameSkipThrottlePercentSetting: Int = 33
    private var frameSkipIntervalSetting: Int = 0
    private var interframeBlendingSetting: Boolean = false
    private var idleLoopRemovalSetting: String = "REMOVE_KNOWN"
    private var gbControllerRumbleSetting: Boolean = false
    private var activeCheatCodes: List<String> = emptyList()

    fun loadGame(gamePath: String) {
        if (
            currentGamePath == gamePath &&
            emulatorCore.isInitialized() &&
            emulatorCore.isRomLoaded()
        ) {
            _uiState.value = _uiState.value.copy(isPlaying = true, errorMessage = null)
            if (frameLoopJob?.isActive != true) {
                startFrameLoop()
            }
            if (audioPumpJob?.isActive != true) {
                startAudioPumpLoop()
            }
            if (!_uiState.value.isPaused && audioEnabledSetting && !_uiState.value.isMuted) {
                audioOutput.start()
            }
            return
        }

        currentGamePath = gamePath
        viewModelScope.launch {
            try {
                val initialized = if (emulatorCore.isInitialized()) {
                    true
                } else {
                    emulatorCore.init()
                }
                if (!initialized) {
                    _uiState.value = _uiState.value.copy(
                        isPlaying = false,
                        errorMessage = "模拟器核心初始化失败"
                    )
                    return@launch
                }

                emulatorCore.setAudioConfig(audioSampleRate, audioBufferSize)
                emulatorCore.setGameOptions(
                    frameSkipEnabled = frameSkipEnabledSetting,
                    frameSkipThrottlePercent = frameSkipThrottlePercentSetting,
                    frameSkipInterval = frameSkipIntervalSetting,
                    interframeBlending = interframeBlendingSetting,
                    idleLoopRemoval = idleLoopRemovalSetting,
                    gbControllerRumble = gbControllerRumbleSetting
                )
                val loaded = emulatorCore.loadGame(gamePath)
                if (!loaded) {
                    _uiState.value = _uiState.value.copy(
                        isPlaying = false,
                        errorMessage = "ROM 加载失败"
                    )
                    return@launch
                }

                applyCurrentCheatsToCore()

                _uiState.value = _uiState.value.copy(
                    isPlaying = true,
                    isPaused = false,
                    errorMessage = null
                )
                if (audioOutput.init(audioSampleRate, audioBufferSize)) {
                    val finalEnabled = audioEnabledSetting && !_uiState.value.isMuted
                    audioOutput.setAudioEnabled(finalEnabled)
                    audioOutput.setVolume((masterVolumeSetting / 100f).coerceIn(0f, 1f))
                    audioOutput.setAudioFilterConfig(audioFilterEnabledSetting, audioFilterLevelSetting)
                    if (finalEnabled) {
                        audioOutput.start()
                    }
                }
                startFrameLoop()
                startAudioPumpLoop()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "无法加载游戏: ${e.message}"
                )
            }
        }
    }

    private fun startFrameLoop() {
        frameLoopJob?.cancel()
        frameLoopJob = viewModelScope.launch(Dispatchers.Default) {
            var nextEmuTick = System.nanoTime()
            var nextRenderTick = nextEmuTick
            var frameCounter = 0L
            while (isActive && _uiState.value.isPlaying) {
                if (_uiState.value.isPaused) {
                    delay(8)
                    nextEmuTick = System.nanoTime()
                    nextRenderTick = nextEmuTick
                    continue
                }

                val speed = _uiState.value.fastForwardSpeed.coerceAtLeast(1)
                val fps = _uiState.value.targetFps.coerceIn(30, 120)
                val emuFrameNs = (1_000_000_000L / (60 * speed)).coerceAtLeast(1_000_000L)
                val renderFrameNs = (1_000_000_000L / fps).coerceAtLeast(1_000_000L)

                var now = System.nanoTime()
                var emuSteps = 0
                while (now >= nextEmuTick && emuSteps < 6) {
                    emulatorCore.runFrame()
                    emuSteps++
                    frameCounter++
                    nextEmuTick += emuFrameNs
                    now = System.nanoTime()
                }

                val backlogPercent = (emuSteps * 100) / 6
                val shouldSkipRender =
                    frameSkipEnabledSetting &&
                        frameSkipIntervalSetting > 0 &&
                        backlogPercent >= frameSkipThrottlePercentSetting &&
                        (frameCounter % (frameSkipIntervalSetting + 1) != 0L)

                if (now >= nextRenderTick && !shouldSkipRender) {
                    emulatorCore.getVideoFrame()?.let { frame ->
                        _videoFrame.value = frame
                    }
                    nextRenderTick += renderFrameNs
                } else if (now >= nextRenderTick) {
                    nextRenderTick += renderFrameNs
                }

                if (emuSteps == 6 && now > nextEmuTick) {
                    nextEmuTick = now
                }
                if (now > nextRenderTick + renderFrameNs * 2) {
                    nextRenderTick = now
                }

                val sleepNs = minOf(nextEmuTick, nextRenderTick) - System.nanoTime()
                if (sleepNs > 1_000_000L) {
                    delay(sleepNs / 1_000_000L)
                } else {
                    delay(1)
                }
            }
        }
    }

    private fun startAudioPumpLoop() {
        audioPumpJob?.cancel()
        audioPumpJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive && _uiState.value.isPlaying) {
                if (_uiState.value.isPaused) {
                    delay(3)
                    continue
                }

                var pulled = false
                var reads = 0
                while (reads < 4) {
                    val audio = emulatorCore.getAudioFrame() ?: break
                    if (audio.isEmpty()) {
                        break
                    }
                    val sourceRate = emulatorCore.getAudioSampleRate().takeIf { it > 0 } ?: 32768
                    audioOutput.writeAudioData(audio, sourceRate)
                    pulled = true
                    reads++
                }

                if (pulled) {
                    delay(1)
                } else {
                    delay(1)
                }
            }
        }
    }

    fun setTargetFps(fps: Int) {
        _uiState.value = _uiState.value.copy(targetFps = fps.coerceIn(30, 120))
    }

    fun applyCheatCodes(codes: List<String>) {
        activeCheatCodes = codes
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        applyCurrentCheatsToCore()
    }

    private fun applyCurrentCheatsToCore() {
        if (!emulatorCore.isInitialized() || !emulatorCore.isRomLoaded()) {
            return
        }
        emulatorCore.clearCheats()
        activeCheatCodes.forEach { code ->
            emulatorCore.addCheatCode(code)
        }
    }

    fun updateAudioConfig(
        sampleRate: Int,
        bufferSize: Int,
        enabled: Boolean,
        masterVolume: Float,
        filterEnabled: Boolean,
        filterLevel: Int
    ) {
        audioSampleRate = sampleRate.coerceIn(8000, 96000)
        audioBufferSize = bufferSize.coerceIn(1024, 65536)
        audioEnabledSetting = enabled
        masterVolumeSetting = masterVolume.coerceIn(0f, 100f)
        audioFilterEnabledSetting = filterEnabled
        audioFilterLevelSetting = filterLevel.coerceIn(0, 100)

        emulatorCore.setAudioConfig(audioSampleRate, audioBufferSize)

        if (_uiState.value.isPlaying) {
            if (audioOutput.init(audioSampleRate, audioBufferSize)) {
                val finalEnabled = audioEnabledSetting && !_uiState.value.isMuted
                audioOutput.setAudioEnabled(finalEnabled)
                audioOutput.setVolume((masterVolumeSetting / 100f).coerceIn(0f, 1f))
                audioOutput.setAudioFilterConfig(audioFilterEnabledSetting, audioFilterLevelSetting)
                if (finalEnabled && !_uiState.value.isPaused) {
                    audioOutput.start()
                }
            }
        }
    }

    fun updateGameOptions(
        frameSkipEnabled: Boolean,
        frameSkipThrottlePercent: Int,
        frameSkipInterval: Int,
        interframeBlending: Boolean,
        idleLoopRemoval: String,
        gbControllerRumble: Boolean
    ) {
        frameSkipEnabledSetting = frameSkipEnabled
        frameSkipThrottlePercentSetting = frameSkipThrottlePercent.coerceIn(0, 100)
        frameSkipIntervalSetting = frameSkipInterval.coerceIn(0, 12)
        interframeBlendingSetting = interframeBlending
        idleLoopRemovalSetting = idleLoopRemoval
        gbControllerRumbleSetting = gbControllerRumble

        emulatorCore.setGameOptions(
            frameSkipEnabled = frameSkipEnabledSetting,
            frameSkipThrottlePercent = frameSkipThrottlePercentSetting,
            frameSkipInterval = frameSkipIntervalSetting,
            interframeBlending = interframeBlendingSetting,
            idleLoopRemoval = idleLoopRemovalSetting,
            gbControllerRumble = gbControllerRumbleSetting
        )
    }

    fun showMenu(menuType: GameMenuType) {
        _uiState.value = _uiState.value.copy(currentMenu = menuType)
    }

    fun hideMenu() {
        _uiState.value = _uiState.value.copy(currentMenu = null)
    }

    fun quickSave() {
        currentGamePath?.let {
            viewModelScope.launch {
                saveState(0)
            }
        }
    }

    fun saveState(slot: Int) {
        currentGamePath?.let {
            viewModelScope.launch(Dispatchers.Default) {
                try {
                    val wasPaused = _uiState.value.isPaused
                    if (!wasPaused) {
                        emulatorCore.pause()
                    }
                    val ok = emulatorCore.saveState(slot)
                    if (!wasPaused) {
                        emulatorCore.resume()
                    }
                    _uiState.value = if (ok) {
                        _uiState.value.copy(
                            lastSaveSlot = slot,
                            errorMessage = null
                        )
                    } else {
                        _uiState.value.copy(errorMessage = "存档失败：槽位不可用")
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "存档失败: ${e.message}"
                    )
                }
            }
        }
    }

    fun loadState(slot: Int) {
        currentGamePath?.let {
            viewModelScope.launch(Dispatchers.Default) {
                try {
                    val wasPaused = _uiState.value.isPaused
                    if (!wasPaused) {
                        emulatorCore.pause()
                    }
                    val ok = emulatorCore.loadState(slot)
                    if (!wasPaused) {
                        emulatorCore.resume()
                    }
                    _uiState.value = if (ok) {
                        _uiState.value.copy(
                            lastLoadSlot = slot,
                            errorMessage = null
                        )
                    } else {
                        _uiState.value.copy(errorMessage = "读档失败：该槽位没有存档")
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "读档失败: ${e.message}"
                    )
                }
            }
        }
    }

    fun setFastForwardSpeed(speed: Int) {
        _uiState.value = _uiState.value.copy(
            fastForwardSpeed = speed,
            isFastForward = speed > 1
        )
    }

    fun togglePauseResume() {
        val paused = !_uiState.value.isPaused
        if (paused) {
            emulatorCore.pause()
            audioOutput.pause()
        } else {
            emulatorCore.resume()
            if (audioEnabledSetting && !_uiState.value.isMuted) {
                audioOutput.resume()
            }
        }
        _uiState.value = _uiState.value.copy(isPaused = paused)
    }

    fun toggleMute() {
        val muted = !_uiState.value.isMuted
        val finalEnabled = audioEnabledSetting && !muted
        audioOutput.setAudioEnabled(finalEnabled)
        if (finalEnabled) {
            audioOutput.setVolume((masterVolumeSetting / 100f).coerceIn(0f, 1f))
            if (!_uiState.value.isPaused && _uiState.value.isPlaying) {
                audioOutput.start()
            }
        }
        _uiState.value = _uiState.value.copy(isMuted = muted)
    }

    fun resetGame() {
        val path = currentGamePath ?: return
        viewModelScope.launch {
            try {
                frameLoopJob?.cancelAndJoin()
                frameLoopJob = null
                audioPumpJob?.cancelAndJoin()
                audioPumpJob = null
                audioOutput.stop()

                emulatorCore.setAudioConfig(audioSampleRate, audioBufferSize)
                emulatorCore.setGameOptions(
                    frameSkipEnabled = frameSkipEnabledSetting,
                    frameSkipThrottlePercent = frameSkipThrottlePercentSetting,
                    frameSkipInterval = frameSkipIntervalSetting,
                    interframeBlending = interframeBlendingSetting,
                    idleLoopRemoval = idleLoopRemovalSetting,
                    gbControllerRumble = gbControllerRumbleSetting
                )
                val loaded = emulatorCore.loadGame(path)
                if (!loaded) {
                    _uiState.value = _uiState.value.copy(errorMessage = "重置失败：ROM重载失败")
                    return@launch
                }

                applyCurrentCheatsToCore()

                if (audioOutput.init(audioSampleRate, audioBufferSize)) {
                    val finalEnabled = audioEnabledSetting && !_uiState.value.isMuted
                    audioOutput.setAudioEnabled(finalEnabled)
                    audioOutput.setVolume((masterVolumeSetting / 100f).coerceIn(0f, 1f))
                    audioOutput.setAudioFilterConfig(audioFilterEnabledSetting, audioFilterLevelSetting)
                    if (finalEnabled) {
                        audioOutput.start()
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isPaused = false,
                    errorMessage = null
                )
                startFrameLoop()
                startAudioPumpLoop()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "重置失败: ${e.message}")
            }
        }
    }

    fun exitGame(onFinished: (() -> Unit)? = null) {
        viewModelScope.launch {
            frameLoopJob?.cancelAndJoin()
            frameLoopJob = null
            audioPumpJob?.cancelAndJoin()
            audioPumpJob = null
            runCatching { audioOutput.stop() }
            runCatching { emulatorCore.stopGame() }
            runCatching { emulatorCore.cleanup() }
            _videoFrame.value = null
            _uiState.value = GameUiState(isPlaying = false, isPaused = false, isMuted = false)
            currentGamePath = null
            onFinished?.invoke()
        }
    }

    fun onButtonPress(button: GameButton) {
        emulatorCore.setButtonPressed(button.ordinal, true)
    }

    fun onButtonRelease(button: GameButton) {
        emulatorCore.setButtonPressed(button.ordinal, false)
    }

    fun onDpadChange(_direction: DpadDirection) {
        // Handle d-pad input
    }

    override fun onCleared() {
        super.onCleared()
        frameLoopJob?.cancel()
        audioPumpJob?.cancel()
        runCatching { audioOutput.cleanup() }
        runCatching { emulatorCore.cleanup() }
    }
}

enum class GameButton {
    A, B, SELECT, START, UP, DOWN, LEFT, RIGHT, L, R
}

enum class DpadDirection {
    UP, DOWN, LEFT, RIGHT, CENTER
}
