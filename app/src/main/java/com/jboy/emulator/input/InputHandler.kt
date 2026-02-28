package com.jboy.emulator.input

import com.jboy.emulator.core.InputKeys

class InputHandler private constructor() {
    companion object {
        private val instance = InputHandler()
        fun getInstance(): InputHandler = instance
    }

    enum class Button {
        A, B, SELECT, START, UP, DOWN, LEFT, RIGHT, L, R
    }

    private var keyMask: Int = 0
    private val virtualButtonMap: MutableMap<Button, Button> = mutableMapOf()

    init {
        Button.entries.forEach { button ->
            virtualButtonMap[button] = button
        }
    }

    fun pressButton(button: Button) {
        val mapped = virtualButtonMap[button] ?: button
        keyMask = keyMask or toMask(mapped)
        com.jboy.emulator.core.InputHandler.getInstance().onTouchKey(toMask(mapped), true)
    }

    fun releaseButton(button: Button) {
        val mapped = virtualButtonMap[button] ?: button
        keyMask = keyMask and toMask(mapped).inv()
        com.jboy.emulator.core.InputHandler.getInstance().onTouchKey(toMask(mapped), false)
    }

    fun setVirtualButtonMapping(source: Button, target: Button) {
        virtualButtonMap[source] = target
    }

    fun setVirtualButtonMappings(mappings: Map<Button, Button>) {
        Button.entries.forEach { button ->
            virtualButtonMap[button] = mappings[button] ?: button
        }
    }

    private fun toMask(button: Button): Int = when (button) {
        Button.A -> InputKeys.A
        Button.B -> InputKeys.B
        Button.SELECT -> InputKeys.SELECT
        Button.START -> InputKeys.START
        Button.UP -> InputKeys.UP
        Button.DOWN -> InputKeys.DOWN
        Button.LEFT -> InputKeys.LEFT
        Button.RIGHT -> InputKeys.RIGHT
        Button.L -> InputKeys.L
        Button.R -> InputKeys.R
    }
}
