package com.jboy.emulator.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

sealed class RomScanState {
    object Idle : RomScanState()
    data class Scanning(val currentFile: String, val progress: Int, val total: Int) : RomScanState()
    data class Complete(val roms: List<RomInfo>) : RomScanState()
    data class Error(val message: String) : RomScanState()
}

data class RomInfo(
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val displayName: String,
    val gameCode: String? = null,
    val coverPath: String? = null,
    val isFavorite: Boolean = false,
    val lastPlayed: Long? = null,
    val playTime: Long = 0
) {
    val isZip: Boolean get() = fileName.endsWith(".zip", ignoreCase = true)
    val extension: String get() = fileName.substringAfterLast('.', "")
}

class RomRepository(private val context: Context) {
    
    private val romCacheDir: File by lazy {
        File(context.filesDir, "roms").apply { mkdirs() }
    }
    
    private val coverCacheDir: File by lazy {
        File(context.filesDir, "covers").apply { mkdirs() }
    }
    
    private val supportedExtensions = listOf("gba", "zip")
    
    /**
     * 扫描文件夹中的所有ROM文件
     */
    fun scanRomFiles(folderUri: Uri): Flow<RomScanState> = flow {
        emit(RomScanState.Scanning("开始扫描...", 0, 0))
        
        try {
            val documentFile = DocumentFile.fromTreeUri(context, folderUri)
                ?: throw IllegalArgumentException("无法访问文件夹")
            
            val allFiles = mutableListOf<DocumentFile>()
            collectRomFiles(documentFile, allFiles)
            
            val romInfos = mutableListOf<RomInfo>()
            val totalFiles = allFiles.size
            
            allFiles.forEachIndexed { index, file ->
                emit(RomScanState.Scanning(file.name ?: "", index + 1, totalFiles))
                
                val romInfo = processRomFile(file)
                romInfo?.let { romInfos.add(it) }
            }
            
            emit(RomScanState.Complete(romInfos))
        } catch (e: Exception) {
            emit(RomScanState.Error(e.message ?: "扫描失败"))
        }
    }
    
    /**
     * 递归收集ROM文件
     */
    private fun collectRomFiles(folder: DocumentFile, result: MutableList<DocumentFile>) {
        folder.listFiles().forEach { file ->
            when {
                file.isDirectory -> collectRomFiles(file, result)
                file.isFile && isRomFile(file.name) -> result.add(file)
            }
        }
    }
    
    /**
     * 检查是否是支持的ROM文件
     */
    private fun isRomFile(fileName: String?): Boolean {
        if (fileName == null) return false
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in supportedExtensions
    }
    
