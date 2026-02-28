package com.jboy.emulator.data

import android.content.Context
import android.view.KeyEvent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 创建 DataStore 实例
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class SettingsData(
    val videoFilter: String = "NEAREST",
    val aspectRatio: String = "ORIGINAL",
    val showFps: Boolean = false,
    val themeCustomEnabled: Boolean = false,
    val themePreset: String = "JBOY_CLASSIC",
    val themePrimaryHex: String = "#00696B",
    val themeSecondaryHex: String = "#4A6364",
    val themeTertiaryHex: String = "#4B607B",
    val themeBackgroundHex: String = "#FAFDFC",
    val themeSurfaceHex: String = "#FAFDFC",
    val audioEnabled: Boolean = true,
    val masterVolume: Float = 100f,
    val audioSampleRate: Int = 44100,
    val audioBufferSize: Int = 8192,
    val audioFilterEnabled: Boolean = true,
    val audioFilterLevel: Int = 60,
    val controllerOpacity: Float = 0.8f,
    val buttonSize: Float = 1f,
    val dpadMode: String = "WHEEL",
    val vibrationEnabled: Boolean = true,
    val gbControllerRumble: Boolean = false,
    val frameSkipEnabled: Boolean = false,
    val frameSkipThrottlePercent: Int = 33,
    val frameSkipInterval: Int = 0,
    val interframeBlending: Boolean = false,
    val idleLoopRemoval: String = "REMOVE_KNOWN",
    val keyMapA: String = "A",
    val keyMapB: String = "B",
    val keyMapUp: String = "UP",
    val keyMapDown: String = "DOWN",
    val keyMapLeft: String = "LEFT",
    val keyMapRight: String = "RIGHT",
    val keyMapL: String = "L",
    val keyMapR: String = "R",
    val keyMapStart: String = "START",
    val keyMapSelect: String = "SELECT",
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
    val biosPath: String? = null,
    val fastForwardSpeed: String = "X2",
    val language: String = "zh-CN",
    val immersiveMode: Boolean = false,
    val autoSaveOnExit: Boolean = true,
    val pauseOnBackground: Boolean = true,
    val showVirtualControls: Boolean = true
)

class SettingsDataStore(private val context: Context) {
    
