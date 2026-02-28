package com.jboy.emulator.ui.gamepad

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.jboy.emulator.input.InputHandler
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

enum class DpadMode(val displayName: String) {
    WHEEL("轮盘十字键"),
    JOYSTICK("摇杆")
}

@Composable
fun DPad(
    inputHandler: InputHandler,
    config: GamepadConfig,
    mode: DpadMode,
    inputEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    when (mode) {
        DpadMode.WHEEL -> WheelDPad(
            inputHandler = inputHandler,
            config = config,
            inputEnabled = inputEnabled,
            modifier = modifier
        )

        DpadMode.JOYSTICK -> StickDPad(
            inputHandler = inputHandler,
            config = config,
            inputEnabled = inputEnabled,
            modifier = modifier
        )
    }
}

@Composable
private fun WheelDPad(
    inputHandler: InputHandler,
    config: GamepadConfig,
    inputEnabled: Boolean,
    modifier: Modifier
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    val activeDirections = remember { mutableSetOf<Direction>() }

    val wheelSize = config.dpadSize * 1.02f
    val maxRadius = wheelSize * 0.40f
    val deadZone = wheelSize * 0.12f

    val baseModifier = Modifier
            .size(wheelSize.dp)
            .alpha(config.buttonAlpha)
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                shape = CircleShape
            )
    val interactionModifier = if (inputEnabled) {
        baseModifier
            .pointerInput(maxRadius, deadZone) {
                detectDragGestures(
                    onDragStart = { start ->
                        val centered = Offset(
                            x = start.x - size.width / 2f,
                            y = start.y - size.height / 2f
                        )
                        knobOffset = clampToRadius(centered, maxRadius)
                        updateDirectionStatesFromWheel(
                            offset = knobOffset,
                            deadZone = deadZone,
                            inputHandler = inputHandler,
                            activeDirections = activeDirections
                        )
                    },
                    onDragEnd = {
                        releaseAllDirections(inputHandler, activeDirections)
                        knobOffset = Offset.Zero
                    },
                    onDragCancel = {
                        releaseAllDirections(inputHandler, activeDirections)
                        knobOffset = Offset.Zero
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val centered = Offset(
                            x = change.position.x - size.width / 2f,
                            y = change.position.y - size.height / 2f
                        )
                        knobOffset = clampToRadius(centered, maxRadius)
                        updateDirectionStatesFromWheel(
                            offset = knobOffset,
                            deadZone = deadZone,
                            inputHandler = inputHandler,
                            activeDirections = activeDirections
                        )
                    }
                )
            }
            .pointerInput(maxRadius, deadZone) {
                detectTapGestures(
                    onPress = { pressed ->
                        val centered = Offset(
                            x = pressed.x - size.width / 2f,
                            y = pressed.y - size.height / 2f
                        )
                        knobOffset = clampToRadius(centered, maxRadius)
                        updateDirectionStatesFromWheel(
                            offset = knobOffset,
                            deadZone = deadZone,
                            inputHandler = inputHandler,
                            activeDirections = activeDirections
                        )
                        tryAwaitRelease()
                        releaseAllDirections(inputHandler, activeDirections)
                        knobOffset = Offset.Zero
                    }
                )
            }
    } else {
        baseModifier
    }

    Box(
        modifier = modifier
            .then(interactionModifier),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size((wheelSize * 0.70f).dp)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.24f),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size((wheelSize * 0.30f).dp)
                .offset(
                    x = (knobOffset.x / 1.9f).dp,
                    y = (knobOffset.y / 1.9f).dp
                )
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun StickDPad(
    inputHandler: InputHandler,
    config: GamepadConfig,
    inputEnabled: Boolean,
    modifier: Modifier
) {
    var stickOffset by remember { mutableStateOf(Offset.Zero) }
    val activeDirections = remember { mutableSetOf<Direction>() }

    val joystickSize = config.dpadSize
    val maxRadius = joystickSize * 0.40f
    val deadZone = maxRadius * 0.16f
    val pressThreshold = maxRadius * 0.40f
    val releaseThreshold = maxRadius * 0.26f

    val baseModifier = Modifier
            .size(joystickSize.dp)
            .alpha(config.buttonAlpha)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = CircleShape
            )
    val interactionModifier = if (inputEnabled) {
        baseModifier
            .pointerInput(maxRadius, deadZone, pressThreshold, releaseThreshold) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val centered = Offset(
                            x = offset.x - size.width / 2f,
                            y = offset.y - size.height / 2f
                        )
                        stickOffset = clampToRadius(centered, maxRadius)
                        updateDirectionStatesByThreshold(
                            offset = stickOffset,
                            deadZone = deadZone,
                            pressThreshold = pressThreshold,
                            releaseThreshold = releaseThreshold,
                            inputHandler = inputHandler,
                            activeDirections = activeDirections
                        )
                    },
                    onDragEnd = {
                        releaseAllDirections(inputHandler, activeDirections)
                        stickOffset = Offset.Zero
                    },
                    onDragCancel = {
                        releaseAllDirections(inputHandler, activeDirections)
                        stickOffset = Offset.Zero
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val centered = Offset(
                            x = change.position.x - size.width / 2f,
                            y = change.position.y - size.height / 2f
                        )
                        stickOffset = clampToRadius(centered, maxRadius)
                        updateDirectionStatesByThreshold(
                            offset = stickOffset,
                            deadZone = deadZone,
                            pressThreshold = pressThreshold,
                            releaseThreshold = releaseThreshold,
                            inputHandler = inputHandler,
                            activeDirections = activeDirections
                        )
                    }
                )
            }
            .pointerInput(maxRadius, deadZone, pressThreshold, releaseThreshold) {
                detectTapGestures(
                    onPress = { pressed ->
                        val centered = Offset(
                            x = pressed.x - size.width / 2f,
                            y = pressed.y - size.height / 2f
                        )
                        stickOffset = clampToRadius(centered, maxRadius)
                        updateDirectionStatesByThreshold(
                            offset = stickOffset,
                            deadZone = deadZone,
                            pressThreshold = pressThreshold,
                            releaseThreshold = releaseThreshold,
                            inputHandler = inputHandler,
                            activeDirections = activeDirections
                        )
                        tryAwaitRelease()
                        releaseAllDirections(inputHandler, activeDirections)
                        stickOffset = Offset.Zero
                    }
                )
            }
    } else {
        baseModifier
    }

    Box(
        modifier = modifier
            .then(interactionModifier),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size((joystickSize * 0.38f).dp)
                .offset(
                    x = (stickOffset.x / 1.75f).dp,
                    y = (stickOffset.y / 1.75f).dp
                )
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                    shape = CircleShape
                )
        )
    }
}

