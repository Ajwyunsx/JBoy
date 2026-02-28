package com.jboy.emulator.ui.netplay

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

data class NetplayUiState(
    val serverAddress: String = "ws://127.0.0.1:8080",
    val roomId: String = "",
    val nickname: String = "Player",
    val statusText: String = "未连接",
    val lastMessage: String = "",
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val errorText: String? = null
)

class NetplayViewModel : ViewModel() {
    private val wsClient = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null

    private val _uiState = MutableStateFlow(NetplayUiState())
    val uiState: StateFlow<NetplayUiState> = _uiState.asStateFlow()

    fun updateServerAddress(value: String) {
        _uiState.update { it.copy(serverAddress = value.trim(), errorText = null) }
    }

    fun updateRoomId(value: String) {
        _uiState.update { it.copy(roomId = value.trim(), errorText = null) }
    }

    fun updateNickname(value: String) {
        _uiState.update { it.copy(nickname = value.trim(), errorText = null) }
    }

    fun connect() {
        if (webSocket != null || _uiState.value.isConnecting) {
            return
        }

        val url = normalizeWsUrl(_uiState.value.serverAddress)
        if (url == null) {
            _uiState.update {
                it.copy(
                    statusText = "连接失败",
                    errorText = "服务器地址无效",
                    isConnecting = false,
                    isConnected = false
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isConnecting = true,
                isConnected = false,
                statusText = "连接中...",
                errorText = null
            )
        }

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = wsClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        isConnected = true,
                        statusText = "已连接，准备握手",
                        errorText = null
                    )
                }
                sendHandshake()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                _uiState.update {
                    it.copy(
                        statusText = "收到服务端消息",
                        lastMessage = text,
                        errorText = null
                    )
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _uiState.update {
                    it.copy(
                        statusText = "关闭中 ($code)",
                        isConnecting = false,
                        isConnected = false
                    )
                }
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                this@NetplayViewModel.webSocket = null
                _uiState.update {
                    it.copy(
                        statusText = "已断开 ($code)",
                        isConnecting = false,
                        isConnected = false
                    )
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                this@NetplayViewModel.webSocket = null
                _uiState.update {
                    it.copy(
                        statusText = "连接失败",
                        isConnecting = false,
                        isConnected = false,
                        errorText = t.message ?: "未知错误"
                    )
                }
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        _uiState.update {
            it.copy(
                statusText = "已断开",
                isConnecting = false,
                isConnected = false
            )
        }
    }

    fun sendHandshake() {
        val ws = webSocket ?: return
        val state = _uiState.value
        val room = state.roomId.ifBlank { "default" }
        val nickname = state.nickname.ifBlank { "Player" }
        val payload =
            "{\"type\":\"hello\",\"room\":\"${jsonEscape(room)}\",\"nickname\":\"${jsonEscape(nickname)}\",\"client\":\"jboy-android\"}"
        ws.send(payload)
        _uiState.update {
            it.copy(
                statusText = "握手已发送",
                lastMessage = payload,
                errorText = null
            )
        }
    }

    fun sendPing() {
        val ws = webSocket ?: return
        val payload = "{\"type\":\"ping\",\"ts\":${System.currentTimeMillis()}}"
        ws.send(payload)
        _uiState.update {
            it.copy(
                statusText = "Ping 已发送",
                lastMessage = payload,
                errorText = null
            )
        }
    }

    private fun normalizeWsUrl(input: String): String? {
        val raw = input.trim()
        if (raw.isEmpty()) {
            return null
        }

        val withScheme = when {
            raw.startsWith("ws://", ignoreCase = true) -> raw
            raw.startsWith("wss://", ignoreCase = true) -> raw
            raw.startsWith("http://", ignoreCase = true) -> "ws://${raw.removePrefix("http://")}"
            raw.startsWith("https://", ignoreCase = true) -> "wss://${raw.removePrefix("https://")}"
            else -> "ws://$raw"
        }

        return if (withScheme.length > 5 && withScheme.contains(':')) {
            withScheme
        } else {
            null
        }
    }

    private fun jsonEscape(value: String): String {
        return buildString {
            value.forEach { ch ->
                when (ch) {
                    '"' -> append("\\\"")
                    '\\' -> append("\\\\")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        webSocket?.cancel()
        webSocket = null
        wsClient.connectionPool.evictAll()
        wsClient.dispatcher.executorService.shutdown()
    }
}