    private object PreferencesKeys {
        val VIDEO_FILTER = stringPreferencesKey("video_filter")
        val ASPECT_RATIO = stringPreferencesKey("aspect_ratio")
        val SHOW_FPS = booleanPreferencesKey("show_fps")
        val THEME_CUSTOM_ENABLED = booleanPreferencesKey("theme_custom_enabled")
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val THEME_PRIMARY_HEX = stringPreferencesKey("theme_primary_hex")
        val THEME_SECONDARY_HEX = stringPreferencesKey("theme_secondary_hex")
        val THEME_TERTIARY_HEX = stringPreferencesKey("theme_tertiary_hex")
        val THEME_BACKGROUND_HEX = stringPreferencesKey("theme_background_hex")
        val THEME_SURFACE_HEX = stringPreferencesKey("theme_surface_hex")
        val AUDIO_ENABLED = booleanPreferencesKey("audio_enabled")
        val MASTER_VOLUME = floatPreferencesKey("master_volume")
        val AUDIO_SAMPLE_RATE = intPreferencesKey("audio_sample_rate")
        val AUDIO_BUFFER_SIZE = intPreferencesKey("audio_buffer_size")
        val AUDIO_FILTER_ENABLED = booleanPreferencesKey("audio_filter_enabled")
        val AUDIO_FILTER_LEVEL = intPreferencesKey("audio_filter_level")
        val CONTROLLER_OPACITY = floatPreferencesKey("controller_opacity")
        val BUTTON_SIZE = floatPreferencesKey("button_size")
        val DPAD_MODE = stringPreferencesKey("dpad_mode")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val GB_CONTROLLER_RUMBLE = booleanPreferencesKey("gb_controller_rumble")
        val FRAME_SKIP_ENABLED = booleanPreferencesKey("frame_skip_enabled")
        val FRAME_SKIP_THROTTLE_PERCENT = intPreferencesKey("frame_skip_throttle_percent")
        val FRAME_SKIP_INTERVAL = intPreferencesKey("frame_skip_interval")
        val INTERFRAME_BLENDING = booleanPreferencesKey("interframe_blending")
        val IDLE_LOOP_REMOVAL = stringPreferencesKey("idle_loop_removal")
        val KEY_MAP_A = stringPreferencesKey("key_map_a")
        val KEY_MAP_B = stringPreferencesKey("key_map_b")
        val KEY_MAP_UP = stringPreferencesKey("key_map_up")
        val KEY_MAP_DOWN = stringPreferencesKey("key_map_down")
        val KEY_MAP_LEFT = stringPreferencesKey("key_map_left")
        val KEY_MAP_RIGHT = stringPreferencesKey("key_map_right")
        val KEY_MAP_L = stringPreferencesKey("key_map_l")
        val KEY_MAP_R = stringPreferencesKey("key_map_r")
        val KEY_MAP_START = stringPreferencesKey("key_map_start")
        val KEY_MAP_SELECT = stringPreferencesKey("key_map_select")
        val HARDWARE_KEY_A = intPreferencesKey("hardware_key_a")
        val HARDWARE_KEY_B = intPreferencesKey("hardware_key_b")
        val HARDWARE_KEY_UP = intPreferencesKey("hardware_key_up")
        val HARDWARE_KEY_DOWN = intPreferencesKey("hardware_key_down")
        val HARDWARE_KEY_LEFT = intPreferencesKey("hardware_key_left")
        val HARDWARE_KEY_RIGHT = intPreferencesKey("hardware_key_right")
        val HARDWARE_KEY_L = intPreferencesKey("hardware_key_l")
        val HARDWARE_KEY_R = intPreferencesKey("hardware_key_r")
        val HARDWARE_KEY_START = intPreferencesKey("hardware_key_start")
        val HARDWARE_KEY_SELECT = intPreferencesKey("hardware_key_select")
        val BIOS_PATH = stringPreferencesKey("bios_path")
        val FAST_FORWARD_SPEED = stringPreferencesKey("fast_forward_speed")
        val LANGUAGE = stringPreferencesKey("language")
        val IMMERSIVE_MODE = booleanPreferencesKey("immersive_mode")
        val AUTO_SAVE_ON_EXIT = booleanPreferencesKey("auto_save_on_exit")
        val PAUSE_ON_BACKGROUND = booleanPreferencesKey("pause_on_background")
        val SHOW_VIRTUAL_CONTROLS = booleanPreferencesKey("show_virtual_controls")
    }
    