private fun updateDirectionStatesByThreshold(
    offset: Offset,
    deadZone: Float,
    pressThreshold: Float,
    releaseThreshold: Float,
    inputHandler: InputHandler,
    activeDirections: MutableSet<Direction>
) {
    val next = stickDirections(
        offset = offset,
        deadZone = deadZone,
        pressThreshold = pressThreshold,
        releaseThreshold = releaseThreshold,
        activeDirections = activeDirections
    )
    syncDirections(inputHandler, activeDirections, next)
}

private fun updateDirectionStatesFromWheel(
    offset: Offset,
    deadZone: Float,
    inputHandler: InputHandler,
    activeDirections: MutableSet<Direction>
) {
    val next = wheelDirections(offset, deadZone, activeDirections)
    syncDirections(inputHandler, activeDirections, next)
}

private fun stickDirections(
    offset: Offset,
    deadZone: Float,
    pressThreshold: Float,
    releaseThreshold: Float,
    activeDirections: Set<Direction>
): Set<Direction> {
    val distance = sqrt(offset.x * offset.x + offset.y * offset.y)
    val releaseDeadZone = deadZone * 0.75f
    if (activeDirections.isEmpty()) {
        if (distance < deadZone) {
            return emptySet()
        }
    } else if (distance < releaseDeadZone) {
        return emptySet()
    }

    val next = mutableSetOf<Direction>()

    if (shouldDirectionActive(offset.x, Direction.RIGHT in activeDirections, pressThreshold, releaseThreshold)) {
        next += Direction.RIGHT
    }
    if (shouldDirectionActive(-offset.x, Direction.LEFT in activeDirections, pressThreshold, releaseThreshold)) {
        next += Direction.LEFT
    }
    if (shouldDirectionActive(offset.y, Direction.DOWN in activeDirections, pressThreshold, releaseThreshold)) {
        next += Direction.DOWN
    }
    if (shouldDirectionActive(-offset.y, Direction.UP in activeDirections, pressThreshold, releaseThreshold)) {
        next += Direction.UP
    }

    if (next.size == 2) {
        val absX = abs(offset.x)
        val absY = abs(offset.y)
        if (absX > absY * 1.85f && (Direction.UP in next || Direction.DOWN in next)) {
            next.remove(Direction.UP)
            next.remove(Direction.DOWN)
        } else if (absY > absX * 1.85f && (Direction.LEFT in next || Direction.RIGHT in next)) {
            next.remove(Direction.LEFT)
            next.remove(Direction.RIGHT)
        }
    }

    return next
}

private fun shouldDirectionActive(
    axisValue: Float,
    currentlyActive: Boolean,
    pressThreshold: Float,
    releaseThreshold: Float
): Boolean {
    return if (currentlyActive) {
        axisValue > releaseThreshold
    } else {
        axisValue > pressThreshold
    }
}

