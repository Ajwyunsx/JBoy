package com.jboy.emulator.ui.gamepad

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jboy.emulator.input.InputHandler

/**
 * 菜单按钮组件
 * 包含 L/R 肩键和 Start/Select 按钮
 */
@Composable
fun MenuButtons(
    inputHandler: InputHandler,
    config: GamepadConfig,
    inputEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // L 肩键
        ShoulderButton(
            text = "L",
            isLeft = true,
            onPress = { inputHandler.pressButton(InputHandler.Button.L) },
            onRelease = { inputHandler.releaseButton(InputHandler.Button.L) },
            config = config,
            inputEnabled = inputEnabled
        )

        // R 肩键
        ShoulderButton(
            text = "R",
            isLeft = false,
            onPress = { inputHandler.pressButton(InputHandler.Button.R) },
            onRelease = { inputHandler.releaseButton(InputHandler.Button.R) },
            config = config,
            inputEnabled = inputEnabled
        )
    }
}

/**
 * 肩键按钮
 */
@Composable
private fun ShoulderButton(
    text: String,
    isLeft: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    config: GamepadConfig,
    inputEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale = if (isPressed) 0.95f else 1f
    val shape = if (isLeft) {
        RoundedCornerShape(topStart = 24.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 24.dp)
    } else {
        RoundedCornerShape(topStart = 8.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 8.dp)
    }

    val touchModifier = if (inputEnabled) {
        Modifier.pointerInput(text) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    onPress()
                    tryAwaitRelease()
                    isPressed = false
                    onRelease()
                }
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .width(config.shoulderButtonWidth.dp)
            .height(config.shoulderButtonHeight.dp)
            .scale(scale)
            .alpha(config.buttonAlpha)
            .background(
                color = if (isPressed) {
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)
                } else {
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                },
                shape = shape
            )
            .then(touchModifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSecondary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ShoulderButtonControl(
    text: String,
    inputButton: InputHandler.Button,
    isLeft: Boolean,
    inputHandler: InputHandler,
    config: GamepadConfig,
    inputEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    ShoulderButton(
        text = text,
        isLeft = isLeft,
        onPress = { inputHandler.pressButton(inputButton) },
        onRelease = { inputHandler.releaseButton(inputButton) },
        config = config,
        inputEnabled = inputEnabled,
        modifier = modifier
    )
}

/**
 * 菜单按钮（Start/Select）
 */
@Composable
fun MenuButton(
    text: String,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    config: GamepadConfig,
    inputEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale = if (isPressed) 0.95f else 1f

    val touchModifier = if (inputEnabled) {
        Modifier.pointerInput(text) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    onPress()
                    tryAwaitRelease()
                    isPressed = false
                    onRelease()
                }
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .scale(scale)
            .alpha(config.buttonAlpha)
            .background(
                color = if (isPressed) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(16.dp)
            )
            .then(touchModifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
