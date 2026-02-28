package com.jboy.emulator.ui.gamelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jboy.emulator.JBoyApplication
import com.jboy.emulator.data.RomInfo
import com.jboy.emulator.data.RomRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameListViewModel : ViewModel() {

    private val romRepository by lazy {
        RomRepository(JBoyApplication.instance.applicationContext)
    }
    
    private val _games = MutableStateFlow<List<GameItem>>(emptyList())
    val games: StateFlow<List<GameItem>> = _games.asStateFlow()
    
    private val _filteredGames = MutableStateFlow<List<GameItem>>(emptyList())
    val filteredGames: StateFlow<List<GameItem>> = _filteredGames.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _recentGames = MutableStateFlow<List<GameItem>>(emptyList())
    val recentGames: StateFlow<List<GameItem>> = _recentGames.asStateFlow()

    init {
        loadGames()
    }

    fun loadGames() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // TODO: 从数据库或文件系统加载游戏列表
                val gameList = loadGamesFromStorage()
                _games.value = gameList
                _filteredGames.value = gameList
                updateRecentGames(gameList)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchGames(query: String) {
        _searchQuery.value = query
        _filteredGames.value = if (query.isBlank()) {
            _games.value
        } else {
            _games.value.filter { game ->
                game.name.contains(query, ignoreCase = true)
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _filteredGames.value = _games.value
    }

    private fun updateRecentGames(games: List<GameItem>) {
        _recentGames.value = games
            .filter { it.lastPlayed > 0 }
            .sortedByDescending { it.lastPlayed }
            .take(5)
    }

    private suspend fun loadGamesFromStorage(): List<GameItem> = withContext(Dispatchers.IO) {
        romRepository.getAllGames().map { rom ->
            GameItem(
                id = rom.filePath.hashCode().toLong(),
                name = rom.displayName,
                path = rom.filePath,
                coverPath = rom.coverPath,
                lastPlayed = rom.lastPlayed ?: 0L,
                totalPlayTime = rom.playTime,
                isFavorite = rom.isFavorite
            )
        }
    }

    fun addGame(game: GameItem): Boolean {
        val currentList = _games.value.toMutableList()
        if (currentList.any { it.path == game.path }) {
            return false
        }
        currentList.add(game)
        _games.value = currentList
        searchGames(_searchQuery.value)
        return true
    }

    fun addRom(romInfo: RomInfo): Boolean {
        val game = GameItem(
            id = System.currentTimeMillis() + romInfo.filePath.hashCode().toLong(),
            name = romInfo.displayName,
            path = romInfo.filePath,
            coverPath = romInfo.coverPath,
            lastPlayed = 0L,
            totalPlayTime = 0L,
            isFavorite = false
        )
        return addGame(game)
    }

    fun removeGame(gameId: Long) {
        _games.value = _games.value.filter { it.id != gameId }
        searchGames(_searchQuery.value)
    }

    fun toggleFavorite(gameId: Long) {
        _games.value = _games.value.map { game ->
            if (game.id == gameId) {
                game.copy(isFavorite = !game.isFavorite)
            } else {
                game
            }
        }
        searchGames(_searchQuery.value)
    }
}
