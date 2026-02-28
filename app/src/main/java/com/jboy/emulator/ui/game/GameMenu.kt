package com.jboy.emulator.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameMenu(
    onDismiss: () -> Unit,
    onSaveState: (Int) -> Unit,
    onLoadState: (Int) -> Unit,
    onSaveSlot: (Int) -> Unit,
    onLoadSlot: (Int) -> Unit,
    onAddSaveSlot: () -> Unit,
    onRemoveSaveSlot: (Int) -> Unit,
    onFastForward: (Int) -> Unit,
    onTargetFps: (Int) -> Unit,
    onResumeGame: () -> Unit,
    onTogglePause: () -> Unit,
    onToggleMute: () -> Unit,
    onResetGame: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNetplay: () -> Unit,
    onExitGame: () -> Unit,
    onAddCheatCode: (String) -> Unit,
    onToggleCheatCode: (Long, Boolean) -> Unit,
    onRemoveCheatCode: (Long) -> Unit,
    onClearCheatCodes: () -> Unit,
    onToggleLayoutEditMode: () -> Unit,
    onSaveLayout: () -> Unit,
    onResetLayout: () -> Unit,
    currentFastForwardSpeed: Int,
    currentTargetFps: Int,
    isPaused: Boolean,
    isMuted: Boolean,
    isLayoutEditMode: Boolean,
    cheatCodes: List<CheatCodeItem>,
    saveSlots: List<Int>
) {
    var cheatInput by remember { mutableStateOf("") }
    var slotInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "游戏菜单",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "当前速度: ${currentFastForwardSpeed}x",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "目标帧率: ${currentTargetFps} FPS",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(30, 45, 60, 90, 120).forEach { fps ->
                        FastForwardButton(
                            speed = fps,
                            isSelected = currentTargetFps == fps,
                            onClick = {
                                onTargetFps(fps)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            suffix = ""
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onTogglePause()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isPaused) "继续" else "暂停")
                    }
                    OutlinedButton(
                        onClick = {
                            onToggleMute()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isMuted) "取消静音" else "静音")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onSaveState(1)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("快速存档")
                    }
                    OutlinedButton(
                        onClick = {
                            onLoadState(1)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("快速读档")
                    }
                }

                OutlinedButton(
                    onClick = {
                        onResetGame()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("重置游戏")
                }

                OutlinedButton(
                    onClick = {
                        onOpenSettings()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("打开设置")
                }

                Text(
                    text = "按键布局",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                if (!isLayoutEditMode) {
                    OutlinedButton(
                        onClick = {
                            onToggleLayoutEditMode()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("进入布局编辑")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onSaveLayout()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("保存布局")
                        }
                        OutlinedButton(
                            onClick = onResetLayout,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("重置")
                        }
                        TextButton(
                            onClick = {
                                onToggleLayoutEditMode()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("退出编辑")
                        }
                    }
                }

                Text(
                    text = "存档槽位",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = slotInput,
                        onValueChange = { value ->
                            slotInput = value.filter { it.isDigit() }
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("输入槽位编号") },
                        placeholder = { Text("例如 12") }
                    )
                    Button(
                        onClick = {
                            val slot = slotInput.toIntOrNull()
                            if (slot != null && slot > 0) {
                                onSaveSlot(slot)
                                onDismiss()
                            }
                        }
                    ) {
                        Text("存")
                    }
                    OutlinedButton(
                        onClick = {
                            val slot = slotInput.toIntOrNull()
                            if (slot != null && slot > 0) {
                                onLoadSlot(slot)
                                onDismiss()
                            }
                        }
                    ) {
                        Text("读")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onAddSaveSlot,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("添加槽位")
                    }
                    Text(
                        text = "总槽位: ${saveSlots.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 12.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    saveSlots.chunked(2).forEach { rowSlots ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowSlots.forEach { slot ->
                                SaveSlotPanel(
                                    slot = slot,
                                    onSave = {
                                        onSaveSlot(slot)
                                        onDismiss()
                                    },
                                    onLoad = {
                                        onLoadSlot(slot)
                                        onDismiss()
                                    },
                                    onRemove = {
                                        onRemoveSaveSlot(slot)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowSlots.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Text(
                    text = "快进速度",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 2, 4, 8, 16).forEach { speed ->
                        FastForwardButton(
                            speed = speed,
                            isSelected = currentFastForwardSpeed == speed,
                            onClick = {
                                onFastForward(speed)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            suffix = "x"
                        )
                    }
                }

                Text(
                    text = "GBA联机",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "联机功能测试中（待定）",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "服务端默认语言规划为 Rust，详细实施计划见根目录 plan.md",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                        OutlinedButton(
                            onClick = {
                                onOpenNetplay()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("打开联机测试页")
                        }
                    }
                }

                Text(
                    text = "金手指",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = cheatInput,
                        onValueChange = { cheatInput = it },
                        modifier = Modifier.weight(1f),
                        singleLine = false,
                        minLines = 1,
                        maxLines = 2,
                        placeholder = { Text("示例: 82000000 03E7") }
                    )
                    Button(
                        onClick = {
                            val code = cheatInput.trim()
                            if (code.isNotEmpty()) {
                                onAddCheatCode(code)
                                cheatInput = ""
                            }
                        }
                    ) {
                        Text("添加")
                    }
                }

                if (cheatCodes.isEmpty()) {
                    Text(
                        text = "暂无金手指代码",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                } else {
                    cheatCodes.forEach { entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Switch(
                                    checked = entry.enabled,
                                    onCheckedChange = { checked ->
                                        onToggleCheatCode(entry.id, checked)
                                    }
                                )
                                Text(
                                    text = entry.code,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 12.sp
                                )
                                TextButton(onClick = { onRemoveCheatCode(entry.id) }) {
                                    Text("删除")
                                }
                            }
                        }
                    }

                    TextButton(onClick = onClearCheatCodes) {
                        Text(
                            text = "清空金手指",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = onExitGame) {
                Text(
                    text = "退出游戏",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onResumeGame()
                onDismiss()
            }) {
                Text("继续游戏")
            }
        }
    )
}

@Composable
private fun SaveSlotPanel(
    slot: Int,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "槽位 $slot",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "存", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onLoad,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "读", fontSize = 12.sp)
                }
            }
            TextButton(
                onClick = onRemove,
                modifier = Modifier.height(28.dp)
            ) {
                Text(text = "移除", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun FastForwardButton(
    speed: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    suffix: String
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    ) {
        Text(
            text = "$speed$suffix",
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