    val settingsFlow: Flow<SettingsData> = context.settingsDataStore.data
        .map { preferences ->
            SettingsData(
                videoFilter = preferences[PreferencesKeys.VIDEO_FILTER] ?: "NEAREST",
                aspectRatio = preferences[PreferencesKeys.ASPECT_RATIO] ?: "ORIGINAL",
                showFps = preferences[PreferencesKeys.SHOW_FPS] ?: false,
                themeCustomEnabled = preferences[PreferencesKeys.THEME_CUSTOM_ENABLED] ?: false,
                themePreset = preferences[PreferencesKeys.THEME_PRESET] ?: "JBOY_CLASSIC",
                themePrimaryHex = preferences[PreferencesKeys.THEME_PRIMARY_HEX] ?: "#00696B",
                themeSecondaryHex = preferences[PreferencesKeys.THEME_SECONDARY_HEX] ?: "#4A6364",
                themeTertiaryHex = preferences[PreferencesKeys.THEME_TERTIARY_HEX] ?: "#4B607B",
                themeBackgroundHex = preferences[PreferencesKeys.THEME_BACKGROUND_HEX] ?: "#FAFDFC",
                themeSurfaceHex = preferences[PreferencesKeys.THEME_SURFACE_HEX] ?: "#FAFDFC",
                audioEnabled = preferences[PreferencesKeys.AUDIO_ENABLED] ?: true,
                masterVolume = preferences[PreferencesKeys.MASTER_VOLUME] ?: 100f,
                audioSampleRate = preferences[PreferencesKeys.AUDIO_SAMPLE_RATE] ?: 44100,
                audioBufferSize = preferences[PreferencesKeys.AUDIO_BUFFER_SIZE] ?: 8192,
                audioFilterEnabled = preferences[PreferencesKeys.AUDIO_FILTER_ENABLED] ?: true,
                audioFilterLevel = (preferences[PreferencesKeys.AUDIO_FILTER_LEVEL] ?: 60).coerceIn(0, 100),
                controllerOpacity = preferences[PreferencesKeys.CONTROLLER_OPACITY] ?: 0.8f,
                buttonSize = preferences[PreferencesKeys.BUTTON_SIZE] ?: 1f,
                dpadMode = preferences[PreferencesKeys.DPAD_MODE] ?: "WHEEL",
                vibrationEnabled = preferences[PreferencesKeys.VIBRATION_ENABLED] ?: true,
                gbControllerRumble = preferences[PreferencesKeys.GB_CONTROLLER_RUMBLE] ?: false,
                frameSkipEnabled = preferences[PreferencesKeys.FRAME_SKIP_ENABLED] ?: false,
                frameSkipThrottlePercent = (preferences[PreferencesKeys.FRAME_SKIP_THROTTLE_PERCENT] ?: 33).coerceIn(0, 100),
                frameSkipInterval = (preferences[PreferencesKeys.FRAME_SKIP_INTERVAL] ?: 0).coerceIn(0, 12),
                interframeBlending = preferences[PreferencesKeys.INTERFRAME_BLENDING] ?: false,
                idleLoopRemoval = preferences[PreferencesKeys.IDLE_LOOP_REMOVAL] ?: "REMOVE_KNOWN",
                keyMapA = preferences[PreferencesKeys.KEY_MAP_A] ?: "A",
                keyMapB = preferences[PreferencesKeys.KEY_MAP_B] ?: "B",
                keyMapUp = preferences[PreferencesKeys.KEY_MAP_UP] ?: "UP",
                keyMapDown = preferences[PreferencesKeys.KEY_MAP_DOWN] ?: "DOWN",
                keyMapLeft = preferences[PreferencesKeys.KEY_MAP_LEFT] ?: "LEFT",
                keyMapRight = preferences[PreferencesKeys.KEY_MAP_RIGHT] ?: "RIGHT",
                keyMapL = preferences[PreferencesKeys.KEY_MAP_L] ?: "L",
                keyMapR = preferences[PreferencesKeys.KEY_MAP_R] ?: "R",
                keyMapStart = preferences[PreferencesKeys.KEY_MAP_START] ?: "START",
                keyMapSelect = preferences[PreferencesKeys.KEY_MAP_SELECT] ?: "SELECT",
                hardwareKeyA = preferences[PreferencesKeys.HARDWARE_KEY_A] ?: KeyEvent.KEYCODE_BUTTON_A,
                hardwareKeyB = preferences[PreferencesKeys.HARDWARE_KEY_B] ?: KeyEvent.KEYCODE_BUTTON_B,
                hardwareKeyUp = preferences[PreferencesKeys.HARDWARE_KEY_UP] ?: KeyEvent.KEYCODE_DPAD_UP,
                hardwareKeyDown = preferences[PreferencesKeys.HARDWARE_KEY_DOWN] ?: KeyEvent.KEYCODE_DPAD_DOWN,
                hardwareKeyLeft = preferences[PreferencesKeys.HARDWARE_KEY_LEFT] ?: KeyEvent.KEYCODE_DPAD_LEFT,
                hardwareKeyRight = preferences[PreferencesKeys.HARDWARE_KEY_RIGHT] ?: KeyEvent.KEYCODE_DPAD_RIGHT,
                hardwareKeyL = preferences[PreferencesKeys.HARDWARE_KEY_L] ?: KeyEvent.KEYCODE_BUTTON_L1,
                hardwareKeyR = preferences[PreferencesKeys.HARDWARE_KEY_R] ?: KeyEvent.KEYCODE_BUTTON_R1,
                hardwareKeyStart = preferences[PreferencesKeys.HARDWARE_KEY_START] ?: KeyEvent.KEYCODE_BUTTON_START,
                hardwareKeySelect = preferences[PreferencesKeys.HARDWARE_KEY_SELECT] ?: KeyEvent.KEYCODE_BUTTON_SELECT,
                biosPath = preferences[PreferencesKeys.BIOS_PATH],
                fastForwardSpeed = preferences[PreferencesKeys.FAST_FORWARD_SPEED] ?: "X2",
                language = preferences[PreferencesKeys.LANGUAGE] ?: "zh-CN",
                immersiveMode = preferences[PreferencesKeys.IMMERSIVE_MODE] ?: false,
                autoSaveOnExit = preferences[PreferencesKeys.AUTO_SAVE_ON_EXIT] ?: true,
                pauseOnBackground = preferences[PreferencesKeys.PAUSE_ON_BACKGROUND] ?: true,
                showVirtualControls = preferences[PreferencesKeys.SHOW_VIRTUAL_CONTROLS] ?: true
            )
        }
    
