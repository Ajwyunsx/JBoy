package com.jboy.emulator.ui.gamepad

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalView

/**
 * 按钮状态扩展函数
 * 用于处理按下/释放状态的视觉效果
 */

/**
 * 为按钮添加游戏手柄风格的按下效果
 * 包含缩放、透明度变化和可选的振动反馈
 */
fun Modifier.gamepadButtonEffect(
    config: GamepadConfig,
    isPressed: Boolean,
    enabled: Boolean = true
): Modifier = composed {
    val scale = if (isPressed && enabled) config.pressedScale else 1f
    val alpha = if (isPressed && enabled) config.pressedAlpha else config.buttonAlpha

    this
        .scale(scale)
        .alpha(if (enabled) alpha else 0.3f)
}

/**
 * 带振动反馈的按钮修饰符
 */
fun Modifier.gamepadClickable(
    config: GamepadConfig,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    enabled: Boolean = true
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current

    // 振动反馈
    LaunchedEffect(isPressed) {
        if (isPressed && config.vibrationEnabled && enabled) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    this
        .gamepadButtonEffect(config = config, isPressed = isPressed, enabled = enabled)
        .clickable(
            interactionSource = interactionSource,
            indication = rememberRipple(
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f)
            ),
            enabled = enabled,
            onClick = {}
        )
}

/**
 * 简化版的游戏手柄按钮效果
 * 仅处理视觉效果，不包含点击逻辑
 */
@Composable
fun rememberGamepadButtonState(
    config: GamepadConfig
): Pair<Float, Float> {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale = if (isPressed) config.pressedScale else 1f
    val alpha = if (isPressed) config.pressedAlpha else config.buttonAlpha

    return Pair(scale, alpha)
}

/**
 * 创建自定义的游戏手柄按钮指示器
 */
@Composable
fun gamepadButtonIndication(
    color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f),
    radius: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp.Unspecified
): Indication {
    return rememberRipple(color = color, radius = radius)
}

/**
 * 振动反馈扩展函数
 */
fun View.performGamepadHaptic(config: GamepadConfig) {
    if (config.vibrationEnabled) {
        this.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }
}

/**
 * 振动反馈扩展函数（长按）
 */
fun View.performGamepadHapticLong(config: GamepadConfig) {
    if (config.vibrationEnabled) {
        this.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}

/**
 * 按钮位置数据类
 */
data class ButtonPosition(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

/**
 * 创建按钮位置
 */
fun createButtonPosition(
    x: Float,
    y: Float,
    width: Float,
    height: Float
): ButtonPosition = ButtonPosition(x, y, width, height)
