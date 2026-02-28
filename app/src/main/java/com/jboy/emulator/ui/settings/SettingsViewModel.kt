package com.jboy.emulator.ui.settings

import android.app.Application
import android.view.KeyEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jboy.emulator.data.SettingsRepository
import com.jboy.emulator.data.SettingsDataStore
import com.jboy.emulator.ui.gamepad.DpadMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AppSettings(
    // 视频设置
    val videoFilter: VideoFilter = VideoFilter.NEAREST,
    val aspectRatio: AspectRatio = AspectRatio.ORIGINAL,
    val showFps: Boolean = false,
    val themeCustomEnabled: Boolean = false,
    val themePreset: ThemePreset = ThemePreset.JBOY_CLASSIC,
    val themePrimaryHex: String = "#00696B",
    val themeSecondaryHex: String = "#4A6364",
    val themeTertiaryHex: String = "#4B607B",
    val themeBackgroundHex: String = "#FAFDFC",
    val themeSurfaceHex: String = "#FAFDFC",
    
    // 音频设置
    val audioEnabled: Boolean = true,
    val masterVolume: Float = 100f,
    val audioSampleRate: Int = 44100,
    val audioBufferSize: Int = 8192,
    val audioFilterEnabled: Boolean = true,
    val audioFilterLevel: Int = 60,
    
    // 控制设置
    val controllerOpacity: Float = 0.8f,
    val buttonSize: Float = 1f,
    val dpadMode: DpadMode = DpadMode.WHEEL,
    val vibrationEnabled: Boolean = true,
    val gbControllerRumble: Boolean = false,
    val frameSkipEnabled: Boolean = false,
    val frameSkipThrottlePercent: Int = 33,
    val frameSkipInterval: Int = 0,
    val interframeBlending: Boolean = false,
    val idleLoopRemoval: IdleLoopRemovalMode = IdleLoopRemovalMode.REMOVE_KNOWN,
    val keyMapA: VirtualKeyTarget = VirtualKeyTarget.A,
    val keyMapB: VirtualKeyTarget = VirtualKeyTarget.B,
    val keyMapUp: VirtualKeyTarget = VirtualKeyTarget.UP,
    val keyMapDown: VirtualKeyTarget = VirtualKeyTarget.DOWN,
    val keyMapLeft: VirtualKeyTarget = VirtualKeyTarget.LEFT,
    val keyMapRight: VirtualKeyTarget = VirtualKeyTarget.RIGHT,
    val keyMapL: VirtualKeyTarget = VirtualKeyTarget.L,
    val keyMapR: VirtualKeyTarget = VirtualKeyTarget.R,
    val keyMapStart: VirtualKeyTarget = VirtualKeyTarget.START,
    val keyMapSelect: VirtualKeyTarget = VirtualKeyTarget.SELECT,
    val hardwareKeyA: Int = KeyEvent.KEYCODE_BUTTON_A,
    val hardwareKeyB: Int = KeyEvent.KEYCODE_BUTTON_B,
    val hardwareKeyUp: Int = KeyEvent.KEYCODE_DPAD_UP,
    val hardwareKeyDown: Int = KeyEvent.KEYCODE_DPAD_DOWN,
    val hardwareKeyLeft: Int = KeyEvent.KEYCODE_DPAD_LEFT,
    val hardwareKeyRight: Int = KeyEvent.KEYCODE_DPAD_RIGHT,
    val hardwareKeyL: Int = KeyEvent.KEYCODE_BUTTON_L1,
    val hardwareKeyR: Int = KeyEvent.KEYCODE_BUTTON_R1,
    val hardwareKeyStart: Int = KeyEvent.KEYCODE_BUTTON_START,
    val hardwareKeySelect: Int = KeyEvent.KEYCODE_BUTTON_SELECT,
    
    // 系统设置
    val biosPath: String? = null,
    val fastForwardSpeed: FastForwardSpeed = FastForwardSpeed.X2,
    val language: String = "zh-CN",
    val immersiveMode: Boolean = false,
    val autoSaveOnExit: Boolean = true,
    val pauseOnBackground: Boolean = true,
    val showVirtualControls: Boolean = true
)

