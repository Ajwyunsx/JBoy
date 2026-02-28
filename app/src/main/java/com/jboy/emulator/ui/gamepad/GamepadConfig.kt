package com.jboy.emulator.ui.gamepad

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 手柄配置数据类
 * 包含按钮位置、透明度、大小比例、振动反馈等设置
 */
data class GamepadConfig(
    /**
     * 按钮透明度 (0.0 - 1.0)
     * 默认 0.4，建议范围 0.3-0.5
     */
    val buttonAlpha: Float = 0.4f,

    /**
     * D-Pad 方向键大小（dp）
     */
    val dpadSize: Float = 120f,

    /**
     * 动作按钮（A/B）大小（dp）
     */
    val actionButtonSize: Float = 72f,

    /**
     * 肩键宽度（dp）
     */
    val shoulderButtonWidth: Float = 80f,

    /**
     * 肩键高度（dp）
     */
    val shoulderButtonHeight: Float = 40f,

    /**
     * 菜单按钮宽度（dp）
     */
    val menuButtonWidth: Float = 64f,

    /**
     * 菜单按钮高度（dp）
     */
    val menuButtonHeight: Float = 28f,

    /**
     * 按钮按下时的透明度
     */
    val pressedAlpha: Float = 0.8f,

    /**
     * 是否启用振动反馈
     */
    val vibrationEnabled: Boolean = true,

    /**
     * 振动持续时间（毫秒）
     */
    val vibrationDuration: Long = 30L,

    /**
     * 按钮按下时的缩放比例
     */
    val pressedScale: Float = 0.92f,

    /**
     * 布局配置
     */
    val layout: GamepadLayout = GamepadLayout.DEFAULT
) {
    companion object {
        /**
         * 默认配置
         */
        val DEFAULT = GamepadConfig()

        /**
         * 高透明度配置（按钮更明显）
         */
        val HIGH_VISIBILITY = GamepadConfig(
            buttonAlpha = 0.6f,
            pressedAlpha = 1.0f
        )

        /**
         * 低透明度配置（按钮更隐蔽）
         */
        val LOW_VISIBILITY = GamepadConfig(
            buttonAlpha = 0.25f,
            pressedAlpha = 0.6f
        )

        /**
         * 大按钮配置（适合大屏设备）
         */
        val LARGE = GamepadConfig(
            dpadSize = 160f,
            actionButtonSize = 96f,
            shoulderButtonWidth = 100f,
            shoulderButtonHeight = 50f,
            menuButtonWidth = 80f,
            menuButtonHeight = 36f
        )

        /**
         * 小按钮配置（适合小屏设备）
         */
        val COMPACT = GamepadConfig(
            dpadSize = 96f,
            actionButtonSize = 56f,
            shoulderButtonWidth = 64f,
            shoulderButtonHeight = 32f,
            menuButtonWidth = 56f,
            menuButtonHeight = 24f
        )
    }
}

/**
 * 手柄布局配置
 */
enum class GamepadLayout {
    /**
     * 默认布局
     */
    DEFAULT,

    /**
     * 紧凑布局
     */
    COMPACT,

    /**
     * 横向布局
     */
    LANDSCAPE,

    /**
     * 仅左侧（仅D-Pad）
     */
    LEFT_ONLY,

    /**
     * 仅右侧（仅动作按钮）
     */
    RIGHT_ONLY
}
