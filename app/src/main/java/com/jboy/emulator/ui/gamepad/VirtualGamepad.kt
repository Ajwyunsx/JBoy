package com.jboy.emulator.ui.gamepad

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.jboy.emulator.input.InputHandler
import kotlin.math.roundToInt

data class GamepadLayoutOffsets(
    val dpadX: Float = 0f,
    val dpadY: Float = 0f,
    val lX: Float = 0f,
    val lY: Float = 0f,
    val rX: Float = 0f,
    val rY: Float = 0f,
    val aX: Float = 0f,
    val aY: Float = 0f,
    val bX: Float = -0.18f,
    val bY: Float = -0.16f,
    val startX: Float = 0.16f,
    val startY: Float = 0f,
    val selectX: Float = -0.16f,
    val selectY: Float = 0f
) {
    fun clamped(): GamepadLayoutOffsets {
        return copy(
            dpadX = dpadX.coerceIn(-0.46f, 0.46f),
            dpadY = dpadY.coerceIn(-0.46f, 0.46f),
            lX = lX.coerceIn(-0.46f, 0.46f),
            lY = lY.coerceIn(-0.46f, 0.46f),
            rX = rX.coerceIn(-0.46f, 0.46f),
            rY = rY.coerceIn(-0.46f, 0.46f),
            aX = aX.coerceIn(-0.46f, 0.46f),
            aY = aY.coerceIn(-0.46f, 0.46f),
            bX = bX.coerceIn(-0.46f, 0.46f),
            bY = bY.coerceIn(-0.46f, 0.46f),
            startX = startX.coerceIn(-0.46f, 0.46f),
            startY = startY.coerceIn(-0.46f, 0.46f),
            selectX = selectX.coerceIn(-0.46f, 0.46f),
            selectY = selectY.coerceIn(-0.46f, 0.46f)
        )
    }

    companion object {
        val DEFAULT = GamepadLayoutOffsets()
    }
}

