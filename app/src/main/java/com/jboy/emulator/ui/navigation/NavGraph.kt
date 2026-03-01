package com.jboy.emulator.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.jboy.emulator.ui.game.GameScreen
import com.jboy.emulator.ui.game.GameViewModel
import com.jboy.emulator.ui.gamelist.GameListScreen
import com.jboy.emulator.ui.gamelist.GameListViewModel
import com.jboy.emulator.ui.netplay.NetplayScreen
import com.jboy.emulator.ui.rompicker.RomPickerScreen
import com.jboy.emulator.ui.settings.SettingsScreen
import com.jboy.emulator.ui.settings.SettingsViewModel
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.GameList.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // 游戏列表页面
        composable(Screen.GameList.route) {
            val viewModel: GameListViewModel = viewModel()
            GameListScreen(
                onGameClick = { game ->
                    val encodedPath = URLEncoder.encode(game.path, StandardCharsets.UTF_8.toString())
                    navController.navigate(Screen.Game.createRoute(encodedPath))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onAddGameClick = {
                    navController.navigate(Screen.RomPicker.route)
                },
                viewModel = viewModel,
            )
        }

        // ROM 导入页面
        composable(Screen.RomPicker.route) {
            val gameListEntry = remember(navController) {
                navController.getBackStackEntry(Screen.GameList.route)
            }
            val gameListViewModel: GameListViewModel = viewModel(viewModelStoreOwner = gameListEntry)

            RomPickerScreen(
                onBack = {
                    navController.popBackStack()
                },
                onRomSelected = { romInfo ->
                    gameListViewModel.addRom(romInfo)
                }
            )
        }

        // 游戏页面
        composable(
            route = Screen.Game.route,
            arguments = listOf(
                navArgument("gamePath") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val gamePath = backStackEntry.arguments?.getString("gamePath")
                ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }
                ?: throw IllegalArgumentException("游戏路径不能为空")

            val viewModel: GameViewModel = hiltViewModel()

            GameScreen(
                gamePath = gamePath,
                onBackToList = {
                    navController.navigate(Screen.GameList.route) {
                        popUpTo(Screen.GameList.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onOpenSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onOpenNetplay = {
                    navController.navigate(Screen.Netplay.createRoute(gamePath))
                },
                viewModel = viewModel
            )
        }

        // 设置页面
        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = viewModel()
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                },
                viewModel = viewModel
            )
        }

        composable(
            route = Screen.Netplay.route,
            arguments = listOf(
                navArgument("gamePath") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val boundGamePath = backStackEntry.arguments?.getString("gamePath")
                ?.takeIf { it.isNotBlank() }
                ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }

            NetplayScreen(
                onBack = {
                    navController.popBackStack()
                },
                boundGamePath = boundGamePath
            )
        }
    }
}

/**
 * 导航辅助函数
 */
object NavigationActions {
    /**
     * 导航到游戏页面
     */
    fun NavHostController.navigateToGame(gamePath: String) {
        val encodedPath = URLEncoder.encode(gamePath, StandardCharsets.UTF_8.toString())
        navigate(Screen.Game.createRoute(encodedPath))
    }

    /**
     * 导航到设置页面
     */
    fun NavHostController.navigateToSettings() {
        navigate(Screen.Settings.route)
    }

    fun NavHostController.navigateToRomPicker() {
        navigate(Screen.RomPicker.route)
    }

    /**
     * 返回游戏列表
     */
    fun NavHostController.navigateToGameList() {
        popBackStack(Screen.GameList.route, inclusive = false)
    }
}
