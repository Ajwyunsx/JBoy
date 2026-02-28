package com.jboy.emulator.ui.game

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.jboy.emulator.data.settingsDataStore
import com.jboy.emulator.core.InputHandler as CoreInputHandler
import com.jboy.emulator.core.InputKeys
import com.jboy.emulator.input.InputHandler
import com.jboy.emulator.ui.gamepad.DpadMode
import com.jboy.emulator.ui.gamepad.GamepadConfig
import com.jboy.emulator.ui.gamepad.GamepadLayoutOffsets
import com.jboy.emulator.ui.gamepad.VirtualGamepad
import com.jboy.emulator.ui.settings.AspectRatio
import com.jboy.emulator.ui.settings.VideoFilter
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import kotlin.math.floor
import kotlin.math.min

private val PREF_VIDEO_FILTER = stringPreferencesKey("video_filter")
private val PREF_ASPECT_RATIO = stringPreferencesKey("aspect_ratio")
private val PREF_SHOW_FPS = booleanPreferencesKey("show_fps")

private val PREF_CONTROLLER_OPACITY = floatPreferencesKey("controller_opacity")
private val PREF_BUTTON_SIZE = floatPreferencesKey("button_size")
private val PREF_VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
private val PREF_IMMERSIVE_MODE = booleanPreferencesKey("immersive_mode")
private val PREF_SHOW_VIRTUAL_CONTROLS = booleanPreferencesKey("show_virtual_controls")
private val PREF_AUDIO_ENABLED = booleanPreferencesKey("audio_enabled")
private val PREF_MASTER_VOLUME = floatPreferencesKey("master_volume")
private val PREF_AUDIO_SAMPLE_RATE = intPreferencesKey("audio_sample_rate")
private val PREF_AUDIO_BUFFER_SIZE = intPreferencesKey("audio_buffer_size")
private val PREF_AUDIO_FILTER_ENABLED = booleanPreferencesKey("audio_filter_enabled")
private val PREF_AUDIO_FILTER_LEVEL = intPreferencesKey("audio_filter_level")
private val PREF_DPAD_MODE = stringPreferencesKey("dpad_mode")
private val PREF_FRAME_SKIP_ENABLED = booleanPreferencesKey("frame_skip_enabled")
private val PREF_FRAME_SKIP_THROTTLE_PERCENT = intPreferencesKey("frame_skip_throttle_percent")
private val PREF_FRAME_SKIP_INTERVAL = intPreferencesKey("frame_skip_interval")
private val PREF_INTERFRAME_BLENDING = booleanPreferencesKey("interframe_blending")
private val PREF_IDLE_LOOP_REMOVAL = stringPreferencesKey("idle_loop_removal")
private val PREF_GB_CONTROLLER_RUMBLE = booleanPreferencesKey("gb_controller_rumble")
private val PREF_KEY_MAP_A = stringPreferencesKey("key_map_a")
private val PREF_KEY_MAP_B = stringPreferencesKey("key_map_b")
private val PREF_KEY_MAP_UP = stringPreferencesKey("key_map_up")
private val PREF_KEY_MAP_DOWN = stringPreferencesKey("key_map_down")
private val PREF_KEY_MAP_LEFT = stringPreferencesKey("key_map_left")
private val PREF_KEY_MAP_RIGHT = stringPreferencesKey("key_map_right")
private val PREF_KEY_MAP_L = stringPreferencesKey("key_map_l")
private val PREF_KEY_MAP_R = stringPreferencesKey("key_map_r")
private val PREF_KEY_MAP_START = stringPreferencesKey("key_map_start")
private val PREF_KEY_MAP_SELECT = stringPreferencesKey("key_map_select")
private val PREF_HARDWARE_KEY_A = intPreferencesKey("hardware_key_a")
private val PREF_HARDWARE_KEY_B = intPreferencesKey("hardware_key_b")
private val PREF_HARDWARE_KEY_UP = intPreferencesKey("hardware_key_up")
private val PREF_HARDWARE_KEY_DOWN = intPreferencesKey("hardware_key_down")
private val PREF_HARDWARE_KEY_LEFT = intPreferencesKey("hardware_key_left")
private val PREF_HARDWARE_KEY_RIGHT = intPreferencesKey("hardware_key_right")
private val PREF_HARDWARE_KEY_L = intPreferencesKey("hardware_key_l")
private val PREF_HARDWARE_KEY_R = intPreferencesKey("hardware_key_r")
private val PREF_HARDWARE_KEY_START = intPreferencesKey("hardware_key_start")
private val PREF_HARDWARE_KEY_SELECT = intPreferencesKey("hardware_key_select")