    suspend fun updateVideoFilter(filter: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.VIDEO_FILTER] = filter
        }
    }
    
    suspend fun updateAspectRatio(ratio: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.ASPECT_RATIO] = ratio
        }
    }
    
    suspend fun updateShowFps(show: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_FPS] = show
        }
    }

    suspend fun updateThemeCustomEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_CUSTOM_ENABLED] = enabled
        }
    }

    suspend fun updateThemePreset(preset: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_PRESET] = preset
        }
    }

    suspend fun updateThemePrimaryHex(hex: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_PRIMARY_HEX] = hex
        }
    }

    suspend fun updateThemeSecondaryHex(hex: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_SECONDARY_HEX] = hex
        }
    }

    suspend fun updateThemeTertiaryHex(hex: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_TERTIARY_HEX] = hex
        }
    }

    suspend fun updateThemeBackgroundHex(hex: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_BACKGROUND_HEX] = hex
        }
    }

    suspend fun updateThemeSurfaceHex(hex: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_SURFACE_HEX] = hex
        }
    }
    
    suspend fun updateAudioEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.AUDIO_ENABLED] = enabled
        }
    }
    
    suspend fun updateMasterVolume(volume: Float) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.MASTER_VOLUME] = volume
        }
    }

    suspend fun updateAudioSampleRate(sampleRate: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.AUDIO_SAMPLE_RATE] = sampleRate.coerceIn(8000, 96000)
        }
    }

    suspend fun updateAudioBufferSize(bufferSize: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.AUDIO_BUFFER_SIZE] = bufferSize.coerceIn(1024, 65536)
        }
    }

    suspend fun updateAudioFilterEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.AUDIO_FILTER_ENABLED] = enabled
        }
    }

    suspend fun updateAudioFilterLevel(level: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.AUDIO_FILTER_LEVEL] = level.coerceIn(0, 100)
        }
    }
    
    suspend fun updateControllerOpacity(opacity: Float) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.CONTROLLER_OPACITY] = opacity
        }
    }
    
    suspend fun updateButtonSize(size: Float) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.BUTTON_SIZE] = size
        }
    }

    suspend fun updateDpadMode(mode: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.DPAD_MODE] = mode
        }
    }
    
    suspend fun updateVibrationEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.VIBRATION_ENABLED] = enabled
        }
    }

    suspend fun updateGbControllerRumble(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.GB_CONTROLLER_RUMBLE] = enabled
        }
    }

    suspend fun updateFrameSkipEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.FRAME_SKIP_ENABLED] = enabled
        }
    }

    suspend fun updateFrameSkipThrottlePercent(percent: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.FRAME_SKIP_THROTTLE_PERCENT] = percent.coerceIn(0, 100)
        }
    }

    suspend fun updateFrameSkipInterval(interval: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.FRAME_SKIP_INTERVAL] = interval.coerceIn(0, 12)
        }
    }

    suspend fun updateInterframeBlending(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.INTERFRAME_BLENDING] = enabled
        }
    }

    suspend fun updateIdleLoopRemoval(mode: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.IDLE_LOOP_REMOVAL] = mode
        }
    }

    suspend fun updateKeyMapA(target: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_MAP_A] = target
        }
    }

    suspend fun updateKeyMapB(target: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_MAP_B] = target
        }
    }

    suspend fun updateKeyMapUp(target: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_MAP_UP] = target
        }
    }

    suspend fun updateKeyMapDown(target: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_MAP_DOWN] = target
        }
    }

    suspend fun updateKeyMapLeft(target: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_MAP_LEFT] = target
        }
    }

    suspend fun updateKeyMapRight(target: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_MAP_RIGHT] = target
        }
    }

    suspend fun updateKeyMapL(target: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_MAP_L] = target
        }
    }

    suspend fun updateKeyMapR(target: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_MAP_R] = target
        }
    }

    suspend fun updateKeyMapStart(target: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_MAP_START] = target
        }
    }

    suspend fun updateKeyMapSelect(target: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_MAP_SELECT] = target
        }
    }

    suspend fun updateHardwareKeyA(keyCode: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.HARDWARE_KEY_A] = keyCode
        }
    }

    suspend fun updateHardwareKeyB(keyCode: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.HARDWARE_KEY_B] = keyCode
        }
    }

    suspend fun updateHardwareKeyUp(keyCode: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.HARDWARE_KEY_UP] = keyCode
        }
    }

    suspend fun updateHardwareKeyDown(keyCode: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.HARDWARE_KEY_DOWN] = keyCode
        }
    }

    suspend fun updateHardwareKeyLeft(keyCode: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.HARDWARE_KEY_LEFT] = keyCode
        }
    }

    suspend fun updateHardwareKeyRight(keyCode: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.HARDWARE_KEY_RIGHT] = keyCode
        }
    }

    suspend fun updateHardwareKeyL(keyCode: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.HARDWARE_KEY_L] = keyCode
        }
    }

    suspend fun updateHardwareKeyR(keyCode: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.HARDWARE_KEY_R] = keyCode
        }
    }

    suspend fun updateHardwareKeyStart(keyCode: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.HARDWARE_KEY_START] = keyCode
        }
    }

    suspend fun updateHardwareKeySelect(keyCode: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.HARDWARE_KEY_SELECT] = keyCode
        }
    }
    
    suspend fun updateBiosPath(path: String?) {
        context.settingsDataStore.edit { preferences ->
            if (path != null) {
                preferences[PreferencesKeys.BIOS_PATH] = path
            } else {
                preferences.remove(PreferencesKeys.BIOS_PATH)
            }
        }
    }
    
    suspend fun updateFastForwardSpeed(speed: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.FAST_FORWARD_SPEED] = speed
        }
    }
    
    suspend fun updateLanguage(language: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = language
        }
    }

    suspend fun updateImmersiveMode(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.IMMERSIVE_MODE] = enabled
        }
    }

    suspend fun updateAutoSaveOnExit(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_SAVE_ON_EXIT] = enabled
        }
    }

    suspend fun updatePauseOnBackground(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.PAUSE_ON_BACKGROUND] = enabled
        }
    }

    suspend fun updateShowVirtualControls(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_VIRTUAL_CONTROLS] = enabled
        }
    }
}

