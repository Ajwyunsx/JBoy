package com.jboy.emulator.ui.netplay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jboy.emulator.data.settingsDataStore
import com.jboy.emulator.ui.i18n.l10n
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val PREF_NETPLAY_TUTORIAL_SEEN = booleanPreferencesKey("netplay_tutorial_seen")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetplayScreen(
    onBack: () -> Unit,
    boundGamePath: String? = null,
    viewModel: NetplayViewModel = viewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val tutorialSeen by remember {
        context.settingsDataStore.data.map { prefs ->
            prefs[PREF_NETPLAY_TUTORIAL_SEEN] ?: false
        }
    }.collectAsState(initial = false)

    var showTutorial by rememberSaveable { mutableStateOf(false) }
    var createRoomName by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(tutorialSeen) {
        if (!tutorialSeen) {
            showTutorial = true
        }
    }

    LaunchedEffect(boundGamePath) {
        viewModel.bindGamePath(boundGamePath)
    }

    if (showTutorial) {
        AlertDialog(
            onDismissRequest = { showTutorial = false },
            title = { Text(l10n("GBA 联机教程")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(l10n("1. 两台设备在同一局域网，或同一 Tailscale 虚拟局域网。"))
                    Text(l10n("2. 先由房主创建房间，再让队友在大厅加入同一房间。"))
                    Text(l10n("3. 双方连接后都点击\"我已准备\"，状态变成\"可开始联机\"。"))
                    Text(l10n("4. 返回游戏后保持同 ROM 与同进度，再进入 Link 流程。"))
                    Text(l10n("5. 连不上时，优先检查服务器地址、端口 8080、防火墙放行。"))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTutorial = false
                        scope.launch {
                            context.settingsDataStore.edit { prefs ->
                                prefs[PREF_NETPLAY_TUTORIAL_SEEN] = true
                            }
                        }
                    }
                ) {
                    Text(l10n("我知道了"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTutorial = false }) {
                    Text(l10n("稍后再看"))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(l10n("GBA联机大厅")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = l10n("返回"))
                    }
                },
                actions = {
                    IconButton(onClick = { showTutorial = true }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = l10n("联机教程"))
                    }
                    IconButton(onClick = { viewModel.refreshLobbyRooms() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = l10n("刷新大厅"))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = l10n("联机大厅状态"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = l10n("协议：jboy-link-1 · 支持局域网与 Tailscale 虚拟局域网"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    if (state.boundGameTitle.isNotBlank()) {
                        Text(
                            text = l10n("已绑定 GBA：${state.boundGameTitle}"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        TextButton(
                            onClick = { viewModel.useBoundGameAsRoomId() },
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Text(l10n("使用绑定游戏名作为房间号"))
                        }
                    }
                    Text(
                        text = l10n("连接状态：${state.statusText}"),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = l10n("握手状态：${state.protocolState}"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = l10n("对端玩家：${state.connectedPeers} · 对端已准备：${state.readyPeers}"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                    )
                    Text(
                        text = l10n(if (state.canStartLink) {
                            "联机握手已就绪：可开始 GBA Link"
                        } else {
                            "等待双方准备完成"
                        }),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.canStartLink) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                        }
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = l10n("创建房间"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = l10n("房主创建后会自动进入房间，队友可在大厅列表直接加入"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                    OutlinedTextField(
                        value = createRoomName,
                        onValueChange = { createRoomName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(l10n("房间名")) },
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (createRoomName.isBlank()) {
                                    createRoomName = "room-${(1000..9999).random()}"
                                }
                                viewModel.createRoomAndConnect(createRoomName)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(l10n("创建并进入"))
                        }
                        TextButton(
                            onClick = {
                                createRoomName = "room-${(1000..9999).random()}"
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(l10n("随机房间名"))
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = l10n("联机大厅房间"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(onClick = { viewModel.refreshLobbyRooms() }) {
                            Text(l10n(if (state.isLoadingLobby) "刷新中..." else "刷新"))
                        }
                    }

                    if (state.lobbyRooms.isEmpty() && !state.isLoadingLobby) {
                        Text(
                            text = l10n("大厅暂时没有房间，先创建一个吧！"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                        )
                    }

                    state.lobbyRooms.forEach { room ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = room.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${room.players}/${room.maxPlayers}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = l10n("房间号: ${room.id}"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                                )
                                Text(
                                    text = l10n("房主: ${room.owner}"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TextButton(
                                        onClick = { viewModel.updateRoomId(room.id) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(l10n("填入房间号"))
                                    }
                                    Button(
                                        onClick = { viewModel.joinRoomAndConnect(room.id) },
                                        modifier = Modifier.weight(1f),
                                        enabled = !state.isConnecting
                                    ) {
                                        Text(l10n("加入房间"))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = l10n("连接设置"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = state.serverAddress,
                        onValueChange = { viewModel.updateServerAddress(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(l10n("服务器地址 (默认端口 8080)")) },
                        singleLine = true
                    )

                    if (state.resolvedWsUrl.isNotBlank()) {
                        Text(
                            text = l10n("解析地址：${state.resolvedWsUrl}"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }

                    OutlinedTextField(
                        value = state.roomId,
                        onValueChange = { viewModel.updateRoomId(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(l10n("房间号")) },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = state.nickname,
                        onValueChange = { viewModel.updateNickname(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(l10n("昵称")) },
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.connect() },
                            enabled = !state.isConnecting && !state.isConnected,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(l10n(if (state.isConnecting) "连接中" else "连接"))
                        }

                        TextButton(
                            onClick = { viewModel.disconnect() },
                            enabled = state.isConnecting || state.isConnected,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(l10n("断开"))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(
                            onClick = { viewModel.sendHandshake() },
                            enabled = state.isConnected,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(l10n("发握手"))
                        }

                        TextButton(
                            onClick = { viewModel.sendPing() },
                            enabled = state.isConnected,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ping")
                        }

                        Button(
                            onClick = { viewModel.toggleReady() },
                            enabled = state.isConnected,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(l10n(if (state.isSelfReady) "取消准备" else "我已准备"))
                        }
                    }
                }
            }

            if (!state.errorText.isNullOrBlank()) {
                Text(
                    text = l10n("错误：${state.errorText}"),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = l10n("最后消息：${state.lastMessage.ifBlank { "(无)" }}"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}
