package com.jboy.emulator.ui.i18n

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

private data class ReplaceRule(
    val regex: Regex,
    val replacement: (MatchResult) -> String
)

private val directZhToEn = mapOf(
    "返回" to "Back",
    "设置" to "Settings",
    "主题设置" to "Theme",
    "启用自定义主题" to "Enable custom theme",
    "主题预设" to "Theme preset",
    "主色 Primary" to "Primary",
    "辅色 Secondary" to "Secondary",
    "强调色 Tertiary" to "Tertiary",
    "背景色 Background" to "Background",
    "表面色 Surface" to "Surface",
    "视频设置" to "Video",
    "视频滤镜" to "Video filter",
    "屏幕比例" to "Aspect ratio",
    "显示FPS" to "Show FPS",
    "音频设置" to "Audio",
    "启用音频" to "Enable audio",
    "主音量" to "Master volume",
    "音频采样率" to "Audio sample rate",
    "音频缓冲区" to "Audio buffer size",
    "启用音频过滤" to "Enable audio filter",
    "音频过滤器级别" to "Audio filter level",
    "控制设置" to "Controls",
    "手柄透明度" to "Controller opacity",
    "按钮大小" to "Button size",
    "方向控制模式" to "D-pad mode",
    "振动反馈" to "Vibration",
    "启用Game Boy控制器震动" to "Enable GB controller rumble",
    "映射 A 键" to "Map A",
    "映射 B 键" to "Map B",
    "映射 UP 键" to "Map UP",
    "映射 DOWN 键" to "Map DOWN",
    "映射 LEFT 键" to "Map LEFT",
    "映射 RIGHT 键" to "Map RIGHT",
    "映射 L 键" to "Map L",
    "映射 R 键" to "Map R",
    "映射 START" to "Map START",
    "映射 SELECT" to "Map SELECT",
    "实体手柄映射（蓝牙/OTG）" to "Hardware gamepad mapping (Bluetooth/OTG)",
    "按键" to "button",
    "显示虚拟按键" to "Show virtual controls",
    "游戏设置" to "Gameplay",
    "退出时自动存档" to "Auto-save on exit",
    "切后台自动暂停" to "Auto-pause in background",
    "启用跳帧" to "Enable frame skip",
    "跳帧搁置（百分比）" to "Frame skip throttle (%)",
    "跳帧间隔" to "Frame skip interval",
    "禁用" to "Disabled",
    "帧间混合" to "Interframe blending",
    "空闲循环移除" to "Idle loop removal",
    "系统设置" to "System",
    "BIOS文件" to "BIOS file",
    "未选择（使用HLE）" to "Not selected (use HLE)",
    "快进默认速度" to "Default fast-forward speed",
    "语言" to "Language",
    "沉浸模式" to "Immersive mode",
    "应用" to "Apply",
    "默认" to "Default",
    "等待中" to "Listening",
    "学习" to "Learn",
    "未绑定" to "Unbound",
    "选择" to "Select",
    "最邻近" to "Nearest",
    "线性" to "Linear",
    "高级" to "Advanced",
    "原始" to "Original",
    "拉伸" to "Stretch",
    "适应屏幕" to "Fit Screen",
    "整数缩放" to "Integer Scale",
    "无限制" to "Unlimited",
    "轮盘十字键" to "Wheel D-pad",
    "摇杆" to "Joystick",
    "JBOY 青色" to "JBOY Cyan",
    "落日橙" to "Sunset Orange",
    "复古绿" to "Retro Green",
    "深海蓝" to "Deep Ocean Blue",
    "删除已知空闲循环" to "Remove known idle loops",
    "检测并删除" to "Detect and remove",
    "不移除" to "Do not remove",
    "简体中文" to "Simplified Chinese",
    "繁體中文" to "Traditional Chinese",
    "日本語" to "Japanese",
    "游戏库" to "Library",
    "搜索" to "Search",
    "刷新" to "Refresh",
    "显示全部" to "Show all",
    "仅看收藏" to "Favorites only",
    "添加游戏" to "Add game",
    "未找到游戏" to "No games found",
    "暂无游戏" to "No games yet",
    "尝试其他搜索词" to "Try another keyword",
    "点击右下角按钮添加游戏" to "Tap the bottom-right button to add games",
    "点击添加按钮导入游戏" to "Tap the add button to import games",
    "搜索游戏..." to "Search games...",
    "清除" to "Clear",
    "游戏菜单" to "Game Menu",
    "继续" to "Resume",
    "暂停" to "Pause",
    "取消静音" to "Unmute",
    "静音" to "Mute",
    "快速存档" to "Quick Save",
    "快速读档" to "Quick Load",
    "重置游戏" to "Reset Game",
    "打开设置" to "Open Settings",
    "按键布局" to "Layout",
    "进入布局编辑" to "Edit Layout",
    "保存布局" to "Save Layout",
    "重置" to "Reset",
    "退出编辑" to "Exit Edit",
    "存档槽位" to "Save Slots",
    "输入槽位编号" to "Slot Number",
    "例如 12" to "e.g. 12",
    "存" to "Save",
    "读" to "Load",
    "添加槽位" to "Add Slot",
    "快进速度" to "Fast-forward",
    "GBA联机" to "GBA Netplay",
    "局域网联机已启用" to "LAN netplay enabled",
    "支持同一 Wi-Fi、热点、Tailscale/虚拟局域网，连接后可握手并准备开局" to "Supports same Wi-Fi, hotspot, and Tailscale virtual LAN. Connect, handshake, and get ready.",
    "打开联机面板" to "Open Netplay Panel",
    "金手指" to "Cheats",
    "示例: 82000000 03E7" to "Example: 82000000 03E7",
    "添加" to "Add",
    "暂无金手指代码" to "No cheat codes",
    "删除" to "Delete",
    "清空金手指" to "Clear Cheats",
    "退出游戏" to "Exit Game",
    "继续游戏" to "Continue",
    "移除" to "Remove",
    "菜单" to "Menu",
    "沉浸模式" to "Immersive",
    "布局编辑中：拖动按键后在菜单点击“保存布局”" to "Layout edit mode: drag controls and tap \"Save Layout\" in the menu.",
    "导入游戏" to "Import Games",
    "全选" to "Select All",
    "反选" to "Invert",
    "选择ROM文件" to "Select ROM Files",
    "支持 .gba 和 .zip 格式" to "Supports .gba and .zip",
    "浏览文件夹" to "Browse Folder",
    "选择文件" to "Select Files",
    "正在扫描..." to "Scanning...",
    "未找到ROM文件" to "No ROM files found",
    "出错了" to "Something went wrong",
    "重试" to "Retry",
    "GBA 联机教程" to "GBA Netplay Guide",
    "1. 两台设备在同一局域网，或同一 Tailscale 虚拟局域网。" to "1. Keep both devices on the same LAN or the same Tailscale network.",
    "2. 先由房主创建房间，再让队友在大厅加入同一房间。" to "2. Host creates a room first, then teammates join that room from the lobby.",
    "3. 双方连接后都点击\"我已准备\"，状态变成\"可开始联机\"。" to "3. After both connect, tap \"Ready\" on both sides until it shows \"can start\".",
    "4. 返回游戏后保持同 ROM 与同进度，再进入 Link 流程。" to "4. Return to game with the same ROM and matching progress, then enter Link flow.",
    "5. 连不上时，优先检查服务器地址、端口 8080、防火墙放行。" to "5. If connection fails, check server address, port 8080, and firewall rules.",
    "我知道了" to "Got it",
    "稍后再看" to "Later",
    "GBA联机大厅" to "GBA Netplay Lobby",
    "联机教程" to "Netplay Guide",
    "刷新大厅" to "Refresh Lobby",
    "联机大厅状态" to "Lobby Status",
    "协议：jboy-link-1 · 支持局域网与 Tailscale 虚拟局域网" to "Protocol: jboy-link-1 · LAN + Tailscale supported",
    "联机握手已就绪：可开始 GBA Link" to "Handshake ready: you can start GBA Link",
    "等待双方准备完成" to "Waiting for both players to be ready",
    "创建房间" to "Create Room",
    "房主创建后会自动进入房间，队友可在大厅列表直接加入" to "Host enters automatically after creating. Teammates can join from the lobby list.",
    "房间名" to "Room Name",
    "创建并进入" to "Create & Join",
    "随机房间名" to "Random Name",
    "联机大厅房间" to "Lobby Rooms",
    "刷新中..." to "Refreshing...",
    "大厅暂时没有房间，先创建一个吧！" to "No rooms in lobby yet. Create one first!",
    "填入房间号" to "Fill Room ID",
    "加入房间" to "Join Room",
    "连接设置" to "Connection Settings",
    "服务器地址 (默认端口 8080)" to "Server Address (default port 8080)",
    "房间号" to "Room ID",
    "昵称" to "Nickname",
    "连接中" to "Connecting",
    "连接" to "Connect",
    "断开" to "Disconnect",
    "发握手" to "Handshake",
    "取消准备" to "Cancel Ready",
    "我已准备" to "Ready",
    "错误：" to "Error: ",
    "最后消息：" to "Last message: ",
    "(无)" to "(none)",
    "未连接" to "Disconnected",
    "未握手" to "Not Handshaked",
    "大厅地址无效，请检查服务器地址" to "Invalid lobby address. Check server address.",
    "大厅刷新失败" to "Lobby refresh failed",
    "未知错误" to "Unknown error",
    "请输入房间名" to "Please enter a room name",
    "正在创建并进入房间..." to "Creating and entering room...",
    "房间号无效" to "Invalid room ID",
    "连接失败" to "Connection failed",
    "服务器地址无效或缺少房间/昵称" to "Invalid server address or missing room/nickname",
    "连接中..." to "Connecting...",
    "已连接，准备握手" to "Connected, ready for handshake",
    "握手已发送，等待服务端确认" to "Handshake sent, waiting for server ack",
    "已发送准备状态" to "Ready state sent",
    "已取消准备状态" to "Ready canceled",
    "Ping 已发送" to "Ping sent",
    "收到服务端消息" to "Server message received",
    "有玩家加入房间" to "A player joined the room",
    "有玩家离开房间" to "A player left the room",
    "握手完成，可开始联机" to "Handshake completed, netplay can start",
    "握手完成，等待双方准备" to "Handshake completed, waiting for both players",
    "协议已同步 (jboy-link-1)" to "Protocol synced (jboy-link-1)",
    "已收到 Pong" to "Pong received",
    "收到协议消息" to "Protocol message received",
    "本地模式" to "Local mode",
    "联机已连接，等待准备" to "Netplay connected, waiting for ready",
    "模拟器核心初始化失败" to "Failed to initialize emulator core",
    "ROM 加载失败" to "Failed to load ROM",
    "存档失败：槽位不可用" to "Save failed: invalid slot",
    "读档失败：该槽位没有存档" to "Load failed: no save in this slot",
    "重置失败：ROM重载失败" to "Reset failed: ROM reload failed",
    "BIOS已加载" to "BIOS loaded",
    "从未游玩" to "Never played",
    "刚刚" to "Just now",
    "收藏" to "Favorite",
    "取消收藏" to "Unfavorite"
)

