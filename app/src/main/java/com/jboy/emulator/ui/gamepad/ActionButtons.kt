package com.jboy.emulator.ui.gamepad

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jboy.emulator.input.InputHandler

@Composable
fun ActionButtons(
    inputHandler: InputHandler,
    config: GamepadConfig,
    inputEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ActionButtonControl(
            text = "B",
            inputButton = InputHandler.Button.B,
            inputHandler = inputHandler,
            config = config,
            inputEnabled = inputEnabled
        )

        ActionButtonControl(
            text = "A",
            inputButton = InputHandler.Button.A,
            inputHandler = inputHandler,
            config = config,
            inputEnabled = inputEnabled
        )
    }
}

@Composable
fun ActionButtonControl(
    text: String,
    inputButton: InputHandler.Button,
    inputHandler: InputHandler,
    config: GamepadConfig,
    inputEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale = if (isPressed) 0.92f else 1f
    val buttonColor = when (inputButton) {
        InputHandler.Button.A -> MaterialTheme.colorScheme.primary
        InputHandler.Button.B -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    val touchModifier = if (inputEnabled) {
        Modifier.pointerInput(inputButton) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    inputHandler.pressButton(inputButton)
                    tryAwaitRelease()
                    isPressed = false
                    inputHandler.releaseButton(inputButton)
                }
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(config.actionButtonSize.dp)
            .scale(scale)
            .alpha(config.buttonAlpha)
            .background(
                color = if (isPressed) {
                    buttonColor.copy(alpha = 0.9f)
                } else {
                    buttonColor.copy(alpha = 0.6f)
                },
                shape = CircleShape
            )
            .then(touchModifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
