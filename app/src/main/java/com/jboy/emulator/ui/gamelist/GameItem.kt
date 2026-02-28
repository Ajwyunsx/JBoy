package com.jboy.emulator.ui.gamelist

data class GameItem(
    val id: Long,
    val name: String,
    val path: String,
    val coverPath: String?,
    val lastPlayed: Long,
    val totalPlayTime: Long,
    val isFavorite: Boolean
)