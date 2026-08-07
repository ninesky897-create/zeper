package com.zeper.player.games.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.ColorFilter
import coil.compose.AsyncImage
import com.zeper.player.games.data.Game
import com.zeper.player.games.data.GameProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesHubScreen(onGameClick: (Game) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredGames = remember(searchQuery) {
        GameProvider.games.filter { it.title.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Search Games...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            shape = MaterialTheme.shapes.medium
        )

        Text(
            "Snake World",
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredGames) { game ->
                GameCard(game, onGameClick)
            }
        }
    }
}

@Composable
fun GameCard(game: Game, onClick: (Game) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable { onClick(game) },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Custom Snake Icon
            AsyncImage(
                model = "https://img.icons8.com/ios-filled/100/cobra.png",
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                colorFilter = ColorFilter.tint(
                    when(game.category) {
                        "Classic" -> Color(0xFF4CAF50)
                        "Modern" -> Color(0xFF2196F3)
                        "Online" -> Color(0xFFFF9800)
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                game.title, 
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    game.category, 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}
