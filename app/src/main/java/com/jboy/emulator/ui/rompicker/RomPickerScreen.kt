package com.jboy.emulator.ui.rompicker

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jboy.emulator.data.RomInfo
import com.jboy.emulator.data.RomScanState
import com.jboy.emulator.ui.i18n.l10n
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RomPickerScreen(
    onBack: () -> Unit,
    onRomSelected: (RomInfo) -> Boolean,
    viewModel: RomPickerViewModel = viewModel()
) {
    val context = LocalContext.current
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val selectedRoms by viewModel.selectedRoms.collectAsStateWithLifecycle()
    
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.scanFolder(context, it)
        }
    }
    
    val multipleFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importRoms(context, uris)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(l10n("导入游戏")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, l10n("返回"))
                    }
                },
                actions = {
                    val completeState = scanState as? RomScanState.Complete
                    if (completeState != null && completeState.roms.isNotEmpty()) {
                        TextButton(onClick = { viewModel.selectAll(completeState.roms) }) {
                            Text(l10n("全选"))
                        }
                        TextButton(onClick = { viewModel.invertSelection(completeState.roms) }) {
                            Text(l10n("反选"))
                        }
                    }
                    if (selectedRoms.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearSelection() }
                        ) {
                            Text(l10n("清除 (${selectedRoms.size})"))
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            when (scanState) {
                is RomScanState.Complete -> {
                    if (selectedRoms.isNotEmpty()) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                // 导入选中的游戏
                                var successCount = 0
                                selectedRoms.forEach {
                                    if (onRomSelected(it)) {
                                        successCount++
                                    }
                                }
                                val duplicateCount = selectedRoms.size - successCount
                                Toast.makeText(
                                    context,
                                    if (duplicateCount > 0) {
                                        context.l10n("导入成功 $successCount 个，跳过重复 $duplicateCount 个")
                                    } else {
                                        context.l10n("成功导入 $successCount 个游戏")
                                    },
                                    Toast.LENGTH_SHORT
                                ).show()
                                onBack()
                            },
                            icon = { Icon(Icons.Default.Check, null) },
                            text = { Text(l10n("导入 ${selectedRoms.size} 个")) }
                        )
                    }
                }
                else -> {}
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = scanState) {
                is RomScanState.Idle -> {
                    IdleView(
                        onScanFolder = { folderPickerLauncher.launch(null) },
                        onSelectFiles = { 
                            multipleFilesLauncher.launch(
                                arrayOf("*/*")
                            )
                        }
                    )
                }
                is RomScanState.Scanning -> {
                    ScanningView(
                        currentFile = state.currentFile,
                        progress = state.progress,
                        total = state.total
                    )
                }
                is RomScanState.Complete -> {
                    RomListView(
                        roms = state.roms,
                        selectedRoms = selectedRoms,
                        onRomClick = { rom ->
                            viewModel.toggleSelection(rom)
                        },
                        onRomLongClick = { rom ->
                            viewModel.toggleSelection(rom)
                        }
                    )
                }
                is RomScanState.Error -> {
                    ErrorView(
                        message = state.message,
                        onRetry = { folderPickerLauncher.launch(null) }
                    )
                }
            }
        }
    }
}

@Composable
private fun IdleView(
    onScanFolder: () -> Unit,
    onSelectFiles: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = l10n("选择ROM文件"),
            style = MaterialTheme.typography.headlineSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = l10n("支持 .gba 和 .zip 格式"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onScanFolder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Folder, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(l10n("浏览文件夹"))
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedButton(
            onClick = onSelectFiles,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.FileOpen, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(l10n("选择文件"))
        }
    }
}

@Composable
private fun ScanningView(
    currentFile: String,
    progress: Int,
    total: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            strokeWidth = 6.dp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = l10n("正在扫描..."),
            style = MaterialTheme.typography.headlineSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = currentFile,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LinearProgressIndicator(
            progress = { if (total > 0) progress.toFloat() / total else 0f },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "$progress / $total",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RomListView(
    roms: List<RomInfo>,
    selectedRoms: Set<RomInfo>,
    onRomClick: (RomInfo) -> Unit,
    onRomLongClick: (RomInfo) -> Unit
) {
    if (roms.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = l10n("未找到ROM文件"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(roms, key = { it.filePath }) { rom ->
            val isSelected = rom in selectedRoms
            RomItem(
                rom = rom,
                isSelected = isSelected,
                onClick = { onRomClick(rom) },
                onLongClick = { onRomLongClick(rom) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RomItem(
    rom: RomInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val alpha by animateFloatAsState(if (isSelected) 1f else 0.6f, label = "")
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 封面
            Box(
                modifier = Modifier
                    .size(80.dp, 53.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                rom.coverPath?.let { coverPath ->
                    File(coverPath).let { file ->
                        if (file.exists()) {
                            // 这里应该使用图片加载库如 Coil
                            // 简化示例：
                            Icon(
                                imageVector = Icons.Default.VideogameAsset,
                                contentDescription = null,
                                modifier = Modifier.align(Alignment.Center),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } ?: Icon(
                    imageVector = Icons.Default.VideogameAsset,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rom.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = rom.gameCode ?: rom.extension.uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = if (rom.isZip) "ZIP" else "GBA",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // 选中标记
            AnimatedVisibility(visible = isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = l10n("出错了"),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = l10n(message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onRetry) {
            Text(l10n("重试"))
        }
    }
}