@Composable
fun VirtualGamepad(
    inputHandler: InputHandler,
    config: GamepadConfig = GamepadConfig(),
    dpadMode: DpadMode = DpadMode.WHEEL,
    layoutOffsets: GamepadLayoutOffsets = GamepadLayoutOffsets.DEFAULT,
    layoutEditMode: Boolean = false,
    onLayoutOffsetsChange: (GamepadLayoutOffsets) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val density = LocalDensity.current
        val parentWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val parentHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

        val latestOffsets by rememberUpdatedState(layoutOffsets)
        val latestOffsetChange by rememberUpdatedState(onLayoutOffsetsChange)

        fun updateOffsets(transform: (GamepadLayoutOffsets) -> GamepadLayoutOffsets) {
            latestOffsetChange(transform(latestOffsets).clamped())
        }

        DraggableAnchor(
            alignment = Alignment.TopStart,
            normalizedX = layoutOffsets.lX,
            normalizedY = layoutOffsets.lY,
            editMode = layoutEditMode,
            parentWidthPx = parentWidthPx,
            parentHeightPx = parentHeightPx,
            onOffsetChange = { x, y ->
                updateOffsets { it.copy(lX = x, lY = y) }
            }
        ) {
            ShoulderButtonControl(
                text = "L",
                inputButton = InputHandler.Button.L,
                isLeft = true,
                inputHandler = inputHandler,
                config = config,
                inputEnabled = !layoutEditMode,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        DraggableAnchor(
            alignment = Alignment.TopEnd,
            normalizedX = layoutOffsets.rX,
            normalizedY = layoutOffsets.rY,
            editMode = layoutEditMode,
            parentWidthPx = parentWidthPx,
            parentHeightPx = parentHeightPx,
            onOffsetChange = { x, y ->
                updateOffsets { it.copy(rX = x, rY = y) }
            }
        ) {
            ShoulderButtonControl(
                text = "R",
                inputButton = InputHandler.Button.R,
                isLeft = false,
                inputHandler = inputHandler,
                config = config,
                inputEnabled = !layoutEditMode,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        DraggableAnchor(
            alignment = Alignment.BottomStart,
            normalizedX = layoutOffsets.dpadX,
            normalizedY = layoutOffsets.dpadY,
            editMode = layoutEditMode,
            parentWidthPx = parentWidthPx,
            parentHeightPx = parentHeightPx,
            onOffsetChange = { x, y ->
                updateOffsets { it.copy(dpadX = x, dpadY = y) }
            }
        ) {
            DPad(
                inputHandler = inputHandler,
                config = config,
                mode = dpadMode,
                inputEnabled = !layoutEditMode
            )
        }

        DraggableAnchor(
            alignment = Alignment.BottomEnd,
            normalizedX = layoutOffsets.bX,
            normalizedY = layoutOffsets.bY,
            editMode = layoutEditMode,
            parentWidthPx = parentWidthPx,
            parentHeightPx = parentHeightPx,
            onOffsetChange = { x, y ->
                updateOffsets { it.copy(bX = x, bY = y) }
            }
        ) {
            ActionButtonControl(
                text = "B",
                inputButton = InputHandler.Button.B,
                inputHandler = inputHandler,
                config = config,
                inputEnabled = !layoutEditMode
            )
        }

        DraggableAnchor(
            alignment = Alignment.BottomEnd,
            normalizedX = layoutOffsets.aX,
            normalizedY = layoutOffsets.aY,
            editMode = layoutEditMode,
            parentWidthPx = parentWidthPx,
            parentHeightPx = parentHeightPx,
            onOffsetChange = { x, y ->
                updateOffsets { it.copy(aX = x, aY = y) }
            }
        ) {
            ActionButtonControl(
                text = "A",
                inputButton = InputHandler.Button.A,
                inputHandler = inputHandler,
                config = config,
                inputEnabled = !layoutEditMode
            )
        }

        DraggableAnchor(
            alignment = Alignment.BottomCenter,
            normalizedX = layoutOffsets.selectX,
            normalizedY = layoutOffsets.selectY,
            editMode = layoutEditMode,
            parentWidthPx = parentWidthPx,
            parentHeightPx = parentHeightPx,
            onOffsetChange = { x, y ->
                updateOffsets { it.copy(selectX = x, selectY = y) }
            }
        ) {
            MenuButton(
                text = "SELECT",
                onPress = { inputHandler.pressButton(InputHandler.Button.SELECT) },
                onRelease = { inputHandler.releaseButton(InputHandler.Button.SELECT) },
                config = config,
                inputEnabled = !layoutEditMode,
                modifier = Modifier
                    .padding(bottom = 32.dp)
                    .size(config.menuButtonWidth.dp, config.menuButtonHeight.dp)
            )
        }

        DraggableAnchor(
            alignment = Alignment.BottomCenter,
            normalizedX = layoutOffsets.startX,
            normalizedY = layoutOffsets.startY,
            editMode = layoutEditMode,
            parentWidthPx = parentWidthPx,
            parentHeightPx = parentHeightPx,
            onOffsetChange = { x, y ->
                updateOffsets { it.copy(startX = x, startY = y) }
            }
        ) {
            MenuButton(
                text = "START",
                onPress = { inputHandler.pressButton(InputHandler.Button.START) },
                onRelease = { inputHandler.releaseButton(InputHandler.Button.START) },
                config = config,
                inputEnabled = !layoutEditMode,
                modifier = Modifier
                    .padding(bottom = 32.dp)
                    .size(config.menuButtonWidth.dp, config.menuButtonHeight.dp)
            )
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.DraggableAnchor(
    alignment: Alignment,
    normalizedX: Float,
    normalizedY: Float,
    editMode: Boolean,
    parentWidthPx: Float,
    parentHeightPx: Float,
    onOffsetChange: (Float, Float) -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val xPx = normalizedX * parentWidthPx
    val yPx = normalizedY * parentHeightPx

    var dragging by remember { mutableStateOf(false) }
    var dragOffsetPx by remember { mutableStateOf(Offset(xPx, yPx)) }
    if (!dragging) {
        dragOffsetPx = Offset(xPx, yPx)
    }

    val dragModifier = if (editMode) {
        Modifier.pointerInput(editMode, parentWidthPx, parentHeightPx) {
            detectDragGestures(
                onDragStart = {
                    dragging = true
                },
                onDragEnd = {
                    dragging = false
                },
                onDragCancel = {
                    dragging = false
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    dragOffsetPx = Offset(
                        dragOffsetPx.x + dragAmount.x,
                        dragOffsetPx.y + dragAmount.y
                    )
                    val nextX = (dragOffsetPx.x / parentWidthPx).coerceIn(-0.46f, 0.46f)
                    val nextY = (dragOffsetPx.y / parentHeightPx).coerceIn(-0.46f, 0.46f)
                    onOffsetChange(nextX, nextY)
                }
            )
        }
    } else {
        Modifier
    }

    val displayOffset = if (dragging) dragOffsetPx else Offset(xPx, yPx)

    Box(
        modifier = Modifier
            .align(alignment)
            .offset { IntOffset(displayOffset.x.roundToInt(), displayOffset.y.roundToInt()) }
            .then(dragModifier)
            .then(
                if (editMode) {
                    Modifier
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp)
                } else {
                    Modifier
                }
            )
    ) {
        content()
    }
}
