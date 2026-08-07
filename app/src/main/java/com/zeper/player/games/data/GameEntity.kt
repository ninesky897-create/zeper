package com.zeper.player.games.data

data class Game(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val iconRes: Int? = null,
    val webUrl: String // For offline-capable web games or local HTML5
)

object GameProvider {
    val games = listOf(
        Game("1", "Nokia Snake", "Classic", "The legendary retro Nokia snake experience", null, "internal://snake_nokia"),
        Game("2", "Modern Snake", "Modern", "Beautiful graphics and smooth movement", null, "internal://snake_modern"),
        Game("3", "Online PvP Snake", "Online", "Battle against players worldwide", null, "https://snake.io/"),
        Game("4", "Cobra Mode", "Expert", "High speed challenge for pro players", null, "internal://snake_cobra")
    )
}