private fun wheelDirections(offset: Offset, deadZone: Float, activeDirections: Set<Direction>): Set<Direction> {
    val distance = sqrt(offset.x * offset.x + offset.y * offset.y)
    val releaseDeadZone = deadZone * 0.72f
    if (activeDirections.isEmpty()) {
        if (distance < deadZone) {
            return emptySet()
        }
    } else if (distance < releaseDeadZone) {
        return emptySet()
    }

    val angle = normalizeAngle(Math.toDegrees(atan2(offset.y.toDouble(), offset.x.toDouble())))
    val candidateSector = angleToWheelSector(angle)
    val previousSector = directionsToWheelSector(activeDirections)
    if (previousSector == null) {
        return sectorToDirections(candidateSector)
    }

    val previousCenter = previousSector * 45.0
    val switchThreshold = 22.0
    val stickyRadius = deadZone * 1.65f
    if (
        candidateSector != previousSector &&
        distance < stickyRadius &&
        angularDistance(angle, previousCenter) < switchThreshold
    ) {
        return activeDirections.toSet()
    }

    return sectorToDirections(candidateSector)
}

private fun normalizeAngle(angle: Double): Double {
    var normalized = angle % 360.0
    if (normalized < 0.0) {
        normalized += 360.0
    }
    return normalized
}

private fun angleToWheelSector(angle: Double): Int {
    val shifted = (angle + 22.5) % 360.0
    return (shifted / 45.0).toInt().coerceIn(0, 7)
}

private fun angularDistance(a: Double, b: Double): Double {
    val diff = abs((a - b + 540.0) % 360.0 - 180.0)
    return diff
}

private fun sectorToDirections(sector: Int): Set<Direction> {
    return when (sector) {
        0 -> setOf(Direction.RIGHT)
        1 -> setOf(Direction.RIGHT, Direction.DOWN)
        2 -> setOf(Direction.DOWN)
        3 -> setOf(Direction.LEFT, Direction.DOWN)
        4 -> setOf(Direction.LEFT)
        5 -> setOf(Direction.LEFT, Direction.UP)
        6 -> setOf(Direction.UP)
        else -> setOf(Direction.RIGHT, Direction.UP)
    }
}

private fun directionsToWheelSector(directions: Set<Direction>): Int? {
    return when {
        directions.size == 1 && Direction.RIGHT in directions -> 0
        directions.size == 2 && Direction.RIGHT in directions && Direction.DOWN in directions -> 1
        directions.size == 1 && Direction.DOWN in directions -> 2
        directions.size == 2 && Direction.LEFT in directions && Direction.DOWN in directions -> 3
        directions.size == 1 && Direction.LEFT in directions -> 4
        directions.size == 2 && Direction.LEFT in directions && Direction.UP in directions -> 5
        directions.size == 1 && Direction.UP in directions -> 6
        directions.size == 2 && Direction.RIGHT in directions && Direction.UP in directions -> 7
        else -> null
    }
}

private fun syncDirections(
    inputHandler: InputHandler,
    activeDirections: MutableSet<Direction>,
    next: Set<Direction>
) {
    val toRelease = activeDirections - next
    val toPress = next - activeDirections

    toRelease.forEach { releaseDirection(it, inputHandler) }
    toPress.forEach { pressDirection(it, inputHandler) }

    activeDirections.clear()
    activeDirections.addAll(next)
}

private fun releaseAllDirections(inputHandler: InputHandler, activeDirections: MutableSet<Direction>) {
    activeDirections.forEach { releaseDirection(it, inputHandler) }
    activeDirections.clear()
}

private fun pressDirection(direction: Direction, inputHandler: InputHandler) {
    when (direction) {
        Direction.UP -> inputHandler.pressButton(InputHandler.Button.UP)
        Direction.DOWN -> inputHandler.pressButton(InputHandler.Button.DOWN)
        Direction.LEFT -> inputHandler.pressButton(InputHandler.Button.LEFT)
        Direction.RIGHT -> inputHandler.pressButton(InputHandler.Button.RIGHT)
    }
}

private fun releaseDirection(direction: Direction, inputHandler: InputHandler) {
    when (direction) {
        Direction.UP -> inputHandler.releaseButton(InputHandler.Button.UP)
        Direction.DOWN -> inputHandler.releaseButton(InputHandler.Button.DOWN)
        Direction.LEFT -> inputHandler.releaseButton(InputHandler.Button.LEFT)
        Direction.RIGHT -> inputHandler.releaseButton(InputHandler.Button.RIGHT)
    }
}

private fun clampToRadius(offset: Offset, maxRadius: Float): Offset {
    val distance = sqrt(offset.x * offset.x + offset.y * offset.y)
    if (distance <= maxRadius || distance == 0f) return offset
    val factor = maxRadius / distance
    return Offset(offset.x * factor, offset.y * factor)
}

enum class Direction {
    UP, DOWN, LEFT, RIGHT
}