class SettingsViewModel(
    application: Application
) : AndroidViewModel(application) {
    
    private val repository: SettingsRepository = SettingsRepository(
        SettingsDataStore(application)
    )
    
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    
    init {
        viewModelScope.launch {
            repository.settingsFlow.collect { data ->
                _settings.value = AppSettings(
                    videoFilter = VideoFilter.valueOf(data.videoFilter),
                    aspectRatio = AspectRatio.valueOf(data.aspectRatio),
                    showFps = data.showFps,
                    themeCustomEnabled = data.themeCustomEnabled,
                    themePreset = runCatching { ThemePreset.valueOf(data.themePreset) }
                        .getOrDefault(ThemePreset.JBOY_CLASSIC),
                    themePrimaryHex = sanitizeHexColor(data.themePrimaryHex, "#00696B"),
                    themeSecondaryHex = sanitizeHexColor(data.themeSecondaryHex, "#4A6364"),
                    themeTertiaryHex = sanitizeHexColor(data.themeTertiaryHex, "#4B607B"),
                    themeBackgroundHex = sanitizeHexColor(data.themeBackgroundHex, "#FAFDFC"),
                    themeSurfaceHex = sanitizeHexColor(data.themeSurfaceHex, "#FAFDFC"),
                    audioEnabled = data.audioEnabled,
                    masterVolume = data.masterVolume,
                    audioSampleRate = data.audioSampleRate,
                    audioBufferSize = data.audioBufferSize,
                    audioFilterEnabled = data.audioFilterEnabled,
                    audioFilterLevel = data.audioFilterLevel,
                    controllerOpacity = data.controllerOpacity,
                    buttonSize = data.buttonSize,
                    dpadMode = runCatching { DpadMode.valueOf(data.dpadMode) }.getOrDefault(DpadMode.WHEEL),
                    vibrationEnabled = data.vibrationEnabled,
                    gbControllerRumble = data.gbControllerRumble,
                    frameSkipEnabled = data.frameSkipEnabled,
                    frameSkipThrottlePercent = data.frameSkipThrottlePercent,
                    frameSkipInterval = data.frameSkipInterval,
                    interframeBlending = data.interframeBlending,
                    idleLoopRemoval = runCatching { IdleLoopRemovalMode.valueOf(data.idleLoopRemoval) }
                        .getOrDefault(IdleLoopRemovalMode.REMOVE_KNOWN),
                    keyMapA = runCatching { VirtualKeyTarget.valueOf(data.keyMapA) }.getOrDefault(VirtualKeyTarget.A),
                    keyMapB = runCatching { VirtualKeyTarget.valueOf(data.keyMapB) }.getOrDefault(VirtualKeyTarget.B),
                    keyMapUp = runCatching { VirtualKeyTarget.valueOf(data.keyMapUp) }.getOrDefault(VirtualKeyTarget.UP),
                    keyMapDown = runCatching { VirtualKeyTarget.valueOf(data.keyMapDown) }.getOrDefault(VirtualKeyTarget.DOWN),
                    keyMapLeft = runCatching { VirtualKeyTarget.valueOf(data.keyMapLeft) }.getOrDefault(VirtualKeyTarget.LEFT),
                    keyMapRight = runCatching { VirtualKeyTarget.valueOf(data.keyMapRight) }.getOrDefault(VirtualKeyTarget.RIGHT),
                    keyMapL = runCatching { VirtualKeyTarget.valueOf(data.keyMapL) }.getOrDefault(VirtualKeyTarget.L),
                    keyMapR = runCatching { VirtualKeyTarget.valueOf(data.keyMapR) }.getOrDefault(VirtualKeyTarget.R),
                    keyMapStart = runCatching { VirtualKeyTarget.valueOf(data.keyMapStart) }.getOrDefault(VirtualKeyTarget.START),
                    keyMapSelect = runCatching { VirtualKeyTarget.valueOf(data.keyMapSelect) }.getOrDefault(VirtualKeyTarget.SELECT),
                    hardwareKeyA = data.hardwareKeyA,
                    hardwareKeyB = data.hardwareKeyB,
                    hardwareKeyUp = data.hardwareKeyUp,
                    hardwareKeyDown = data.hardwareKeyDown,
                    hardwareKeyLeft = data.hardwareKeyLeft,
                    hardwareKeyRight = data.hardwareKeyRight,
                    hardwareKeyL = data.hardwareKeyL,
                    hardwareKeyR = data.hardwareKeyR,
                    hardwareKeyStart = data.hardwareKeyStart,
                    hardwareKeySelect = data.hardwareKeySelect,
                    biosPath = data.biosPath,
                    fastForwardSpeed = FastForwardSpeed.valueOf(data.fastForwardSpeed),
                    language = data.language,
                    immersiveMode = data.immersiveMode,
                    autoSaveOnExit = data.autoSaveOnExit,
                    pauseOnBackground = data.pauseOnBackground,
                    showVirtualControls = data.showVirtualControls
                )
            }
        }
    }
    
    // 视频设置
    fun updateVideoFilter(filter: VideoFilter) {
        viewModelScope.launch {
            repository.updateVideoFilter(filter.name)
        }
    }
    
    fun updateAspectRatio(ratio: AspectRatio) {
        viewModelScope.launch {
            repository.updateAspectRatio(ratio.name)
        }
    }
    
    fun updateShowFps(show: Boolean) {
        viewModelScope.launch {
            repository.updateShowFps(show)
        }
    }

    fun updateThemeCustomEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateThemeCustomEnabled(enabled)
        }
    }

    fun updateThemePreset(preset: ThemePreset) {
        viewModelScope.launch {
            repository.updateThemePreset(preset.name)
        }
    }

    fun updateThemePrimaryHex(hex: String) {
        viewModelScope.launch {
            repository.updateThemePrimaryHex(sanitizeHexColor(hex, "#00696B"))
        }
    }

    fun updateThemeSecondaryHex(hex: String) {
        viewModelScope.launch {
            repository.updateThemeSecondaryHex(sanitizeHexColor(hex, "#4A6364"))
        }
    }

    fun updateThemeTertiaryHex(hex: String) {
        viewModelScope.launch {
            repository.updateThemeTertiaryHex(sanitizeHexColor(hex, "#4B607B"))
        }
    }

    fun updateThemeBackgroundHex(hex: String) {
        viewModelScope.launch {
            repository.updateThemeBackgroundHex(sanitizeHexColor(hex, "#FAFDFC"))
        }
    }

    fun updateThemeSurfaceHex(hex: String) {
        viewModelScope.launch {
            repository.updateThemeSurfaceHex(sanitizeHexColor(hex, "#FAFDFC"))
        }
    }
    
    // 音频设置
    fun updateAudioEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAudioEnabled(enabled)
        }
    }
    
    fun updateMasterVolume(volume: Float) {
        viewModelScope.launch {
            repository.updateMasterVolume(volume)
        }
    }

    fun updateAudioSampleRate(sampleRate: Int) {
        viewModelScope.launch {
            repository.updateAudioSampleRate(sampleRate)
        }
    }

    fun updateAudioBufferSize(bufferSize: Int) {
        viewModelScope.launch {
            repository.updateAudioBufferSize(bufferSize)
        }
    }

    fun updateAudioFilterEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAudioFilterEnabled(enabled)
        }
    }

    fun updateAudioFilterLevel(level: Int) {
        viewModelScope.launch {
            repository.updateAudioFilterLevel(level)
        }
    }
    
    // 控制设置
    fun updateControllerOpacity(opacity: Float) {
        viewModelScope.launch {
            repository.updateControllerOpacity(opacity)
        }
    }
    
    fun updateButtonSize(size: Float) {
        viewModelScope.launch {
            repository.updateButtonSize(size)
        }
    }

    fun updateDpadMode(mode: DpadMode) {
        viewModelScope.launch {
            repository.updateDpadMode(mode.name)
        }
    }
    
    fun updateVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateVibrationEnabled(enabled)
        }
    }

    fun updateGbControllerRumble(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateGbControllerRumble(enabled)
        }
    }

    fun updateFrameSkipEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateFrameSkipEnabled(enabled)
        }
    }

    fun updateFrameSkipThrottlePercent(percent: Int) {
        viewModelScope.launch {
            repository.updateFrameSkipThrottlePercent(percent)
        }
    }

    fun updateFrameSkipInterval(interval: Int) {
        viewModelScope.launch {
            repository.updateFrameSkipInterval(interval)
        }
    }

    fun updateInterframeBlending(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateInterframeBlending(enabled)
        }
    }

    fun updateIdleLoopRemoval(mode: IdleLoopRemovalMode) {
        viewModelScope.launch {
            repository.updateIdleLoopRemoval(mode.name)
        }
    }

    fun updateKeyMapA(target: VirtualKeyTarget) {
        viewModelScope.launch {
            repository.updateKeyMapA(target.name)
        }
    }

    fun updateKeyMapB(target: VirtualKeyTarget) {
        viewModelScope.launch {
            repository.updateKeyMapB(target.name)
        }
    }

    fun updateKeyMapUp(target: VirtualKeyTarget) {
        viewModelScope.launch {
            repository.updateKeyMapUp(target.name)
        }
    }

    fun updateKeyMapDown(target: VirtualKeyTarget) {
        viewModelScope.launch {
            repository.updateKeyMapDown(target.name)
        }
    }

    fun updateKeyMapLeft(target: VirtualKeyTarget) {
        viewModelScope.launch {
            repository.updateKeyMapLeft(target.name)
        }
    }

    fun updateKeyMapRight(target: VirtualKeyTarget) {
        viewModelScope.launch {
            repository.updateKeyMapRight(target.name)
        }
    }

    fun updateKeyMapL(target: VirtualKeyTarget) {
        viewModelScope.launch {
            repository.updateKeyMapL(target.name)
        }
    }

    fun updateKeyMapR(target: VirtualKeyTarget) {
        viewModelScope.launch {
            repository.updateKeyMapR(target.name)
        }
    }

    fun updateKeyMapStart(target: VirtualKeyTarget) {
        viewModelScope.launch {
            repository.updateKeyMapStart(target.name)
        }
    }

    fun updateKeyMapSelect(target: VirtualKeyTarget) {
        viewModelScope.launch {
            repository.updateKeyMapSelect(target.name)
        }
    }

    fun updateHardwareKey(action: HardwareButtonAction, keyCode: Int) {
        viewModelScope.launch {
            when (action) {
                HardwareButtonAction.A -> repository.updateHardwareKeyA(keyCode)
                HardwareButtonAction.B -> repository.updateHardwareKeyB(keyCode)
                HardwareButtonAction.UP -> repository.updateHardwareKeyUp(keyCode)
                HardwareButtonAction.DOWN -> repository.updateHardwareKeyDown(keyCode)
                HardwareButtonAction.LEFT -> repository.updateHardwareKeyLeft(keyCode)
                HardwareButtonAction.RIGHT -> repository.updateHardwareKeyRight(keyCode)
                HardwareButtonAction.L -> repository.updateHardwareKeyL(keyCode)
                HardwareButtonAction.R -> repository.updateHardwareKeyR(keyCode)
                HardwareButtonAction.START -> repository.updateHardwareKeyStart(keyCode)
                HardwareButtonAction.SELECT -> repository.updateHardwareKeySelect(keyCode)
            }
        }
    }

    fun resetHardwareKey(action: HardwareButtonAction) {
        updateHardwareKey(action, action.defaultKeyCode)
    }
    
    // 系统设置
    fun updateBiosPath(path: String?) {
        viewModelScope.launch {
            repository.updateBiosPath(path)
        }
    }
    
    fun updateFastForwardSpeed(speed: FastForwardSpeed) {
        viewModelScope.launch {
            repository.updateFastForwardSpeed(speed.name)
        }
    }
    
    fun updateLanguage(language: String) {
        viewModelScope.launch {
            repository.updateLanguage(language)
        }
    }

    fun updateImmersiveMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateImmersiveMode(enabled)
        }
    }

    fun updateAutoSaveOnExit(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAutoSaveOnExit(enabled)
        }
    }

    fun updatePauseOnBackground(enabled: Boolean) {
        viewModelScope.launch {
            repository.updatePauseOnBackground(enabled)
        }
    }

    fun updateShowVirtualControls(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateShowVirtualControls(enabled)
        }
    }
    
    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                // 注意：实际使用时需要传入正确的 Application
                throw IllegalStateException("请通过依赖注入或正确的方式创建 ViewModel")
            }
        }
    }
}

