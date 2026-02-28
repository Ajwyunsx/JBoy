package com.jboy.emulator.ui.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * 定义应用中的所有屏幕路由
 * 使用密封类确保路由定义的类型安全
 */
sealed class Screen(val route: String) {

    /**
     * 游戏列表页面
     * 显示所有可用的游戏ROM
     */
    object GameList : Screen("game_list")

    /**
     * 游戏页面
     * 参数: gamePath - 游戏ROM的完整文件路径
     */
    object Game : Screen("game/{gamePath}") {
        /**
         * 创建带参数的路由字符串
         * @param gamePath 游戏文件路径（需要URL编码）
         */
        fun createRoute(gamePath: String): String {
            return "game/$gamePath"
        }
    }

    /**
     * 设置页面
     * 包含各种模拟器配置选项
     */
    object Settings : Screen("settings")

    /**
     * ROM 导入页面
     */
    object RomPicker : Screen("rom_picker")

    /**
     * 联机测试页面
     */
    object Netplay : Screen("netplay")

    companion object {
        /**
         * 获取所有路由的列表，用于调试或日志记录
         */
        fun allRoutes(): List<String> = listOf(
            GameList.route,
            Game.route,
            Settings.route,
            RomPicker.route,
            Netplay.route
        )

        /**
         * 检查给定的路由字符串是否有效
         */
        fun isValidRoute(route: String): Boolean {
            return allRoutes().any { pattern ->
                route.startsWith(pattern.substringBefore("{")) ||
                        route == pattern
            }
        }
    }
}

/**
 * 导航参数常量
 * 用于在页面间传递数据的键名
 */
object NavArgs {
    const val GAME_PATH = "gamePath"
    const val GAME_TITLE = "gameTitle"
}

/**
 * 导航动画时长常量
 */
object NavAnimation {
    const val FADE_IN_DURATION = 300
    const val FADE_OUT_DURATION = 300
    const val SLIDE_IN_DURATION = 400
    const val SLIDE_OUT_DURATION = 400
}
