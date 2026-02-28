package com.jboy.emulator.ui.settings

import android.content.Context
import android.net.Uri
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jboy.emulator.input.HardwareKeyCaptureBus
import com.jboy.emulator.ui.gamepad.DpadMode
import com.jboy.emulator.ui.i18n.l10n
import java.io.File
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var learningAction by remember { mutableStateOf<HardwareButtonAction?>(null) }

    LaunchedEffect(learningAction) {
        val action = learningAction ?: return@LaunchedEffect
        val keyCode = HardwareKeyCaptureBus.keyEvents.first()
        viewModel.updateHardwareKey(action, keyCode)
        Toast.makeText(
            context,
            context.l10n("${action.displayName} 已绑定: ${keyCodeLabel(keyCode)}"),
            Toast.LENGTH_SHORT
        ).show()
        learningAction = null
    }

    val biosPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // Copy BIOS file to app private storage
            val biosFile = copyBiosToInternalStorage(context, it)
            biosFile?.let { file ->
                viewModel.updateBiosPath(file.absolutePath)
                Toast.makeText(context, context.l10n("BIOS已加载"), Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(l10n("设置")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, l10n("返回"))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 主题设置
            SettingsSection(
                title = "主题设置",
                icon = Icons.Default.Palette
            ) {
                SwitchSetting(
                    title = "启用自定义主题",
                    checked = settings.themeCustomEnabled,
                    onCheckedChange = { viewModel.updateThemeCustomEnabled(it) }
                )

                DropdownSetting(
                    title = "主题预设",
                    options = ThemePreset.entries.map { it.displayName },
                    selectedIndex = ThemePreset.entries.indexOf(settings.themePreset).let { if (it >= 0) it else 0 },
                    onSelect = { idx ->
                        val preset = ThemePreset.entries[idx]
                        viewModel.updateThemePreset(preset)
                        viewModel.updateThemePrimaryHex(preset.primaryHex)
                        viewModel.updateThemeSecondaryHex(preset.secondaryHex)
                        viewModel.updateThemeTertiaryHex(preset.tertiaryHex)
                        viewModel.updateThemeBackgroundHex(preset.backgroundHex)
                        viewModel.updateThemeSurfaceHex(preset.surfaceHex)
                    }
                )

                ThemeHexSetting(
                    title = "主色 Primary",
                    hexValue = settings.themePrimaryHex,
                    swatches = listOf("#00696B", "#2E7D32", "#2456D1", "#B85600", "#A11D4A"),
                    enabled = settings.themeCustomEnabled,
                    onHexCommit = { viewModel.updateThemePrimaryHex(it) }
                )

                ThemeHexSetting(
                    title = "辅色 Secondary",
                    hexValue = settings.themeSecondaryHex,
                    swatches = listOf("#4A6364", "#4E6A50", "#4A5D87", "#8B5D3B", "#5C5C7A"),
                    enabled = settings.themeCustomEnabled,
                    onHexCommit = { viewModel.updateThemeSecondaryHex(it) }
                )

                ThemeHexSetting(
                    title = "强调色 Tertiary",
                    hexValue = settings.themeTertiaryHex,
                    swatches = listOf("#4B607B", "#6C7A35", "#6A4CA5", "#AA4C39", "#006A5C"),
                    enabled = settings.themeCustomEnabled,
                    onHexCommit = { viewModel.updateThemeTertiaryHex(it) }
                )

                ThemeHexSetting(
                    title = "背景色 Background",
                    hexValue = settings.themeBackgroundHex,
                    swatches = listOf("#FAFDFC", "#F6FBF4", "#F4F7FF", "#FFF7F1", "#0F1113"),
                    enabled = settings.themeCustomEnabled,
                    onHexCommit = { viewModel.updateThemeBackgroundHex(it) }
                )

                ThemeHexSetting(
                    title = "表面色 Surface",
                    hexValue = settings.themeSurfaceHex,
                    swatches = listOf("#FAFDFC", "#F8FCF6", "#F6F8FF", "#FFF8F3", "#171A1E"),
                    enabled = settings.themeCustomEnabled,
                    onHexCommit = { viewModel.updateThemeSurfaceHex(it) }
                )
            }

            // 视频设置
            SettingsSection(
                title = "视频设置",
                icon = Icons.Default.VideogameAsset
            ) {
                // 视频滤镜
                DropdownSetting(
                    title = "视频滤镜",
                    options = VideoFilter.entries.map { it.displayName },
                    selectedIndex = settings.videoFilter.ordinal,
                    onSelect = { viewModel.updateVideoFilter(VideoFilter.entries[it]) }
                )

                // 屏幕比例
                DropdownSetting(
                    title = "屏幕比例",
                    options = AspectRatio.entries.map { it.displayName },
                    selectedIndex = settings.aspectRatio.ordinal,
                    onSelect = { viewModel.updateAspectRatio(AspectRatio.entries[it]) }
                )

                // 显示FPS
                SwitchSetting(
                    title = "显示FPS",
                    checked = settings.showFps,
                    onCheckedChange = { viewModel.updateShowFps(it) }
                )
            }

            // 音频设置
            SettingsSection(
                title = "音频设置",
                icon = Icons.Default.VolumeUp
            ) {
                val sampleRateOptions = listOf(22050, 32000, 44100, 48000)
                val bufferSizeOptions = listOf(2048, 4096, 8192, 16384)

                // 启用音频
                SwitchSetting(
                    title = "启用音频",
                    checked = settings.audioEnabled,
                    onCheckedChange = { viewModel.updateAudioEnabled(it) }
                )

                // 主音量
                SliderSetting(
                    title = "主音量",
                    value = settings.masterVolume,
                    onValueChange = { viewModel.updateMasterVolume(it) },
                    valueRange = 0f..100f,
                    valueFormatter = { "${it.toInt()}%" },
                    enabled = settings.audioEnabled
                )

                DropdownSetting(
                    title = "音频采样率",
                    options = sampleRateOptions.map { "${it}Hz" },
                    selectedIndex = sampleRateOptions.indexOf(settings.audioSampleRate).let { if (it >= 0) it else 2 },
                    onSelect = { index -> viewModel.updateAudioSampleRate(sampleRateOptions[index]) }
                )

                DropdownSetting(
                    title = "音频缓冲区",
                    options = bufferSizeOptions.map { it.toString() },
                    selectedIndex = bufferSizeOptions.indexOf(settings.audioBufferSize).let { if (it >= 0) it else 2 },
                    onSelect = { index -> viewModel.updateAudioBufferSize(bufferSizeOptions[index]) }
                )

                SwitchSetting(
                    title = "启用音频过滤",
                    checked = settings.audioFilterEnabled,
                    onCheckedChange = { viewModel.updateAudioFilterEnabled(it) }
                )

                SliderSetting(
                    title = "音频过滤器级别",
                    value = settings.audioFilterLevel.toFloat(),
                    onValueChange = { viewModel.updateAudioFilterLevel(it.toInt()) },
                    valueRange = 0f..100f,
                    valueFormatter = { "${it.toInt()}" },
                    enabled = settings.audioFilterEnabled
                )
            }

            // 控制设置
            SettingsSection(
                title = "控制设置",
                icon = Icons.Default.Settings
            ) {
                // 手柄透明度
                SliderSetting(
                    title = "手柄透明度",
                    value = settings.controllerOpacity,
                    onValueChange = { viewModel.updateControllerOpacity(it) },
                    valueRange = 0.2f..1f,
                    valueFormatter = { "${(it * 100).toInt()}%" }
                )

                // 按钮大小
                SliderSetting(
                    title = "按钮大小",
                    value = settings.buttonSize,
                    onValueChange = { viewModel.updateButtonSize(it) },
                    valueRange = 0.5f..1.5f,
                    valueFormatter = { String.format("%.2fx", it) }
                )

                DropdownSetting(
                    title = "方向控制模式",
                    options = DpadMode.entries.map { it.displayName },
                    selectedIndex = DpadMode.entries.indexOf(settings.dpadMode).let { if (it >= 0) it else 0 },
                    onSelect = { index -> viewModel.updateDpadMode(DpadMode.entries[index]) }
                )

                // 振动反馈
                SwitchSetting(
                    title = "振动反馈",
                    checked = settings.vibrationEnabled,
                    onCheckedChange = { viewModel.updateVibrationEnabled(it) }
                )

                SwitchSetting(
                    title = "启用Game Boy控制器震动",
                    checked = settings.gbControllerRumble,
                    onCheckedChange = { viewModel.updateGbControllerRumble(it) }
                )

                val mappingTargets = VirtualKeyTarget.entries
                fun hardwareKeyCode(action: HardwareButtonAction): Int {
                    return when (action) {
                        HardwareButtonAction.A -> settings.hardwareKeyA
                        HardwareButtonAction.B -> settings.hardwareKeyB
                        HardwareButtonAction.UP -> settings.hardwareKeyUp
                        HardwareButtonAction.DOWN -> settings.hardwareKeyDown
                        HardwareButtonAction.LEFT -> settings.hardwareKeyLeft
                        HardwareButtonAction.RIGHT -> settings.hardwareKeyRight
                        HardwareButtonAction.L -> settings.hardwareKeyL
                        HardwareButtonAction.R -> settings.hardwareKeyR
                        HardwareButtonAction.START -> settings.hardwareKeyStart
                        HardwareButtonAction.SELECT -> settings.hardwareKeySelect
                    }
                }

                DropdownSetting(
                    title = "映射 A 键",
                    options = mappingTargets.map { it.displayName },
                    selectedIndex = mappingTargets.indexOf(settings.keyMapA).let { if (it >= 0) it else 0 },
                    onSelect = { index -> viewModel.updateKeyMapA(mappingTargets[index]) }
                )
                DropdownSetting(
                    title = "映射 B 键",
                    options = mappingTargets.map { it.displayName },
                    selectedIndex = mappingTargets.indexOf(settings.keyMapB).let { if (it >= 0) it else 1 },
                    onSelect = { index -> viewModel.updateKeyMapB(mappingTargets[index]) }
                )
                DropdownSetting(
                    title = "映射 UP 键",
                    options = mappingTargets.map { it.displayName },
                    selectedIndex = mappingTargets.indexOf(settings.keyMapUp).let { if (it >= 0) it else 2 },
                    onSelect = { index -> viewModel.updateKeyMapUp(mappingTargets[index]) }
                )
                DropdownSetting(
                    title = "映射 DOWN 键",
                    options = mappingTargets.map { it.displayName },
                    selectedIndex = mappingTargets.indexOf(settings.keyMapDown).let { if (it >= 0) it else 3 },
                    onSelect = { index -> viewModel.updateKeyMapDown(mappingTargets[index]) }
                )
                DropdownSetting(
                    title = "映射 LEFT 键",
                    options = mappingTargets.map { it.displayName },
                    selectedIndex = mappingTargets.indexOf(settings.keyMapLeft).let { if (it >= 0) it else 4 },
                    onSelect = { index -> viewModel.updateKeyMapLeft(mappingTargets[index]) }
                )
                DropdownSetting(
                    title = "映射 RIGHT 键",
                    options = mappingTargets.map { it.displayName },
                    selectedIndex = mappingTargets.indexOf(settings.keyMapRight).let { if (it >= 0) it else 5 },
                    onSelect = { index -> viewModel.updateKeyMapRight(mappingTargets[index]) }
                )
                DropdownSetting(
                    title = "映射 L 键",
                    options = mappingTargets.map { it.displayName },
                    selectedIndex = mappingTargets.indexOf(settings.keyMapL).let { if (it >= 0) it else 6 },
                    onSelect = { index -> viewModel.updateKeyMapL(mappingTargets[index]) }
                )
                DropdownSetting(
                    title = "映射 R 键",
                    options = mappingTargets.map { it.displayName },
                    selectedIndex = mappingTargets.indexOf(settings.keyMapR).let { if (it >= 0) it else 7 },
                    onSelect = { index -> viewModel.updateKeyMapR(mappingTargets[index]) }
                )
                DropdownSetting(
                    title = "映射 START",
                    options = mappingTargets.map { it.displayName },
                    selectedIndex = mappingTargets.indexOf(settings.keyMapStart).let { if (it >= 0) it else 8 },
                    onSelect = { index -> viewModel.updateKeyMapStart(mappingTargets[index]) }
                )
                DropdownSetting(
                    title = "映射 SELECT",
                    options = mappingTargets.map { it.displayName },
                    selectedIndex = mappingTargets.indexOf(settings.keyMapSelect).let { if (it >= 0) it else 9 },
                    onSelect = { index -> viewModel.updateKeyMapSelect(mappingTargets[index]) }
                )

                Text(
                    text = l10n("实体手柄映射（蓝牙/OTG）"),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (learningAction != null) {
                    Text(
                        text = l10n("学习中：请按下 ${learningAction?.displayName ?: "按键"}"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                HardwareButtonAction.entries.forEach { action ->
                    HardwareMappingSetting(
                        title = action.displayName,
                        keyCode = hardwareKeyCode(action),
                        isLearning = learningAction == action,
                        onLearn = { learningAction = action },
                        onReset = { viewModel.resetHardwareKey(action) }
                    )
                }

                SwitchSetting(
                    title = "显示虚拟按键",
                    checked = settings.showVirtualControls,
                    onCheckedChange = { viewModel.updateShowVirtualControls(it) }
                )
            }

            // 游戏设置
            SettingsSection(
                title = "游戏设置",
                icon = Icons.Default.Speed
            ) {
                SwitchSetting(
                    title = "退出时自动存档",
                    checked = settings.autoSaveOnExit,
                    onCheckedChange = { viewModel.updateAutoSaveOnExit(it) }
                )

                SwitchSetting(
                    title = "切后台自动暂停",
                    checked = settings.pauseOnBackground,
                    onCheckedChange = { viewModel.updatePauseOnBackground(it) }
                )

                SwitchSetting(
                    title = "启用跳帧",
                    checked = settings.frameSkipEnabled,
                    onCheckedChange = { viewModel.updateFrameSkipEnabled(it) }
                )

                SliderSetting(
                    title = "跳帧搁置（百分比）",
                    value = settings.frameSkipThrottlePercent.toFloat(),
                    onValueChange = { viewModel.updateFrameSkipThrottlePercent(it.toInt()) },
                    valueRange = 0f..100f,
                    valueFormatter = { "${it.toInt()}%" },
                    enabled = settings.frameSkipEnabled
                )

                val frameSkipIntervals = listOf(0, 1, 2, 3, 4, 5, 6)
                DropdownSetting(
                    title = "跳帧间隔",
                    options = frameSkipIntervals.map { if (it == 0) "禁用" else "$it" },
                    selectedIndex = frameSkipIntervals.indexOf(settings.frameSkipInterval).let { if (it >= 0) it else 0 },
                    onSelect = { index -> viewModel.updateFrameSkipInterval(frameSkipIntervals[index]) }
                )

                SwitchSetting(
                    title = "帧间混合",
                    checked = settings.interframeBlending,
                    onCheckedChange = { viewModel.updateInterframeBlending(it) }
                )

                DropdownSetting(
                    title = "空闲循环移除",
                    options = IdleLoopRemovalMode.entries.map { it.displayName },
                    selectedIndex = IdleLoopRemovalMode.entries.indexOf(settings.idleLoopRemoval).let { if (it >= 0) it else 0 },
                    onSelect = { index -> viewModel.updateIdleLoopRemoval(IdleLoopRemovalMode.entries[index]) }
                )
            }

            // 系统设置
            SettingsSection(
                title = "系统设置",
                icon = Icons.Default.Storage
            ) {
                // BIOS文件
                ButtonSetting(
                    title = "BIOS文件",
                    subtitle = settings.biosPath?.let { File(it).name } ?: "未选择（使用HLE）",
                    onClick = { biosPickerLauncher.launch(arrayOf("*/*")) }
                )

                // 快进速度
                DropdownSetting(
                    title = "快进默认速度",
                    options = FastForwardSpeed.entries.map { it.displayName },
                    selectedIndex = settings.fastForwardSpeed.ordinal,
                    onSelect = { viewModel.updateFastForwardSpeed(FastForwardSpeed.entries[it]) }
                )

                // 语言选择
                DropdownSetting(
                    title = "语言",
                    options = listOf("简体中文", "繁體中文", "English", "日本語"),
                    selectedIndex = getLanguageIndex(settings.language),
                    onSelect = {
                        val languageCode = getLanguageCode(it)
                        viewModel.updateLanguage(languageCode)
                        applyAppLanguage(languageCode)
                    }
                )

                SwitchSetting(
                    title = "沉浸模式",
                    checked = settings.immersiveMode,
                    onCheckedChange = { viewModel.updateImmersiveMode(it) }
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = l10n(title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun DropdownSetting(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(l10n(title))
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(l10n(options[selectedIndex]))
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { Text(l10n(option)) },
                        onClick = {
                            onSelect(index)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SwitchSetting(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(l10n(title))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SliderSetting(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueFormatter: (Float) -> String = { "${it.toInt()}" },
    enabled: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(l10n(title))
            Text(valueFormatter(value))
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled
        )
    }
}

@Composable
private fun ThemeHexSetting(
    title: String,
    hexValue: String,
    swatches: List<String>,
    enabled: Boolean,
    onHexCommit: (String) -> Unit
) {
    var text by remember(hexValue) { mutableStateOf(hexValue) }
    val normalized = normalizeHexInput(text)
    val isValid = isHexColor(normalized)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(l10n(title))
            ColorPreview(hex = hexValue)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(9) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                enabled = enabled,
                label = { Text("HEX") },
                placeholder = { Text("#RRGGBB") },
                isError = enabled && text.isNotBlank() && !isValid
            )
            Button(
                onClick = {
                    if (isValid) {
                        onHexCommit(normalized)
                    }
                },
                enabled = enabled && isValid
            ) {
                Text(l10n("应用"))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            swatches.forEach { hex ->
                ColorPreview(
                    hex = hex,
                    shape = CircleShape,
                    modifier = Modifier.clickable(enabled = enabled) {
                        text = hex
                        onHexCommit(hex)
                    }
                )
            }
        }
    }
}

@Composable
private fun ColorPreview(
    hex: String,
    shape: Shape = RoundedCornerShape(8.dp),
    modifier: Modifier = Modifier
) {
    val color = parseHexColorOrDefault(hex)
    Box(
        modifier = modifier
            .size(28.dp)
            .background(color, shape)
    )
}

private fun normalizeHexInput(input: String): String {
    val cleaned = input.trim().uppercase().replace("[^#0-9A-F]".toRegex(), "")
    return when {
        cleaned.startsWith("#") -> cleaned
        cleaned.isEmpty() -> ""
        else -> "#$cleaned"
    }
}

private fun isHexColor(input: String): Boolean {
    return Regex("^#[0-9A-F]{6}$").matches(input)
}

private fun parseHexColorOrDefault(hex: String): Color {
    val normalized = normalizeHexInput(hex)
    if (!isHexColor(normalized)) {
        return Color(0xFF666666)
    }

    val rgb = normalized.removePrefix("#").toLongOrNull(16) ?: return Color(0xFF666666)
    val argb = (0xFF000000 or rgb).toInt()
    return Color(argb)
}

@Composable
private fun HardwareMappingSetting(
    title: String,
    keyCode: Int,
    isLearning: Boolean,
    onLearn: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = l10n(title))
            Text(
                text = l10n(keyCodeLabel(keyCode)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = onReset) {
                Text(l10n("默认"))
            }
            Button(onClick = onLearn) {
                Text(l10n(if (isLearning) "等待中" else "学习"))
            }
        }
    }
}

private fun keyCodeLabel(keyCode: Int): String {
    return if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
        "未绑定"
    } else {
        KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
    }
}

private fun applyAppLanguage(language: String) {
    val languageTag = when (language) {
        "zh-CN" -> "zh-CN"
        "zh-TW" -> "zh-TW"
        "en" -> "en"
        "ja" -> "ja"
        else -> "zh-CN"
    }
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
}

@Composable
private fun ButtonSetting(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(l10n(title))
            Text(
                text = l10n(subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onClick) {
            Text(l10n("选择"))
        }
    }
}

private fun copyBiosToInternalStorage(context: Context, uri: Uri): File? {
    return try {
        val biosDir = File(context.filesDir, "bios").apply { mkdirs() }
        val destFile = File(biosDir, "gba_bios.bin")
        
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        destFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun getLanguageIndex(language: String): Int = when (language) {
    "zh-CN" -> 0
    "zh-TW" -> 1
    "en" -> 2
    "ja" -> 3
    else -> 0
}

private fun getLanguageCode(index: Int): String = when (index) {
    0 -> "zh-CN"
    1 -> "zh-TW"
    2 -> "en"
    3 -> "ja"
    else -> "zh-CN"
}

// 枚举定义
enum class VideoFilter(val displayName: String) {
    NEAREST("最邻近"),
    LINEAR("线性"),
    CRT("CRT"),
    ADVANCED("高级")
}

enum class AspectRatio(val displayName: String) {
    ORIGINAL("原始"),
    STRETCH("拉伸"),
    FIT("适应屏幕"),
    INTEGER_SCALE("整数缩放")
}

enum class FastForwardSpeed(val displayName: String, val speed: Float) {
    X2("2x", 2f),
    X4("4x", 4f),
    X8("8x", 8f),
    UNLIMITED("无限制", 16f)
}