private fun sanitizeHexColor(input: String, fallback: String): String {
    val raw = input.trim().uppercase()
    val normalized = when {
        raw.startsWith("#") -> raw
        raw.isEmpty() -> fallback
        else -> "#$raw"
    }

    val ok = Regex("^#[0-9A-F]{6}$").matches(normalized)
    return if (ok) normalized else fallback
}

enum class ThemePreset(
    val displayName: String,
    val primaryHex: String,
    val secondaryHex: String,
    val tertiaryHex: String,
    val backgroundHex: String,
    val surfaceHex: String
) {
    JBOY_CLASSIC(
        displayName = "JBOY 青色",
        primaryHex = "#00696B",
        secondaryHex = "#4A6364",
        tertiaryHex = "#4B607B",
        backgroundHex = "#FAFDFC",
        surfaceHex = "#FAFDFC"
    ),
    SUNSET_ORANGE(
        displayName = "落日橙",
        primaryHex = "#B85600",
        secondaryHex = "#8B5D3B",
        tertiaryHex = "#6B5CAB",
        backgroundHex = "#FFF7F1",
        surfaceHex = "#FFF8F3"
    ),
    RETRO_GREEN(
        displayName = "复古绿",
        primaryHex = "#2E7D32",
        secondaryHex = "#4E6A50",
        tertiaryHex = "#6C7A35",
        backgroundHex = "#F6FBF4",
        surfaceHex = "#F8FCF6"
    ),
    COSMIC_BLUE(
        displayName = "深海蓝",
        primaryHex = "#2456D1",
        secondaryHex = "#4A5D87",
        tertiaryHex = "#6A4CA5",
        backgroundHex = "#F4F7FF",
        surfaceHex = "#F6F8FF"
    )
}

