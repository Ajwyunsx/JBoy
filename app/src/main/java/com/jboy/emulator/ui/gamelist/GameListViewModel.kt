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

    private val _favoritesOnly = MutableStateFlow(false)
    val favoritesOnly: StateFlow<Boolean> = _favoritesOnly.asStateFlow()

    private val _totalGames = MutableStateFlow(0)
    val totalGames: StateFlow<Int> = _totalGames.asStateFlow()

    private val _favoriteGames = MutableStateFlow(0)
    val favoriteGames: StateFlow<Int> = _favoriteGames.asStateFlow()
    
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
                _totalGames.value = gameList.size
                _favoriteGames.value = gameList.count { it.isFavorite }
                applyFilters()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchGames(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    fun clearSearch() {
        _searchQuery.value = ""
        applyFilters()
    }

    fun toggleFavoritesOnly() {
        _favoritesOnly.value = !_favoritesOnly.value
        applyFilters()
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
        applyFilters()
    }

    fun toggleFavorite(gameId: Long) {
        val updated = _games.value.map { game ->
            if (game.id == gameId) {
                game.copy(isFavorite = !game.isFavorite)
            } else {
                game
            }
        }

        _games.value = updated
        _favoriteGames.value = updated.count { it.isFavorite }
        applyFilters()

        viewModelScope.launch(Dispatchers.IO) {
            val game = updated.firstOrNull { it.id == gameId } ?: return@launch
            romRepository.setFavorite(game.path, game.isFavorite)
        }
    }

    fun recordGameLaunched(path: String) {
        val now = System.currentTimeMillis()
        _games.value = _games.value.map { game ->
            if (game.path == path) game.copy(lastPlayed = now) else game
        }
        applyFilters()

        viewModelScope.launch(Dispatchers.IO) {
            romRepository.recordPlaySession(filePath = path, sessionDurationMs = 0L, playedAt = now)
        }
    }

    private fun applyFilters() {
        val query = _searchQuery.value
        val favoritesOnly = _favoritesOnly.value
        val base = _games.value

        val filtered = base
            .asSequence()
            .filter { !favoritesOnly || it.isFavorite }
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
            .sortedWith(
                compareByDescending<GameItem> { it.isFavorite }
                    .thenByDescending { it.lastPlayed }
                    .thenBy { it.name.lowercase() }
            )
            .toList()

        _filteredGames.value = filtered
        _totalGames.value = base.size
        _favoriteGames.value = base.count { it.isFavorite }
        updateRecentGames(base)
    }
}
