package com.jboy.emulator.input

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object HardwareKeyCaptureBus {
    private val _keyEvents = MutableSharedFlow<Int>(extraBufferCapacity = 16)
    val keyEvents = _keyEvents.asSharedFlow()

    fun emitKeyDown(keyCode: Int) {
        _keyEvents.tryEmit(keyCode)
    }
}