private val PREF_DPAD_OFFSET_X = floatPreferencesKey("layout_dpad_offset_x")
private val PREF_DPAD_OFFSET_Y = floatPreferencesKey("layout_dpad_offset_y")
private val PREF_ACTION_OFFSET_X = floatPreferencesKey("layout_action_offset_x")
private val PREF_ACTION_OFFSET_Y = floatPreferencesKey("layout_action_offset_y")
private val PREF_MENU_OFFSET_X = floatPreferencesKey("layout_menu_offset_x")
private val PREF_MENU_OFFSET_Y = floatPreferencesKey("layout_menu_offset_y")

private val PREF_A_OFFSET_X = floatPreferencesKey("layout_a_offset_x")
private val PREF_A_OFFSET_Y = floatPreferencesKey("layout_a_offset_y")
private val PREF_B_OFFSET_X = floatPreferencesKey("layout_b_offset_x")
private val PREF_B_OFFSET_Y = floatPreferencesKey("layout_b_offset_y")
private val PREF_L_OFFSET_X = floatPreferencesKey("layout_l_offset_x")
private val PREF_L_OFFSET_Y = floatPreferencesKey("layout_l_offset_y")
private val PREF_R_OFFSET_X = floatPreferencesKey("layout_r_offset_x")
private val PREF_R_OFFSET_Y = floatPreferencesKey("layout_r_offset_y")
private val PREF_START_OFFSET_X = floatPreferencesKey("layout_start_offset_x")
private val PREF_START_OFFSET_Y = floatPreferencesKey("layout_start_offset_y")
private val PREF_SELECT_OFFSET_X = floatPreferencesKey("layout_select_offset_x")
private val PREF_SELECT_OFFSET_Y = floatPreferencesKey("layout_select_offset_y")

private fun cheatKeyForPath(gamePath: String) =
    stringPreferencesKey("cheats_${gamePath.hashCode().toUInt().toString(16)}")

private fun saveSlotsKeyForPath(gamePath: String) =
    stringPreferencesKey("save_slots_${gamePath.hashCode().toUInt().toString(16)}")

private fun decodeSaveSlots(encoded: String?): List<Int> {
    val parsed = encoded
        ?.split(',')
        ?.mapNotNull { it.trim().toIntOrNull() }
        ?.filter { it > 0 }
        ?.distinct()
        ?.sorted()
        .orEmpty()
    return if (parsed.isEmpty()) {
        listOf(1, 2, 3, 4)
    } else {
        parsed
    }
}

private fun encodeSaveSlots(slots: List<Int>): String {
    return slots
        .filter { it > 0 }
        .distinct()
        .sorted()
        .joinToString(",")
}

private fun encodeCheatCodes(codes: List<CheatCodeItem>): String {
    return codes.joinToString(separator = "\n") { entry ->
        val escapedCode = entry.code
            .replace("\\", "\\\\")
            .replace("|", "\\p")
            .replace("\n", "\\n")
        "${entry.id}|${if (entry.enabled) 1 else 0}|$escapedCode"
    }
}

private fun decodeCheatCodes(encoded: String): List<CheatCodeItem> {
    if (encoded.isBlank()) return emptyList()
    return encoded
        .lineSequence()
        .mapNotNull { line ->
            val parts = line.split("|", limit = 3)
            if (parts.size < 3) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val enabled = parts[1] == "1"
            val code = parts[2]
                .replace("\\n", "\n")
                .replace("\\p", "|")
                .replace("\\\\", "\\")
                .trim()
            if (code.isEmpty()) {
                null
            } else {
                CheatCodeItem(id = id, code = code, enabled = enabled)
            }
        }
        .toList()
}