private val rules = listOf(
    ReplaceRule(Regex("^共 (\\d+) 款游戏 · 收藏 (\\d+)$")) { m ->
        "${m.groupValues[1]} games · ${m.groupValues[2]} favorites"
    },
    ReplaceRule(Regex("^总槽位: (\\d+)$")) { m -> "Total slots: ${m.groupValues[1]}" },
    ReplaceRule(Regex("^槽位 (\\d+)$")) { m -> "Slot ${m.groupValues[1]}" },
    ReplaceRule(Regex("^导入成功 (\\d+) 个，跳过重复 (\\d+) 个$")) { m ->
        "Imported ${m.groupValues[1]}, skipped duplicates ${m.groupValues[2]}"
    },
    ReplaceRule(Regex("^成功导入 (\\d+) 个游戏$")) { m ->
        "Imported ${m.groupValues[1]} games"
    },
    ReplaceRule(Regex("^导入 (\\d+) 个$")) { m -> "Import ${m.groupValues[1]}" },
    ReplaceRule(Regex("^清除 \\((\\d+)\\)$")) { m -> "Clear (${m.groupValues[1]})" },
    ReplaceRule(Regex("^连接状态：(.*)$")) { m -> "Connection: ${toEnglishText(m.groupValues[1].trim())}" },
    ReplaceRule(Regex("^握手状态：(.*)$")) { m -> "Handshake: ${toEnglishText(m.groupValues[1].trim())}" },
    ReplaceRule(Regex("^对端玩家：(\\d+) · 对端已准备：(\\d+)$")) { m ->
        "Peers: ${m.groupValues[1]} · Ready peers: ${m.groupValues[2]}"
    },
    ReplaceRule(Regex("^房间号: (.+)$")) { m -> "Room ID: ${m.groupValues[1]}" },
    ReplaceRule(Regex("^房主: (.+)$")) { m -> "Host: ${m.groupValues[1]}" },
    ReplaceRule(Regex("^解析地址：(.+)$")) { m -> "Resolved URL: ${m.groupValues[1]}" },
    ReplaceRule(Regex("^错误：(.*)$")) { m -> "Error: ${toEnglishText(m.groupValues[1].trim())}" },
    ReplaceRule(Regex("^最后消息：(.*)$")) { m -> "Last message: ${toEnglishText(m.groupValues[1].trim())}" },
    ReplaceRule(Regex("^学习中：请按下 (.+)$")) { m -> "Learning: press ${toEnglishText(m.groupValues[1])}" },
    ReplaceRule(Regex("^(.+) 已绑定: (.+)$")) { m ->
        "${toEnglishText(m.groupValues[1])} bound: ${toEnglishText(m.groupValues[2])}"
    },
    ReplaceRule(Regex("^当前速度: (.+)$")) { m -> "Speed: ${m.groupValues[1]}" },
    ReplaceRule(Regex("^目标帧率: (.+)$")) { m -> "Target FPS: ${m.groupValues[1]}" },
    ReplaceRule(Regex("^联机已就绪: (.+)$")) { m -> "Netplay ready: ${m.groupValues[1]}" },
    ReplaceRule(Regex("^无法加载游戏: (.+)$")) { m -> "Failed to load game: ${m.groupValues[1]}" },
    ReplaceRule(Regex("^存档失败: (.+)$")) { m -> "Save failed: ${m.groupValues[1]}" },
    ReplaceRule(Regex("^读档失败: (.+)$")) { m -> "Load failed: ${m.groupValues[1]}" },
    ReplaceRule(Regex("^重置失败: (.+)$")) { m -> "Reset failed: ${m.groupValues[1]}" },
    ReplaceRule(Regex("^大厅已同步 \\((\\d+) 个房间\\)$")) { m ->
        "Lobby synced (${m.groupValues[1]} rooms)"
    },
    ReplaceRule(Regex("^大厅刷新失败: (.+)$")) { m ->
        "Lobby refresh failed: ${toEnglishText(m.groupValues[1])}"
    },
    ReplaceRule(Regex("^正在进入房间 (.+)$")) { m -> "Joining room ${m.groupValues[1]}" },
    ReplaceRule(Regex("^关闭中 \\((\\d+)\\)$")) { m -> "Closing (${m.groupValues[1]})" },
    ReplaceRule(Regex("^已断开 \\((\\d+)\\)$")) { m -> "Disconnected (${m.groupValues[1]})" },
    ReplaceRule(Regex("^(.+)分钟前$")) { m -> "${m.groupValues[1]} min ago" },
    ReplaceRule(Regex("^(.+)小时前$")) { m -> "${m.groupValues[1]} hr ago" },
    ReplaceRule(Regex("^(.+)天前$")) { m -> "${m.groupValues[1]} days ago" }
)

private fun currentLocaleLanguage(context: Context): String {
    val config = context.resources.configuration
    val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        config.locales[0]
    } else {
        @Suppress("DEPRECATION")
        config.locale
    }
    return locale?.language?.lowercase(Locale.US).orEmpty()
}

private fun shouldUseEnglish(language: String): Boolean {
    return language.startsWith("en")
}

private fun toEnglishText(raw: String): String {
    directZhToEn[raw]?.let { return it }
    rules.forEach { rule ->
        val matched = rule.regex.matchEntire(raw)
        if (matched != null) {
            return rule.replacement(matched)
        }
    }
    return raw
}

fun Context.l10n(raw: String): String {
    val language = currentLocaleLanguage(this)
    return if (shouldUseEnglish(language)) toEnglishText(raw) else raw
}

@Composable
fun l10n(raw: String): String {
    val config = LocalConfiguration.current
    val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        config.locales[0]
    } else {
        @Suppress("DEPRECATION")
        config.locale
    }
    val language = locale?.language?.lowercase(Locale.US).orEmpty()
    return if (shouldUseEnglish(language)) toEnglishText(raw) else raw
}
