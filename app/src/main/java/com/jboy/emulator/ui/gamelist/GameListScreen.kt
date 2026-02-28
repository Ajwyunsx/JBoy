package com.jboy.emulator.ui.gamelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jboy.emulator.ui.components.EmptyState
import com.jboy.emulator.ui.components.GameCard
import com.jboy.emulator.ui.i18n.l10n

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameListScreen(
    onGameClick: (GameItem) -> Unit,
    onSettingsClick: () -> Unit,
    onAddGameClick: () -> Unit,
    viewModel: GameListViewModel = viewModel()
) {
    val games by viewModel.filteredGames.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val favoritesOnly by viewModel.favoritesOnly.collectAsState()
    val totalGames by viewModel.totalGames.collectAsState()
    val favoriteGames by viewModel.favoriteGames.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val pullToRefreshState = rememberPullToRefreshState()
    
    var isSearchActive by remember { mutableStateOf(false) }

    // 下拉刷新逻辑
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.loadGames()
            pullToRefreshState.endRefresh()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = l10n("游戏库"),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (totalGames > 0) {
                            Text(
                                text = l10n("共 ${totalGames} 款游戏 · 收藏 ${favoriteGames}"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    // 搜索按钮
                    IconButton(onClick = { isSearchActive = !isSearchActive }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = l10n("搜索"),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // 设置按钮
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = l10n("设置"),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // 刷新按钮
                    IconButton(onClick = { viewModel.loadGames() }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = l10n("刷新"),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { viewModel.toggleFavoritesOnly() }) {
                        Icon(
                            imageVector = if (favoritesOnly) {
                                Icons.Filled.Favorite
                            } else {
                                Icons.Outlined.FavoriteBorder
                            },
                            contentDescription = l10n(if (favoritesOnly) "显示全部" else "仅看收藏"),
                            tint = if (favoritesOnly) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddGameClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = l10n("添加游戏")
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 搜索栏（当激活时显示）
            if (isSearchActive) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::searchGames,
                    onClear = {
                        viewModel.clearSearch()
                        isSearchActive = false
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // 游戏列表或空状态
            if (games.isEmpty() && !isLoading) {
                EmptyState(
                    modifier = Modifier.fillMaxSize(),
                    title = l10n(if (searchQuery.isNotEmpty()) "未找到游戏" else "暂无游戏"),
                    description = if (searchQuery.isNotEmpty()) {
                        l10n("尝试其他搜索词")
                    } else {
                        l10n("点击右下角按钮添加游戏")
                    }
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(
                        top = if (isSearchActive) 68.dp else 8.dp,
                        bottom = 88.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = games,
                        key = { it.id }
                    ) { game ->
                        GameCard(
                            game = game,
                            onClick = {
                                viewModel.recordGameLaunched(game.path)
                                onGameClick(game)
                            },
                            onFavoriteClick = { viewModel.toggleFavorite(game.id) }
                        )
                    }
                }
            }

            // 下拉刷新指示器
            PullToRefreshContainer(
                modifier = Modifier.align(Alignment.TopCenter),
                state = pullToRefreshState
            )

            // 加载指示器
            if (isLoading && games.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.SearchBar(
        modifier = modifier.fillMaxWidth(),
        query = query,
        onQueryChange = onQueryChange,
        onSearch = { },
        active = false,
        onActiveChange = { },
        placeholder = { Text(l10n("搜索游戏...")) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = l10n("清除")
                    )
                }
            }
        }
    ) {
    }
}