@Composable
fun GameScreen(
    gamePath: String,
    onBackToList: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenNetplay: () -> Unit = {},
    viewModel: GameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var isLayoutEditMode by remember { mutableStateOf(false) }
    val inputHandler = remember { InputHandler.getInstance() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editableLayoutOffsets by remember { mutableStateOf(GamepadLayoutOffsets.DEFAULT) }
    var cheatCodes by remember(gamePath) { mutableStateOf<List<CheatCodeItem>>(emptyList()) }
    var cheatCodesLoaded by remember(gamePath) { mutableStateOf(false) }
    var saveSlots by remember(gamePath) { mutableStateOf(listOf(1, 2, 3, 4)) }
    var saveSlotsLoaded by remember(gamePath) { mutableStateOf(false) }

    val gamepadPrefs by context.settingsDataStore.data
        .map { prefs ->
            GamepadPrefs(
                videoFilter = runCatching {
                    VideoFilter.valueOf(prefs[PREF_VIDEO_FILTER] ?: VideoFilter.NEAREST.name)
                }.getOrDefault(VideoFilter.NEAREST),
                aspectRatio = runCatching {
                    AspectRatio.valueOf(prefs[PREF_ASPECT_RATIO] ?: AspectRatio.ORIGINAL.name)
                }.getOrDefault(AspectRatio.ORIGINAL),
                showFps = prefs[PREF_SHOW_FPS] ?: false,
                buttonAlpha = (prefs[PREF_CONTROLLER_OPACITY] ?: 0.8f).coerceIn(0.2f, 1f),
                buttonScale = (prefs[PREF_BUTTON_SIZE] ?: 1f).coerceIn(0.5f, 1.5f),
                vibrationEnabled = prefs[PREF_VIBRATION_ENABLED] ?: true,
                immersiveMode = prefs[PREF_IMMERSIVE_MODE] ?: false,
                showVirtualControls = prefs[PREF_SHOW_VIRTUAL_CONTROLS] ?: true,
                audioEnabled = prefs[PREF_AUDIO_ENABLED] ?: true,
                masterVolume = (prefs[PREF_MASTER_VOLUME] ?: 100f).coerceIn(0f, 100f),
                audioSampleRate = prefs[PREF_AUDIO_SAMPLE_RATE] ?: 44100,
                audioBufferSize = prefs[PREF_AUDIO_BUFFER_SIZE] ?: 8192,
                audioFilterEnabled = prefs[PREF_AUDIO_FILTER_ENABLED] ?: true,
                audioFilterLevel = (prefs[PREF_AUDIO_FILTER_LEVEL] ?: 60).coerceIn(0, 100),
                frameSkipEnabled = prefs[PREF_FRAME_SKIP_ENABLED] ?: false,
                frameSkipThrottlePercent = (prefs[PREF_FRAME_SKIP_THROTTLE_PERCENT] ?: 33).coerceIn(0, 100),
                frameSkipInterval = (prefs[PREF_FRAME_SKIP_INTERVAL] ?: 0).coerceIn(0, 12),
                interframeBlending = prefs[PREF_INTERFRAME_BLENDING] ?: false,
                idleLoopRemoval = prefs[PREF_IDLE_LOOP_REMOVAL] ?: "REMOVE_KNOWN",
                gbControllerRumble = prefs[PREF_GB_CONTROLLER_RUMBLE] ?: false,
                keyMapA = prefs[PREF_KEY_MAP_A] ?: "A",
                keyMapB = prefs[PREF_KEY_MAP_B] ?: "B",
                keyMapUp = prefs[PREF_KEY_MAP_UP] ?: "UP",
                keyMapDown = prefs[PREF_KEY_MAP_DOWN] ?: "DOWN",
                keyMapLeft = prefs[PREF_KEY_MAP_LEFT] ?: "LEFT",
                keyMapRight = prefs[PREF_KEY_MAP_RIGHT] ?: "RIGHT",
                keyMapL = prefs[PREF_KEY_MAP_L] ?: "L",
                keyMapR = prefs[PREF_KEY_MAP_R] ?: "R",
                keyMapStart = prefs[PREF_KEY_MAP_START] ?: "START",
                keyMapSelect = prefs[PREF_KEY_MAP_SELECT] ?: "SELECT",
                hardwareKeyA = prefs[PREF_HARDWARE_KEY_A] ?: KeyEvent.KEYCODE_BUTTON_A,
                hardwareKeyB = prefs[PREF_HARDWARE_KEY_B] ?: KeyEvent.KEYCODE_BUTTON_B,
                hardwareKeyUp = prefs[PREF_HARDWARE_KEY_UP] ?: KeyEvent.KEYCODE_DPAD_UP,
                hardwareKeyDown = prefs[PREF_HARDWARE_KEY_DOWN] ?: KeyEvent.KEYCODE_DPAD_DOWN,
                hardwareKeyLeft = prefs[PREF_HARDWARE_KEY_LEFT] ?: KeyEvent.KEYCODE_DPAD_LEFT,
                hardwareKeyRight = prefs[PREF_HARDWARE_KEY_RIGHT] ?: KeyEvent.KEYCODE_DPAD_RIGHT,
                hardwareKeyL = prefs[PREF_HARDWARE_KEY_L] ?: KeyEvent.KEYCODE_BUTTON_L1,
                hardwareKeyR = prefs[PREF_HARDWARE_KEY_R] ?: KeyEvent.KEYCODE_BUTTON_R1,
                hardwareKeyStart = prefs[PREF_HARDWARE_KEY_START] ?: KeyEvent.KEYCODE_BUTTON_START,
                hardwareKeySelect = prefs[PREF_HARDWARE_KEY_SELECT] ?: KeyEvent.KEYCODE_BUTTON_SELECT,
                dpadMode = runCatching {
                    DpadMode.valueOf(prefs[PREF_DPAD_MODE] ?: DpadMode.WHEEL.name)
                }.getOrDefault(DpadMode.WHEEL),
                layoutOffsets = GamepadLayoutOffsets(
                    dpadX = prefs[PREF_DPAD_OFFSET_X] ?: 0f,
                    dpadY = prefs[PREF_DPAD_OFFSET_Y] ?: 0f,
                    lX = prefs[PREF_L_OFFSET_X] ?: 0f,
                    lY = prefs[PREF_L_OFFSET_Y] ?: 0f,
                    rX = prefs[PREF_R_OFFSET_X] ?: 0f,
                    rY = prefs[PREF_R_OFFSET_Y] ?: 0f,
                    aX = prefs[PREF_A_OFFSET_X] ?: (prefs[PREF_ACTION_OFFSET_X] ?: GamepadLayoutOffsets.DEFAULT.aX),
                    aY = prefs[PREF_A_OFFSET_Y] ?: (prefs[PREF_ACTION_OFFSET_Y] ?: GamepadLayoutOffsets.DEFAULT.aY),
                    bX = prefs[PREF_B_OFFSET_X] ?: (GamepadLayoutOffsets.DEFAULT.bX),
                    bY = prefs[PREF_B_OFFSET_Y] ?: (GamepadLayoutOffsets.DEFAULT.bY),
                    startX = prefs[PREF_START_OFFSET_X] ?: ((prefs[PREF_MENU_OFFSET_X] ?: 0f) + GamepadLayoutOffsets.DEFAULT.startX),
                    startY = prefs[PREF_START_OFFSET_Y] ?: ((prefs[PREF_MENU_OFFSET_Y] ?: 0f) + GamepadLayoutOffsets.DEFAULT.startY),
                    selectX = prefs[PREF_SELECT_OFFSET_X] ?: ((prefs[PREF_MENU_OFFSET_X] ?: 0f) + GamepadLayoutOffsets.DEFAULT.selectX),
                    selectY = prefs[PREF_SELECT_OFFSET_Y] ?: ((prefs[PREF_MENU_OFFSET_Y] ?: 0f) + GamepadLayoutOffsets.DEFAULT.selectY)
                ).clamped()
            )
        }
        .collectAsState(initial = GamepadPrefs())

    val gamepadConfig = remember(gamepadPrefs) {
        val scale = gamepadPrefs.buttonScale
        GamepadConfig(
            buttonAlpha = gamepadPrefs.buttonAlpha,
            dpadSize = 120f * scale,
            actionButtonSize = 72f * scale,
            shoulderButtonWidth = 80f * scale,
            shoulderButtonHeight = 40f * scale,
            menuButtonWidth = 64f * scale,
            menuButtonHeight = 28f * scale,
            vibrationEnabled = gamepadPrefs.vibrationEnabled
        )
    }

    LaunchedEffect(gamepadPrefs.layoutOffsets, isLayoutEditMode) {
        if (!isLayoutEditMode) {
            editableLayoutOffsets = gamepadPrefs.layoutOffsets
        }
    }

    LaunchedEffect(gamePath) {
        val key = cheatKeyForPath(gamePath)
        val loaded = context.settingsDataStore.data
            .map { prefs -> decodeCheatCodes(prefs[key] ?: "") }
            .first()
        cheatCodes = loaded
        cheatCodesLoaded = true
        viewModel.applyCheatCodes(loaded.filter { it.enabled }.map { it.code })
    }

    LaunchedEffect(gamePath) {
        val key = saveSlotsKeyForPath(gamePath)
        val loadedSlots = context.settingsDataStore.data
            .map { prefs -> decodeSaveSlots(prefs[key]) }
            .first()
        saveSlots = loadedSlots
        saveSlotsLoaded = true
    }

    LaunchedEffect(gamePath, cheatCodes, cheatCodesLoaded) {
        if (!cheatCodesLoaded) {
            return@LaunchedEffect
        }
        val key = cheatKeyForPath(gamePath)
        context.settingsDataStore.edit { prefs ->
            prefs[key] = encodeCheatCodes(cheatCodes)
        }
        viewModel.applyCheatCodes(cheatCodes.filter { it.enabled }.map { it.code })
    }

    LaunchedEffect(gamePath, saveSlots, saveSlotsLoaded) {
        if (!saveSlotsLoaded) {
            return@LaunchedEffect
        }
        val key = saveSlotsKeyForPath(gamePath)
        context.settingsDataStore.edit { prefs ->
            prefs[key] = encodeSaveSlots(saveSlots)
        }
    }

    LaunchedEffect(
        gamepadPrefs.keyMapA,
        gamepadPrefs.keyMapB,
        gamepadPrefs.keyMapUp,
        gamepadPrefs.keyMapDown,
        gamepadPrefs.keyMapLeft,
        gamepadPrefs.keyMapRight,
        gamepadPrefs.keyMapL,
        gamepadPrefs.keyMapR,
        gamepadPrefs.keyMapStart,
        gamepadPrefs.keyMapSelect,
        gamepadPrefs.hardwareKeyA,
        gamepadPrefs.hardwareKeyB,
        gamepadPrefs.hardwareKeyUp,
        gamepadPrefs.hardwareKeyDown,
        gamepadPrefs.hardwareKeyLeft,
        gamepadPrefs.hardwareKeyRight,
        gamepadPrefs.hardwareKeyL,
        gamepadPrefs.hardwareKeyR,
        gamepadPrefs.hardwareKeyStart,
        gamepadPrefs.hardwareKeySelect
    ) {
        fun parseTarget(name: String, fallback: InputHandler.Button): InputHandler.Button {
            return runCatching { InputHandler.Button.valueOf(name) }.getOrDefault(fallback)
        }

        fun toInputKey(button: InputHandler.Button): Int {
            return when (button) {
                InputHandler.Button.A -> InputKeys.A
                InputHandler.Button.B -> InputKeys.B
                InputHandler.Button.SELECT -> InputKeys.SELECT
                InputHandler.Button.START -> InputKeys.START
                InputHandler.Button.UP -> InputKeys.UP
                InputHandler.Button.DOWN -> InputKeys.DOWN
                InputHandler.Button.LEFT -> InputKeys.LEFT
                InputHandler.Button.RIGHT -> InputKeys.RIGHT
                InputHandler.Button.L -> InputKeys.L
                InputHandler.Button.R -> InputKeys.R
            }
        }

        val mappedA = parseTarget(gamepadPrefs.keyMapA, InputHandler.Button.A)
        val mappedB = parseTarget(gamepadPrefs.keyMapB, InputHandler.Button.B)
        val mappedUp = parseTarget(gamepadPrefs.keyMapUp, InputHandler.Button.UP)
        val mappedDown = parseTarget(gamepadPrefs.keyMapDown, InputHandler.Button.DOWN)
        val mappedLeft = parseTarget(gamepadPrefs.keyMapLeft, InputHandler.Button.LEFT)
        val mappedRight = parseTarget(gamepadPrefs.keyMapRight, InputHandler.Button.RIGHT)
        val mappedL = parseTarget(gamepadPrefs.keyMapL, InputHandler.Button.L)
        val mappedR = parseTarget(gamepadPrefs.keyMapR, InputHandler.Button.R)
        val mappedStart = parseTarget(gamepadPrefs.keyMapStart, InputHandler.Button.START)
        val mappedSelect = parseTarget(gamepadPrefs.keyMapSelect, InputHandler.Button.SELECT)

        inputHandler.setVirtualButtonMappings(
            mapOf(
                InputHandler.Button.A to mappedA,
                InputHandler.Button.B to mappedB,
                InputHandler.Button.UP to mappedUp,
                InputHandler.Button.DOWN to mappedDown,
                InputHandler.Button.LEFT to mappedLeft,
                InputHandler.Button.RIGHT to mappedRight,
                InputHandler.Button.L to mappedL,
                InputHandler.Button.R to mappedR,
                InputHandler.Button.START to mappedStart,
                InputHandler.Button.SELECT to mappedSelect
            )
        )

        val coreInput = CoreInputHandler.getInstance()
        coreInput.clearKeyMapping()

        fun setHardwareMapping(androidKeyCode: Int, target: InputHandler.Button) {
            if (androidKeyCode != KeyEvent.KEYCODE_UNKNOWN) {
                coreInput.setKeyMapping(androidKeyCode, toInputKey(target))
            }
        }

        setHardwareMapping(gamepadPrefs.hardwareKeyA, mappedA)
        setHardwareMapping(gamepadPrefs.hardwareKeyB, mappedB)
        setHardwareMapping(gamepadPrefs.hardwareKeyUp, mappedUp)
        setHardwareMapping(gamepadPrefs.hardwareKeyDown, mappedDown)
        setHardwareMapping(gamepadPrefs.hardwareKeyLeft, mappedLeft)
        setHardwareMapping(gamepadPrefs.hardwareKeyRight, mappedRight)
        setHardwareMapping(gamepadPrefs.hardwareKeyL, mappedL)
        setHardwareMapping(gamepadPrefs.hardwareKeyR, mappedR)
        setHardwareMapping(gamepadPrefs.hardwareKeyStart, mappedStart)
        setHardwareMapping(gamepadPrefs.hardwareKeySelect, mappedSelect)
        setHardwareMapping(KeyEvent.KEYCODE_ENTER, mappedStart)
        setHardwareMapping(KeyEvent.KEYCODE_SPACE, mappedSelect)
    }

    LaunchedEffect(gamePath) {
        viewModel.loadGame(gamePath)
    }

    LaunchedEffect(
        gamepadPrefs.audioSampleRate,
        gamepadPrefs.audioBufferSize,
        gamepadPrefs.audioEnabled,
        gamepadPrefs.masterVolume,
        gamepadPrefs.audioFilterEnabled,
        gamepadPrefs.audioFilterLevel
    ) {
        viewModel.updateAudioConfig(
            sampleRate = gamepadPrefs.audioSampleRate,
            bufferSize = gamepadPrefs.audioBufferSize,
            enabled = gamepadPrefs.audioEnabled,
            masterVolume = gamepadPrefs.masterVolume,
            filterEnabled = gamepadPrefs.audioFilterEnabled,
            filterLevel = gamepadPrefs.audioFilterLevel
        )
    }

    LaunchedEffect(
        gamepadPrefs.frameSkipEnabled,
        gamepadPrefs.frameSkipThrottlePercent,
        gamepadPrefs.frameSkipInterval,
        gamepadPrefs.interframeBlending,
        gamepadPrefs.idleLoopRemoval,
        gamepadPrefs.gbControllerRumble
    ) {
        viewModel.updateGameOptions(
            frameSkipEnabled = gamepadPrefs.frameSkipEnabled,
            frameSkipThrottlePercent = gamepadPrefs.frameSkipThrottlePercent,
            frameSkipInterval = gamepadPrefs.frameSkipInterval,
            interframeBlending = gamepadPrefs.interframeBlending,
            idleLoopRemoval = gamepadPrefs.idleLoopRemoval,
            gbControllerRumble = gamepadPrefs.gbControllerRumble
        )
    }

    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    DisposableEffect(gamepadPrefs.immersiveMode) {
        val activity = context as? Activity
        if (activity != null) {
            WindowCompat.setDecorFitsSystemWindows(activity.window, !gamepadPrefs.immersiveMode)
            val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
            if (gamepadPrefs.immersiveMode) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            if (activity != null) {
                WindowCompat.setDecorFitsSystemWindows(activity.window, true)
                val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            VideoRenderer(
                frameFlow = viewModel.videoFrame,
                videoFilter = gamepadPrefs.videoFilter,
                aspectRatio = gamepadPrefs.aspectRatio,
                showFps = gamepadPrefs.showFps,
                modifier = Modifier.fillMaxSize()
            )

            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage ?: "",
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color(0x99000000))
                        .padding(8.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 2.dp)
                .background(
                    color = Color(0x7A000000),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "菜单",
                    tint = Color.White
                )
            }

            IconButton(onClick = { viewModel.quickSave() }) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "快速存档",
                    tint = Color.White
                )
            }

            IconButton(onClick = {
                scope.launch {
                    context.settingsDataStore.edit { prefs ->
                        prefs[PREF_IMMERSIVE_MODE] = !gamepadPrefs.immersiveMode
                    }
                }
            }) {
                Icon(
                    imageVector = if (gamepadPrefs.immersiveMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = "沉浸模式",
                    tint = Color.White
                )
            }
        }

        // 虚拟手柄覆盖层（右下角）
        if (gamepadPrefs.showVirtualControls) {
            VirtualGamepad(
                inputHandler = inputHandler,
                config = gamepadConfig,
                dpadMode = gamepadPrefs.dpadMode,
                layoutOffsets = editableLayoutOffsets,
                layoutEditMode = isLayoutEditMode,
                onLayoutOffsetsChange = { next ->
                    editableLayoutOffsets = next.clamped()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }

        if (isLayoutEditMode) {
            Text(
                text = "布局编辑中：拖动按键后在菜单点击“保存布局”",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, top = 12.dp)
                    .background(Color(0x7A000000), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        // 游戏菜单对话框
        if (showMenu) {
            GameMenu(
                onDismiss = { showMenu = false },
                onSaveState = { slot -> viewModel.saveState(slot) },
                onLoadState = { slot -> viewModel.loadState(slot) },
                onFastForward = { speed -> viewModel.setFastForwardSpeed(speed) },
                onTargetFps = { fps -> viewModel.setTargetFps(fps) },
                onResumeGame = { },
                onTogglePause = { viewModel.togglePauseResume() },
                onToggleMute = { viewModel.toggleMute() },
                onResetGame = { viewModel.resetGame() },
                onExitGame = {
                    viewModel.exitGame {
                        onBackToList()
                    }
                },
                onAddCheatCode = { code ->
                    val normalized = code.trim()
                    if (normalized.isNotEmpty()) {
                        cheatCodes = cheatCodes + CheatCodeItem(
                            id = System.nanoTime(),
                            code = normalized,
                            enabled = true
                        )
                    }
                },
                onToggleCheatCode = { id, enabled ->
                    cheatCodes = cheatCodes.map { entry ->
                        if (entry.id == id) entry.copy(enabled = enabled) else entry
                    }
                },
                onRemoveCheatCode = { id ->
                    cheatCodes = cheatCodes.filterNot { it.id == id }
                },
                onClearCheatCodes = {
                    cheatCodes = emptyList()
                },
                onOpenSettings = {
                    showMenu = false
                    onOpenSettings()
                },
                onOpenNetplay = {
                    showMenu = false
                    onOpenNetplay()
                },
                onToggleLayoutEditMode = {
                    isLayoutEditMode = !isLayoutEditMode
                    if (!isLayoutEditMode) {
                        editableLayoutOffsets = gamepadPrefs.layoutOffsets
                    }
                },
                onSaveLayout = {
                    val offsets = editableLayoutOffsets.clamped()
                    editableLayoutOffsets = offsets
                    scope.launch {
                        context.settingsDataStore.edit { prefs ->
                            prefs[PREF_DPAD_OFFSET_X] = offsets.dpadX
                            prefs[PREF_DPAD_OFFSET_Y] = offsets.dpadY
                            prefs[PREF_L_OFFSET_X] = offsets.lX
                            prefs[PREF_L_OFFSET_Y] = offsets.lY
                            prefs[PREF_R_OFFSET_X] = offsets.rX
                            prefs[PREF_R_OFFSET_Y] = offsets.rY
                            prefs[PREF_A_OFFSET_X] = offsets.aX
                            prefs[PREF_A_OFFSET_Y] = offsets.aY
                            prefs[PREF_B_OFFSET_X] = offsets.bX
                            prefs[PREF_B_OFFSET_Y] = offsets.bY
                            prefs[PREF_START_OFFSET_X] = offsets.startX
                            prefs[PREF_START_OFFSET_Y] = offsets.startY
                            prefs[PREF_SELECT_OFFSET_X] = offsets.selectX
                            prefs[PREF_SELECT_OFFSET_Y] = offsets.selectY
                        }
                        isLayoutEditMode = false
                    }
                },
                onResetLayout = {
                    editableLayoutOffsets = GamepadLayoutOffsets.DEFAULT
                },
                saveSlots = saveSlots,
                onSaveSlot = { slot -> viewModel.saveState(slot) },
                onLoadSlot = { slot -> viewModel.loadState(slot) },
                onAddSaveSlot = {
                    val next = (saveSlots.maxOrNull() ?: 0) + 1
                    saveSlots = saveSlots + next
                },
                onRemoveSaveSlot = { slot ->
                    saveSlots = saveSlots.filterNot { it == slot }
                },
                currentFastForwardSpeed = uiState.fastForwardSpeed,
                currentTargetFps = uiState.targetFps,
                isPaused = uiState.isPaused,
                isMuted = uiState.isMuted,
                isLayoutEditMode = isLayoutEditMode,
                cheatCodes = cheatCodes
            )
        }
    }
}

private data class GamepadPrefs(
    val videoFilter: VideoFilter = VideoFilter.NEAREST,
    val aspectRatio: AspectRatio = AspectRatio.ORIGINAL,
    val showFps: Boolean = false,
    val buttonAlpha: Float = 0.8f,
    val buttonScale: Float = 1f,
    val vibrationEnabled: Boolean = true,
    val immersiveMode: Boolean = false,
    val showVirtualControls: Boolean = true,
    val audioEnabled: Boolean = true,
    val masterVolume: Float = 100f,
    val audioSampleRate: Int = 44100,
    val audioBufferSize: Int = 8192,
    val audioFilterEnabled: Boolean = true,
    val audioFilterLevel: Int = 60,
    val frameSkipEnabled: Boolean = false,
    val frameSkipThrottlePercent: Int = 33,
    val frameSkipInterval: Int = 0,
    val interframeBlending: Boolean = false,
    val idleLoopRemoval: String = "REMOVE_KNOWN",
    val gbControllerRumble: Boolean = false,
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
    val dpadMode: DpadMode = DpadMode.WHEEL,
    val layoutOffsets: GamepadLayoutOffsets = GamepadLayoutOffsets.DEFAULT
)

@Composable
fun VideoRenderer(
    frameFlow: StateFlow<ByteArray?>,
    videoFilter: VideoFilter,
    aspectRatio: AspectRatio,
    showFps: Boolean,
    modifier: Modifier = Modifier
) {
    val frameData by frameFlow.collectAsState()
    val width = 240
    val height = 160
    val bitmap = remember { Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888) }
    val pixels = remember { IntArray(width * height) }
    var fps by remember { mutableStateOf(0) }
    var fpsFrameCount by remember { mutableStateOf(0) }
    var fpsLastTs by remember { mutableStateOf(System.nanoTime()) }

    LaunchedEffect(frameData) {
        val frame = frameData
        if (frame != null && frame.size >= width * height * 2) {
            var src = 0
            var i = 0
            while (i < pixels.size) {
                val lo = frame[src].toInt() and 0xFF
                val hi = frame[src + 1].toInt() and 0xFF
                val rgb565 = (hi shl 8) or lo

                val r = ((rgb565 shr 11) and 0x1F) * 255 / 31
                val g = ((rgb565 shr 5) and 0x3F) * 255 / 63
                val b = (rgb565 and 0x1F) * 255 / 31
                pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

                src += 2
                i++
            }
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

            if (showFps) {
                fpsFrameCount += 1
                val now = System.nanoTime()
                val elapsedNs = now - fpsLastTs
                if (elapsedNs >= 1_000_000_000L) {
                    fps = ((fpsFrameCount * 1_000_000_000L) / elapsedNs).toInt()
                    fpsFrameCount = 0
                    fpsLastTs = now
                }
            }
        }
    }

    val filterQuality = when (videoFilter) {
        VideoFilter.NEAREST -> FilterQuality.None
        VideoFilter.LINEAR -> FilterQuality.Medium
        VideoFilter.CRT -> FilterQuality.None
        VideoFilter.ADVANCED -> FilterQuality.High
    }

    val colorFilter = when (videoFilter) {
        VideoFilter.ADVANCED -> {
            val matrix = ColorMatrix()
            matrix.setToSaturation(1.12f)
            ColorFilter.colorMatrix(matrix)
        }
        else -> null
    }

    BoxWithConstraints(modifier = modifier.background(Color.Black)) {
        val density = LocalDensity.current
        val maxWpx = with(density) { maxWidth.toPx() }
        val maxHpx = with(density) { maxHeight.toPx() }

        val imageModifier = when (aspectRatio) {
            AspectRatio.STRETCH -> Modifier.fillMaxSize()
            AspectRatio.FIT -> Modifier.fillMaxSize()
            AspectRatio.ORIGINAL -> Modifier.size(240.dp, 160.dp)
            AspectRatio.INTEGER_SCALE -> {
                val scale = floor(min(maxWpx / 240f, maxHpx / 160f)).toInt().coerceAtLeast(1)
                val scaledW = with(density) { (240f * scale).dp }
                val scaledH = with(density) { (160f * scale).dp }
                Modifier.size(scaledW, scaledH)
            }
        }

        val contentScale = when (aspectRatio) {
            AspectRatio.STRETCH -> ContentScale.FillBounds
            AspectRatio.FIT, AspectRatio.ORIGINAL, AspectRatio.INTEGER_SCALE -> ContentScale.Fit
        }

        if (frameData != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Game Frame",
                    modifier = imageModifier,
                    contentScale = contentScale,
                    filterQuality = filterQuality,
                    colorFilter = colorFilter
                )

                if (videoFilter == VideoFilter.CRT) {
                    Canvas(modifier = imageModifier) {
                        val stripe = 3f
                        var y = 0f
                        while (y < size.height) {
                            drawRect(
                                color = Color.Black.copy(alpha = 0.12f),
                                topLeft = androidx.compose.ui.geometry.Offset(0f, y),
                                size = androidx.compose.ui.geometry.Size(size.width, stripe)
                            )
                            y += stripe * 2f
                        }
                    }
                }
            }
        }

        if (showFps) {
            Text(
                text = "FPS: $fps",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .background(Color(0x66000000), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
