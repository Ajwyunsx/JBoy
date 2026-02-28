package com.jboy.emulator.netplay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NetplaySessionState(
    val protocol: String = "jboy-link-1",
    val serverAddress: String = "",
    val resolvedWsUrl: String = "",
    val roomId: String = "",
    val nickname: String = "",
    val connectedPeers: Int = 0,
    val readyPeers: Int = 0,
    val isConnected: Boolean = false,
    val isSelfReady: Boolean = false,
    val canStartLink: Boolean = false
)

object NetplaySessionBus {
    private val _state = MutableStateFlow(NetplaySessionState())
    val state: StateFlow<NetplaySessionState> = _state.asStateFlow()

    fun publish(next: NetplaySessionState) {
        _state.value = next
    }

    fun clear() {
        _state.value = NetplaySessionState()
    }
}
