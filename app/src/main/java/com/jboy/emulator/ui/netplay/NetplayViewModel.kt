package com.jboy.emulator.ui.netplay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jboy.emulator.netplay.NetplaySessionBus
import com.jboy.emulator.netplay.NetplaySessionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

data class NetplayUiState(
    val serverAddress: String = "ws://127.0.0.1:8080",
    val roomId: String = "",
    val nickname: String = "Player",
    val statusText: String = "未连接",
    val lastMessage: String = "",
    val resolvedWsUrl: String = "",
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val errorText: String? = null,
    val protocolState: String = "未握手",
    val connectedPeers: Int = 0,
    val readyPeers: Int = 0,
    val isSelfReady: Boolean = false,
    val canStartLink: Boolean = false,
    val lobbyRooms: List<LobbyRoomItem> = emptyList(),
    val isLoadingLobby: Boolean = false
)

data class LobbyRoomItem(
    val id: String,
    val name: String,
    val owner: String,
    val players: Int,
    val maxPlayers: Int,
    val createdAt: String
)

class NetplayViewModel : ViewModel() {
    private val wsClient = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null

    private val _uiState = MutableStateFlow(NetplayUiState())
    val uiState: StateFlow<NetplayUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            uiState.collect { state ->
                NetplaySessionBus.publish(
                    NetplaySessionState(
                        protocol = "jboy-link-1",
                        serverAddress = state.serverAddress,
                        resolvedWsUrl = state.resolvedWsUrl,
                        roomId = state.roomId.ifBlank { "default" },
                        nickname = state.nickname.ifBlank { "Player" },
                        connectedPeers = state.connectedPeers,
                        readyPeers = state.readyPeers,
                        isConnected = state.isConnected,
                        isSelfReady = state.isSelfReady,
                        canStartLink = state.canStartLink
                    )
                )
            }
        }

        refreshLobbyRooms()
    }

    fun refreshLobbyRooms() {
        val state = _uiState.value
        val baseUrl = buildHttpBaseUrl(state.serverAddress)
        if (baseUrl == null) {
            _uiState.update {
                it.copy(
                    isLoadingLobby = false,
                    errorText = "大厅地址无效，请检查服务器地址"
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoadingLobby = true,
                errorText = null
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/rooms")
                .get()
                .build()

            runCatching {
                wsClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        error("HTTP ${response.code}")
                    }
                    response.body?.string().orEmpty()
                }
            }.onSuccess { bodyText ->
                val rooms = parseLobbyRooms(bodyText)
                _uiState.update {
                    it.copy(
                        isLoadingLobby = false,
                        lobbyRooms = rooms,
                        statusText = if (it.isConnected) {
                            it.statusText
                        } else {
                            "大厅已同步 (${rooms.size} 个房间)"
                        }
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoadingLobby = false,
                        errorText = "大厅刷新失败: ${throwable.message ?: "未知错误"}"
                    )
                }
            }
        }
    }

    fun createRoomAndConnect(roomName: String) {
        val roomId = normalizeRoomId(roomName)
        if (roomId.isBlank()) {
            _uiState.update {
                it.copy(errorText = "请输入房间名")
            }
            return
        }

        if (_uiState.value.isConnected || _uiState.value.isConnecting) {
            disconnect()
        }

        _uiState.update {
            it.copy(
                roomId = roomId,
                statusText = "正在创建并进入房间...",
                errorText = null
            )
        }
        connect()
    }

    fun joinRoomAndConnect(roomId: String) {
        val target = roomId.trim()
        if (target.isBlank()) {
            _uiState.update { it.copy(errorText = "房间号无效") }
            return
        }

        if (_uiState.value.isConnected || _uiState.value.isConnecting) {
            disconnect()
        }

        _uiState.update {
            it.copy(
                roomId = target,
                statusText = "正在进入房间 $target",
                errorText = null
            )
        }
        connect()
    }

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

        val state = _uiState.value
        val url = buildWsEndpointUrl(
            serverAddress = state.serverAddress,
            roomId = state.roomId,
            nickname = state.nickname
        )
        if (url == null) {
            _uiState.update {
                it.copy(
                    statusText = "连接失败",
                    errorText = "服务器地址无效或缺少房间/昵称",
                    resolvedWsUrl = "",
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
                resolvedWsUrl = url,
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
                refreshLobbyRooms()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleServerMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _uiState.update {
                    it.copy(
                        statusText = "关闭中 ($code)",
                        isConnecting = false,
                        isConnected = false,
                        resolvedWsUrl = ""
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
                        isConnected = false,
                        resolvedWsUrl = "",
                        protocolState = "未握手",
                        connectedPeers = 0,
                        readyPeers = 0,
                        isSelfReady = false,
                        canStartLink = false
                    )
                }
                refreshLobbyRooms()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                this@NetplayViewModel.webSocket = null
                val httpCode = response?.code
                val detail = when {
                    httpCode == 404 -> "(HTTP 404) 检查 ws 路径是否为 /ws/{room}/{player}"
                    httpCode != null -> "(HTTP $httpCode)"
                    else -> ""
                }
                _uiState.update {
                    it.copy(
                        statusText = "连接失败",
                        isConnecting = false,
                        isConnected = false,
                        resolvedWsUrl = "",
                        protocolState = "未握手",
                        connectedPeers = 0,
                        readyPeers = 0,
                        isSelfReady = false,
                        canStartLink = false,
                        errorText = "${t.message ?: "未知错误"} $detail".trim()
                    )
                }
                refreshLobbyRooms()
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
                isConnected = false,
                resolvedWsUrl = "",
                protocolState = "未握手",
                connectedPeers = 0,
                readyPeers = 0,
                isSelfReady = false,
                canStartLink = false
            )
        }
        refreshLobbyRooms()
    }

    fun sendHandshake() {
        val ws = webSocket ?: return
        val state = _uiState.value
        val room = state.roomId.ifBlank { "default" }
        val nickname = state.nickname.ifBlank { "Player" }
        val payload =
            "{\"type\":\"hello\",\"protocol\":\"jboy-link-1\",\"room\":\"${jsonEscape(room)}\",\"nickname\":\"${jsonEscape(nickname)}\",\"client\":\"jboy-android\",\"link\":\"gba-link-cable\"}"
        ws.send(payload)
        _uiState.update {
            it.copy(
                statusText = "握手已发送，等待服务端确认",
                lastMessage = payload,
                errorText = null
            )
        }
    }

    fun toggleReady() {
        val ws = webSocket ?: return
        val nextReady = !_uiState.value.isSelfReady
        val payload = "{\"type\":\"ready\",\"ready\":$nextReady}"
        ws.send(payload)
        _uiState.update {
            it.copy(
                statusText = if (nextReady) "已发送准备状态" else "已取消准备状态",
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
            raw.startsWith("tailscale://", ignoreCase = true) -> "ws://${raw.removePrefix("tailscale://")}"
            else -> "ws://$raw"
        }

        val uri = runCatching { URI(withScheme) }.getOrNull() ?: return null
        val host = uri.host?.trim().orEmpty()
        return if (host.isEmpty()) {
            null
        } else {
            val port = if (uri.port in 1..65535) uri.port else DEFAULT_NETPLAY_PORT
            val path = uri.rawPath?.takeIf { it.isNotBlank() } ?: ""
            val query = uri.rawQuery?.let { "?$it" } ?: ""
            val safeHost = if (host.contains(':') && !host.startsWith("[")) {
                "[$host]"
            } else {
                host
            }
            "${uri.scheme?.lowercase() ?: "ws"}://$safeHost:$port$path$query"
        }
    }

    private fun buildWsEndpointUrl(serverAddress: String, roomId: String, nickname: String): String? {
        val base = normalizeWsUrl(serverAddress)?.trimEnd('/') ?: return null
        val room = roomId.trim().ifBlank { "default" }
        val player = nickname.trim().ifBlank { "Player" }

        val encodedRoom = encodePathSegment(room)
        val encodedPlayer = encodePathSegment(player)

        val lower = base.lowercase()
        return when {
            WS_FULL_PATH_REGEX.matches(base) -> base
            lower.endsWith("/ws") -> "$base/$encodedRoom/$encodedPlayer"
            else -> "$base/ws/$encodedRoom/$encodedPlayer"
        }
    }

    private fun encodePathSegment(input: String): String {
        return URLEncoder
            .encode(input, StandardCharsets.UTF_8)
            .replace("+", "%20")
    }

    private fun buildHttpBaseUrl(serverAddress: String): String? {
        val wsBase = normalizeWsUrl(serverAddress) ?: return null
        val uri = runCatching { URI(wsBase) }.getOrNull() ?: return null
        val host = uri.host?.trim().orEmpty()
        if (host.isBlank()) {
            return null
        }

        val scheme = if (uri.scheme.equals("wss", ignoreCase = true)) "https" else "http"
        val port = if (uri.port in 1..65535) uri.port else DEFAULT_NETPLAY_PORT
        val safeHost = if (host.contains(':') && !host.startsWith("[")) {
            "[$host]"
        } else {
            host
        }

        val rawPath = uri.rawPath ?: ""
        val lowerPath = rawPath.lowercase()
        val trimmedPath = when {
            lowerPath.contains("/ws/") -> rawPath.substring(0, lowerPath.indexOf("/ws/"))
            lowerPath.endsWith("/ws") -> rawPath.substring(0, lowerPath.length - 3)
            else -> rawPath
        }.trimEnd('/')

        return "$scheme://$safeHost:$port$trimmedPath"
    }

    private fun parseLobbyRooms(bodyText: String): List<LobbyRoomItem> {
        val jsonArray = runCatching { JSONArray(bodyText) }.getOrNull() ?: return emptyList()
        val rooms = ArrayList<LobbyRoomItem>(jsonArray.length())
        for (index in 0 until jsonArray.length()) {
            val node = jsonArray.optJSONObject(index) ?: continue
            val id = node.optString("id").trim()
            if (id.isEmpty()) {
                continue
            }
            val players = node.optJSONArray("players")?.length() ?: 0
            rooms.add(
                LobbyRoomItem(
                    id = id,
                    name = node.optString("name", id).ifBlank { id },
                    owner = node.optString("owner", "Unknown").ifBlank { "Unknown" },
                    players = players,
                    maxPlayers = node.optInt("max_players", 2).coerceAtLeast(1),
                    createdAt = node.optString("created_at", "")
                )
            )
        }
        return rooms.sortedWith(
            compareByDescending<LobbyRoomItem> { it.players }
                .thenBy { it.name.lowercase() }
        )
    }

    private fun normalizeRoomId(input: String): String {
        val collapsedWhitespace = input
            .trim()
            .replace(Regex("\\s+"), "-")
            .trim('-')
        return collapsedWhitespace.take(MAX_ROOM_ID_LENGTH)
    }

    companion object {
        private val WS_FULL_PATH_REGEX = Regex(".*/ws/[^/]+/[^/]+$", RegexOption.IGNORE_CASE)
        private const val DEFAULT_NETPLAY_PORT = 8080
        private const val MAX_ROOM_ID_LENGTH = 40
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

    private fun handleServerMessage(text: String) {
        val envelope = runCatching { JSONObject(text) }.getOrNull()
        if (envelope == null) {
            _uiState.update {
                it.copy(
                    statusText = "收到服务端消息",
                    lastMessage = text,
                    errorText = null
                )
            }
            return
        }

        val event = envelope.optString("event")
        val payload = envelope.optString("payload")

        when (event) {
            "join" -> {
                _uiState.update {
                    it.copy(
                        statusText = "有玩家加入房间",
                        lastMessage = text,
                        errorText = null
                    )
                }
                refreshLobbyRooms()
            }

            "leave" -> {
                _uiState.update {
                    it.copy(
                        statusText = "有玩家离开房间",
                        lastMessage = text,
                        errorText = null
                    )
                }
                refreshLobbyRooms()
            }

            "protocol", "message" -> handleProtocolPayload(text, payload)

            else -> {
                _uiState.update {
                    it.copy(
                        statusText = "收到服务端消息",
                        lastMessage = text,
                        errorText = null
                    )
                }
            }
        }
    }

    private fun handleProtocolPayload(rawText: String, payload: String) {
        val payloadObj = runCatching { JSONObject(payload) }.getOrNull()
        if (payloadObj == null) {
            _uiState.update {
                it.copy(
                    statusText = "收到服务端消息",
                    lastMessage = rawText,
                    errorText = null
                )
            }
            return
        }

        when (payloadObj.optString("type")) {
            "hello_ack", "link_sync" -> {
                val players = payloadObj.optJSONArray("players") ?: JSONArray()
                val readyPlayers = payloadObj.optJSONArray("readyPlayers") ?: JSONArray()
                val state = _uiState.value
                val selfName = state.nickname.ifBlank { "Player" }
                val peers = players.toStringList().count { it != selfName }
                val readyPeerCount = readyPlayers.toStringList().count { it != selfName }
                val selfReady = readyPlayers.toStringList().any { it == selfName }
                val canStart = payloadObj.optBoolean("canStart", false)

                _uiState.update {
                    it.copy(
                        statusText = if (canStart) "握手完成，可开始联机" else "握手完成，等待双方准备",
                        protocolState = "协议已同步 (jboy-link-1)",
                        connectedPeers = peers,
                        readyPeers = readyPeerCount,
                        isSelfReady = selfReady,
                        canStartLink = canStart,
                        lastMessage = rawText,
                        errorText = null
                    )
                }
            }

            "pong" -> {
                _uiState.update {
                    it.copy(
                        statusText = "已收到 Pong",
                        lastMessage = rawText,
                        errorText = null
                    )
                }
            }

            else -> {
                _uiState.update {
                    it.copy(
                        statusText = "收到协议消息",
                        lastMessage = rawText,
                        errorText = null
                    )
                }
            }
        }
    }

    private fun JSONArray.toStringList(): List<String> {
        val out = ArrayList<String>(length())
        for (index in 0 until length()) {
            val value = optString(index).trim()
            if (value.isNotEmpty()) {
                out.add(value)
            }
        }
        return out
    }

    override fun onCleared() {
        super.onCleared()
        webSocket?.cancel()
        webSocket = null
        wsClient.connectionPool.evictAll()
        wsClient.dispatcher.executorService.shutdown()
    }
}