    /**
     * 处理单个ROM文件
     */
    private suspend fun processRomFile(file: DocumentFile): RomInfo? = withContext(Dispatchers.IO) {
        try {
            val fileName = file.name ?: return@withContext null
            val displayName = fileName.substringBeforeLast('.', fileName)
            
            // 复制到缓存目录以便快速访问
            val cachedFile = File(romCacheDir, fileName)
            if (!cachedFile.exists()) {
                context.contentResolver.openInputStream(file.uri)?.use { input ->
                    cachedFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            
            // 尝试读取游戏代码（从GBA ROM头部）
            val gameCode = extractGameCode(cachedFile)
            val tempInfo = RomInfo(
                fileName = fileName,
                filePath = cachedFile.absolutePath,
                displayName = displayName,
                gameCode = gameCode
            )
            val coverPath = generateCover(tempInfo)
            
            RomInfo(
                fileName = fileName,
                filePath = cachedFile.absolutePath,
                displayName = displayName,
                gameCode = gameCode,
                coverPath = coverPath
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun importRomUris(uris: List<Uri>): Flow<RomScanState> = flow {
        emit(RomScanState.Scanning("正在导入...", 0, uris.size))
        try {
            val romInfos = mutableListOf<RomInfo>()
            uris.forEachIndexed { index, uri ->
                val name = DocumentFile.fromSingleUri(context, uri)?.name
                    ?: uri.lastPathSegment
                    ?: "ROM_${index + 1}.gba"
                emit(RomScanState.Scanning(name, index + 1, uris.size))

                val imported = processSingleUri(uri, name)
                if (imported != null) {
                    romInfos.add(imported)
                }
            }
            emit(RomScanState.Complete(romInfos))
        } catch (e: Exception) {
            emit(RomScanState.Error(e.message ?: "导入失败"))
        }
    }

    private suspend fun processSingleUri(uri: Uri, fallbackName: String): RomInfo? = withContext(Dispatchers.IO) {
        try {
            val rawName = DocumentFile.fromSingleUri(context, uri)?.name ?: fallbackName
            if (!isRomFile(rawName)) {
                return@withContext null
            }

            val safeName = rawName.replace("/", "_")
            val cachedFile = File(romCacheDir, safeName)
            if (!cachedFile.exists()) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    cachedFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext null
            }

            val displayName = safeName.substringBeforeLast('.', safeName)
            val gameCode = extractGameCode(cachedFile)
            val tempInfo = RomInfo(
                fileName = safeName,
                filePath = cachedFile.absolutePath,
                displayName = displayName,
                gameCode = gameCode
            )
            val coverPath = generateCover(tempInfo)

            RomInfo(
                fileName = safeName,
                filePath = cachedFile.absolutePath,
                displayName = displayName,
                gameCode = gameCode,
                coverPath = coverPath
            )
        } catch (_: Exception) {
            null
        }
    }
    
    /**
     * 从GBA ROM头部提取游戏代码
     */
    private fun extractGameCode(file: File): String? {
        return try {
            if (file.extension.equals("zip", ignoreCase = true)) {
                // 从ZIP中提取第一个GBA文件
                ZipFile(file).use { zip ->
                    val gbaEntry = zip.entries().asSequence()
                        .find { it.name.endsWith(".gba", ignoreCase = true) }
                    gbaEntry?.let { entry ->
                        zip.getInputStream(entry).use { stream ->
                            val header = ByteArray(192)
                            stream.read(header)
                            String(header, 160, 4, Charsets.US_ASCII).trim()
                        }
                    }
                }
            } else {
                file.inputStream().use { stream ->
                    val header = ByteArray(192)
                    stream.read(header)
                    String(header, 160, 4, Charsets.US_ASCII).trim()
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 生成游戏封面
     */
    private suspend fun generateCover(romInfo: RomInfo): String? = withContext(Dispatchers.IO) {
        try {
            val coverFile = File(coverCacheDir, "${romInfo.fileName}.png")
            
            if (coverFile.exists()) {
                return@withContext coverFile.absolutePath
            }
            
            // 创建简单的封面图（显示游戏名称）
            val width = 240
            val height = 160
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            // 背景渐变
            val paint = Paint().apply {
                color = Color.rgb(40, 40, 80)
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            
            // 边框
            paint.apply {
                color = Color.rgb(100, 100, 150)
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }
            canvas.drawRect(4f, 4f, width - 4f, height - 4f, paint)
            
            // 文字
            paint.apply {
                color = Color.WHITE
                style = Paint.Style.FILL
                textSize = 16f
                textAlign = Paint.Align.CENTER
            }
            
            // 绘制游戏名称（简化版）
            val text = if (romInfo.displayName.length > 20) {
                romInfo.displayName.substring(0, 20) + "..."
            } else {
                romInfo.displayName
            }
            
            val lines = text.chunked(15)
            lines.forEachIndexed { index, line ->
                canvas.drawText(
                    line,
                    width / 2f,
                    height / 2f + (index - lines.size / 2) * 20,
                    paint
                )
            }
            
            // 游戏代码
            romInfo.gameCode?.let { code ->
                paint.textSize = 12f
                canvas.drawText(
                    code,
                    width / 2f,
                    height - 20f,
                    paint
                )
            }
            
            // 保存封面
            FileOutputStream(coverFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
            bitmap.recycle()
            
            coverFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 加载最近游戏列表
     */
    suspend fun loadRecentGames(limit: Int = 10): List<RomInfo> = withContext(Dispatchers.IO) {
        // 从数据库加载，这里简化处理
        emptyList()
    }
    
    /**
     * 保存最近游戏
     */
    suspend fun saveRecentGame(romInfo: RomInfo) = withContext(Dispatchers.IO) {
        // 保存到数据库，这里简化处理
    }
    
    /**
     * 获取所有游戏
     */
    suspend fun getAllGames(): List<RomInfo> = withContext(Dispatchers.IO) {
        romCacheDir.listFiles()
            ?.filter { isRomFile(it.name) }
            ?.map { file ->
                RomInfo(
                    fileName = file.name,
                    filePath = file.absolutePath,
                    displayName = file.nameWithoutExtension
                )
            } ?: emptyList()
    }
    
    /**
     * 删除游戏
     */
    suspend fun deleteGame(romInfo: RomInfo): Boolean = withContext(Dispatchers.IO) {
        try {
            File(romInfo.filePath).delete()
            romInfo.coverPath?.let { File(it).delete() }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取游戏文件（支持ZIP解包）
     */
    suspend fun getGameFile(romInfo: RomInfo): File? = withContext(Dispatchers.IO) {
        if (romInfo.isZip) {
            // 如果是ZIP，解压并返回第一个GBA文件
            extractGbaFromZip(File(romInfo.filePath))
        } else {
            File(romInfo.filePath)
        }
    }
    
    /**
     * 从ZIP文件中解压GBA文件
     */
    private fun extractGbaFromZip(zipFile: File): File? {
        return try {
            val extractedDir = File(romCacheDir, "extracted").apply { mkdirs() }
            
            ZipFile(zipFile).use { zip ->
                val gbaEntry = zip.entries().asSequence()
                    .find { it.name.endsWith(".gba", ignoreCase = true) }
                
                gbaEntry?.let { entry ->
                    val extractedFile = File(extractedDir, entry.name.substringAfterLast('/'))
                    if (!extractedFile.exists()) {
                        zip.getInputStream(entry).use { input ->
                            extractedFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                    extractedFile
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

// 辅助属性
val File.nameWithoutExtension: String
    get() = name.substringBeforeLast('.', name)
