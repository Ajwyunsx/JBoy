package com.jboy.emulator.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GameInfo(
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val displayName: String,
    val gameCode: String? = null,
    val coverPath: String? = null,
    val isFavorite: Boolean = false,
    val lastPlayed: Long? = null,
    val playTime: Long = 0,
    val timesPlayed: Int = 0,
    val addedDate: Long = System.currentTimeMillis()
)

data class SaveStateInfo(
    val id: Long = 0,
    val gameId: Long,
    val slot: Int,
    val savePath: String,
    val screenshotPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val playTime: Long = 0
)

class GameRepository {
    private val _games = MutableStateFlow<List<GameInfo>>(emptyList())
    val games: Flow<List<GameInfo>> = _games.asStateFlow()
    
    private val _recentGames = MutableStateFlow<List<GameInfo>>(emptyList())
    val recentGames: Flow<List<GameInfo>> = _recentGames.asStateFlow()
    
    private var nextId = 1L
    
    fun addGame(romInfo: RomInfo): Long {
        val game = GameInfo(
            id = nextId++,
            fileName = romInfo.fileName,
            filePath = romInfo.filePath,
            displayName = romInfo.displayName,
            gameCode = romInfo.gameCode,
            coverPath = romInfo.coverPath
        )
        _games.value = _games.value + game
        return game.id
    }
    
    fun getAllGames(): List<GameInfo> = _games.value
    
    fun getGameById(id: Long): GameInfo? = _games.value.find { it.id == id }
    
    fun getGameByPath(path: String): GameInfo? = _games.value.find { it.filePath == path }
    
    fun updateGame(game: GameInfo) {
        _games.value = _games.value.map { if (it.id == game.id) game else it }
    }
    
    fun deleteGame(gameId: Long) {
        _games.value = _games.value.filter { it.id != gameId }
    }
    
    fun isGameExists(path: String): Boolean = _games.value.any { it.filePath == path }
    
    fun toggleFavorite(gameId: Long) {
        _games.value = _games.value.map { 
            if (it.id == gameId) it.copy(isFavorite = !it.isFavorite) else it 
        }
    }
    
    fun recordGamePlayed(gameId: Long) {
        _games.value = _games.value.map { 
            if (it.id == gameId) it.copy(
                lastPlayed = System.currentTimeMillis(),
                timesPlayed = it.timesPlayed + 1
            ) else it 
        }
        updateRecentGames()
    }
    
    private fun updateRecentGames() {
        _recentGames.value = _games.value
            .filter { it.lastPlayed != null }
            .sortedByDescending { it.lastPlayed }
            .take(10)
    }
    
    fun searchGames(query: String): List<GameInfo> {
        val lowerQuery = query.lowercase()
        return _games.value.filter { 
            it.displayName.lowercase().contains(lowerQuery) ||
            (it.gameCode?.lowercase()?.contains(lowerQuery) == true)
        }
    }
}
