package com.jboy.emulator.ui.netplay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetplayScreen(
    onBack: () -> Unit,
    viewModel: NetplayViewModel = viewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GBA联机测试") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
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
                        text = "联机功能测试中（待定）",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "服务端默认语言：Rust（见根目录 plan.md）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                }
            }

            OutlinedTextField(
                value = state.serverAddress,
                onValueChange = { viewModel.updateServerAddress(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("服务器地址") },
                singleLine = true
            )

            OutlinedTextField(
                value = state.roomId,
                onValueChange = { viewModel.updateRoomId(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("房间号") },
                singleLine = true
            )

            OutlinedTextField(
                value = state.nickname,
                onValueChange = { viewModel.updateNickname(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("昵称") },
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
                    Text(if (state.isConnecting) "连接中" else "连接")
                }

                TextButton(
                    onClick = { viewModel.disconnect() },
                    enabled = state.isConnecting || state.isConnected,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("断开")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = state.statusText,
                    onValueChange = {},
                    modifier = Modifier.weight(1f),
                    readOnly = true,
                    label = { Text("连接状态") },
                    singleLine = true
                )

                TextButton(
                    onClick = { viewModel.sendHandshake() },
                    enabled = state.isConnected,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("发握手")
                }

                TextButton(
                    onClick = { viewModel.sendPing() },
                    enabled = state.isConnected,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ping")
                }
            }

            if (!state.errorText.isNullOrBlank()) {
                Text(
                    text = "错误：${state.errorText}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = "最后消息：${state.lastMessage.ifBlank { "(无)" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}