class SettingsRepository(private val dataStore: SettingsDataStore) {
    
    val settingsFlow: Flow<SettingsData> = dataStore.settingsFlow
    
    suspend fun updateVideoFilter(filter: String) = dataStore.updateVideoFilter(filter)
    suspend fun updateAspectRatio(ratio: String) = dataStore.updateAspectRatio(ratio)
    suspend fun updateShowFps(show: Boolean) = dataStore.updateShowFps(show)
    suspend fun updateThemeCustomEnabled(enabled: Boolean) = dataStore.updateThemeCustomEnabled(enabled)
    suspend fun updateThemePreset(preset: String) = dataStore.updateThemePreset(preset)
    suspend fun updateThemePrimaryHex(hex: String) = dataStore.updateThemePrimaryHex(hex)
    suspend fun updateThemeSecondaryHex(hex: String) = dataStore.updateThemeSecondaryHex(hex)
    suspend fun updateThemeTertiaryHex(hex: String) = dataStore.updateThemeTertiaryHex(hex)
    suspend fun updateThemeBackgroundHex(hex: String) = dataStore.updateThemeBackgroundHex(hex)
    suspend fun updateThemeSurfaceHex(hex: String) = dataStore.updateThemeSurfaceHex(hex)
    suspend fun updateAudioEnabled(enabled: Boolean) = dataStore.updateAudioEnabled(enabled)
    suspend fun updateMasterVolume(volume: Float) = dataStore.updateMasterVolume(volume)
    suspend fun updateAudioSampleRate(sampleRate: Int) = dataStore.updateAudioSampleRate(sampleRate)
    suspend fun updateAudioBufferSize(bufferSize: Int) = dataStore.updateAudioBufferSize(bufferSize)
    suspend fun updateAudioFilterEnabled(enabled: Boolean) = dataStore.updateAudioFilterEnabled(enabled)
    suspend fun updateAudioFilterLevel(level: Int) = dataStore.updateAudioFilterLevel(level)
    suspend fun updateControllerOpacity(opacity: Float) = dataStore.updateControllerOpacity(opacity)
    suspend fun updateButtonSize(size: Float) = dataStore.updateButtonSize(size)
    suspend fun updateDpadMode(mode: String) = dataStore.updateDpadMode(mode)
    suspend fun updateVibrationEnabled(enabled: Boolean) = dataStore.updateVibrationEnabled(enabled)
    suspend fun updateGbControllerRumble(enabled: Boolean) = dataStore.updateGbControllerRumble(enabled)
    suspend fun updateFrameSkipEnabled(enabled: Boolean) = dataStore.updateFrameSkipEnabled(enabled)
    suspend fun updateFrameSkipThrottlePercent(percent: Int) = dataStore.updateFrameSkipThrottlePercent(percent)
    suspend fun updateFrameSkipInterval(interval: Int) = dataStore.updateFrameSkipInterval(interval)
    suspend fun updateInterframeBlending(enabled: Boolean) = dataStore.updateInterframeBlending(enabled)
    suspend fun updateIdleLoopRemoval(mode: String) = dataStore.updateIdleLoopRemoval(mode)
    suspend fun updateKeyMapA(target: String) = dataStore.updateKeyMapA(target)
    suspend fun updateKeyMapB(target: String) = dataStore.updateKeyMapB(target)
    suspend fun updateKeyMapUp(target: String) = dataStore.updateKeyMapUp(target)
    suspend fun updateKeyMapDown(target: String) = dataStore.updateKeyMapDown(target)
    suspend fun updateKeyMapLeft(target: String) = dataStore.updateKeyMapLeft(target)
    suspend fun updateKeyMapRight(target: String) = dataStore.updateKeyMapRight(target)
    suspend fun updateKeyMapL(target: String) = dataStore.updateKeyMapL(target)
    suspend fun updateKeyMapR(target: String) = dataStore.updateKeyMapR(target)
    suspend fun updateKeyMapStart(target: String) = dataStore.updateKeyMapStart(target)
    suspend fun updateKeyMapSelect(target: String) = dataStore.updateKeyMapSelect(target)
    suspend fun updateHardwareKeyA(keyCode: Int) = dataStore.updateHardwareKeyA(keyCode)
    suspend fun updateHardwareKeyB(keyCode: Int) = dataStore.updateHardwareKeyB(keyCode)
    suspend fun updateHardwareKeyUp(keyCode: Int) = dataStore.updateHardwareKeyUp(keyCode)
    suspend fun updateHardwareKeyDown(keyCode: Int) = dataStore.updateHardwareKeyDown(keyCode)
    suspend fun updateHardwareKeyLeft(keyCode: Int) = dataStore.updateHardwareKeyLeft(keyCode)
    suspend fun updateHardwareKeyRight(keyCode: Int) = dataStore.updateHardwareKeyRight(keyCode)
    suspend fun updateHardwareKeyL(keyCode: Int) = dataStore.updateHardwareKeyL(keyCode)
    suspend fun updateHardwareKeyR(keyCode: Int) = dataStore.updateHardwareKeyR(keyCode)
    suspend fun updateHardwareKeyStart(keyCode: Int) = dataStore.updateHardwareKeyStart(keyCode)
    suspend fun updateHardwareKeySelect(keyCode: Int) = dataStore.updateHardwareKeySelect(keyCode)
    suspend fun updateBiosPath(path: String?) = dataStore.updateBiosPath(path)
    suspend fun updateFastForwardSpeed(speed: String) = dataStore.updateFastForwardSpeed(speed)
    suspend fun updateLanguage(language: String) = dataStore.updateLanguage(language)
    suspend fun updateImmersiveMode(enabled: Boolean) = dataStore.updateImmersiveMode(enabled)
    suspend fun updateAutoSaveOnExit(enabled: Boolean) = dataStore.updateAutoSaveOnExit(enabled)
    suspend fun updatePauseOnBackground(enabled: Boolean) = dataStore.updatePauseOnBackground(enabled)
    suspend fun updateShowVirtualControls(enabled: Boolean) = dataStore.updateShowVirtualControls(enabled)
}