enum class IdleLoopRemovalMode(val displayName: String, val coreValue: String) {
    REMOVE_KNOWN("删除已知空闲循环", "remove"),
    DETECT_AND_REMOVE("检测并删除", "detect"),
    IGNORE("不移除", "ignore")
}

enum class VirtualKeyTarget(val displayName: String) {
    A("A"),
    B("B"),
    UP("UP"),
    DOWN("DOWN"),
    LEFT("LEFT"),
    RIGHT("RIGHT"),
    L("L"),
    R("R"),
    START("START"),
    SELECT("SELECT")
}

enum class HardwareButtonAction(val displayName: String, val defaultKeyCode: Int) {
    A("A", KeyEvent.KEYCODE_BUTTON_A),
    B("B", KeyEvent.KEYCODE_BUTTON_B),
    UP("UP", KeyEvent.KEYCODE_DPAD_UP),
    DOWN("DOWN", KeyEvent.KEYCODE_DPAD_DOWN),
    LEFT("LEFT", KeyEvent.KEYCODE_DPAD_LEFT),
    RIGHT("RIGHT", KeyEvent.KEYCODE_DPAD_RIGHT),
    L("L", KeyEvent.KEYCODE_BUTTON_L1),
    R("R", KeyEvent.KEYCODE_BUTTON_R1),
    START("START", KeyEvent.KEYCODE_BUTTON_START),
    SELECT("SELECT", KeyEvent.KEYCODE_BUTTON_SELECT)
}
