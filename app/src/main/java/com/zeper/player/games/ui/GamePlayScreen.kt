package com.zeper.player.games.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import coil.compose.AsyncImage
import com.zeper.player.core.data.PreferencesManager
import com.zeper.player.games.data.GameProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamePlayScreen(gameId: String, onBack: () -> Unit) {
    val game = remember(gameId) { GameProvider.games.find { it.id == gameId } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(game?.title ?: "Game") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF0A0A0A)),
            contentAlignment = Alignment.Center
        ) {
            when (gameId) {
                "1" -> SnakeGame(
                    mode = SnakeMode.CLASSIC,
                    baseSpeed = 1.0f,
                    background = Color(0xFF94A50E),
                    foreground = Color(0xFF2B3301),
                    onBack = onBack
                )
                "2" -> SnakeGame(
                    mode = SnakeMode.NO_BORDERS,
                    baseSpeed = 1.2f,
                    background = Color(0xFF1A1A1A),
                    foreground = Color(0xFF2196F3),
                    onBack = onBack
                )
                "3" -> OnlineSnakeWebView(game?.webUrl ?: "https://snake.io/", onBack)
                "4" -> SnakeGame(
                    mode = SnakeMode.CLASSIC,
                    baseSpeed = 2.0f,
                    background = Color(0xFF0D0D0D),
                    foreground = Color(0xFFFF9800),
                    onBack = onBack
                )
                "5" -> SnakeGarage(onExit = onBack)
                "6" -> LudoGame()
                "7" -> ChessGame()
                else -> PlaceholderGame(game?.title ?: "Unknown Game")
            }
        }
    }
}

@Composable
fun OnlineSnakeWebView(url: String, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun PuzzleGame() { PlaceholderGame("Space Puzzle") }
@Composable
fun RacerGame() { PlaceholderGame("Neon Racer") }
@Composable
fun TetrisGame() { PlaceholderGame("Tetris Neon") }
@Composable
fun CowboyGame() { PlaceholderGame("Cowboy Run") }
@Composable
fun LudoGame() { PlaceholderGame("Ludo Master") }
@Composable
fun ChessGame() { PlaceholderGame("Chess Daba") }

@Composable
fun PlaceholderGame(title: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.Cyan,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        CircularProgressIndicator(color = Color.Cyan)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Coming Soon Offline...",
            color = Color.LightGray,
            fontSize = 14.sp
        )
    }
}

// ── SNAKE MODES ──────────────────────────────────────────────────

enum class SnakeMode { GARAGE, CLASSIC, NO_BORDERS, LEVELS, SETTINGS }

@Composable
fun SnakeGarage(onExit: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    
    val themeMode by prefs.themeMode.collectAsState(initial = "auto")
    val customEnabled by prefs.customThemeEnabled.collectAsState(initial = false)
    val customBg by prefs.customBgColor.collectAsState(initial = "#000000")
    val customText by prefs.customTextColor.collectAsState(initial = "#00FFFF")

    var currentMode by remember { mutableStateOf(SnakeMode.GARAGE) }
    var baseSpeedMultiplier by remember { mutableFloatStateOf(1.0f) }

    // Logic to determine background and dark color based on theme
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDark = when {
        customEnabled -> {
            // Estimate darkness of custom color or just treat as dark if enabled
            true 
        }
        themeMode == "dark" -> true
        themeMode == "light" -> false
        else -> isSystemDark
    }

    val lcdBackground = if (customEnabled) {
        try { Color(android.graphics.Color.parseColor(customBg)) } catch (e: Exception) { Color(0xFF94A50E) }
    } else if (isDark) {
        Color(0xFF2B3301) // Swapped for dark theme
    } else {
        Color(0xFF94A50E) // Original light
    }

    val lcdDark = if (customEnabled) {
        try { Color(android.graphics.Color.parseColor(customText)) } catch (e: Exception) { Color(0xFF2B3301) }
    } else if (isDark) {
        Color(0xFF94A50E) // Swapped for dark theme
    } else {
        Color(0xFF2B3301) // Original dark
    }

    // Ensure colors are not the same (Opposite Logic)
    val finalLcdDark = remember(lcdBackground, lcdDark, isDark) {
        if (lcdBackground == lcdDark) {
            if (isDark) Color.White else Color.Black
        } else {
            lcdDark
        }
    }

    when (currentMode) {
        SnakeMode.GARAGE -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(lcdBackground)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("SNAKE", color = finalLcdDark, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                Text("II", color = finalLcdDark, fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Cobra Logo
                AsyncImage(
                    model = "https://img.icons8.com/ios-filled/100/cobra.png",
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    colorFilter = ColorFilter.tint(finalLcdDark)
                )

                Spacer(modifier = Modifier.height(32.dp))
                
                GarageButton("Classic Snake", finalLcdDark, lcdBackground) { currentMode = SnakeMode.CLASSIC }
                GarageButton("Snake (No Borders)", finalLcdDark, lcdBackground) { currentMode = SnakeMode.NO_BORDERS }
                GarageButton("Level Snake", finalLcdDark, lcdBackground) { currentMode = SnakeMode.LEVELS }
                GarageButton("Settings", finalLcdDark, lcdBackground) { currentMode = SnakeMode.SETTINGS }
                Spacer(modifier = Modifier.height(16.dp))
                GarageButton("EXIT", Color.Red, Color.White) { onExit() }
            }
        }
        SnakeMode.CLASSIC -> SnakeGame(mode = SnakeMode.CLASSIC, baseSpeed = baseSpeedMultiplier, background = lcdBackground, foreground = finalLcdDark, onBack = { currentMode = SnakeMode.GARAGE })
        SnakeMode.NO_BORDERS -> SnakeGame(mode = SnakeMode.NO_BORDERS, baseSpeed = baseSpeedMultiplier, background = lcdBackground, foreground = finalLcdDark, onBack = { currentMode = SnakeMode.GARAGE })
        SnakeMode.LEVELS -> SnakeGame(mode = SnakeMode.LEVELS, baseSpeed = baseSpeedMultiplier, background = lcdBackground, foreground = finalLcdDark, onBack = { currentMode = SnakeMode.GARAGE })
        SnakeMode.SETTINGS -> {
            Column(
                modifier = Modifier.fillMaxSize().background(lcdBackground).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("SETTINGS", color = finalLcdDark, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(32.dp))
                Text("Base Speed: ${String.format(java.util.Locale.US, "%.1f", baseSpeedMultiplier)}x", color = finalLcdDark)
                Slider(
                    value = baseSpeedMultiplier,
                    onValueChange = { baseSpeedMultiplier = it },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(thumbColor = finalLcdDark, activeTrackColor = finalLcdDark)
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = { currentMode = SnakeMode.GARAGE }, colors = ButtonDefaults.buttonColors(containerColor = finalLcdDark)) {
                    Text("BACK", color = lcdBackground)
                }
            }
        }
    }
}

@Composable
fun GarageButton(text: String, color: Color, textColor: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(text, color = textColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SnakeGame(mode: SnakeMode, baseSpeed: Float, background: Color, foreground: Color, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    
    BackHandler(onBack = onBack)
    var snakePath by remember { mutableStateOf(listOf(Pair(10, 8), Pair(10, 9), Pair(10, 10))) }
    var direction by remember { mutableStateOf(Pair(0, -1)) }
    var food by remember { mutableStateOf(Pair(5, 5)) }
    var score by remember { mutableStateOf(0) }
    var gameOver by remember { mutableStateOf(false) }
    var gameStarted by remember { mutableStateOf(false) }
    var speedDelay by remember { mutableLongStateOf((300L / baseSpeed).toLong()) }
    
    // Collision Grace Period
    var collisionDetected by remember { mutableStateOf(false) }
    var graceTimeRemaining by remember { mutableIntStateOf(10) } // 10 * 100ms = 1s

    // Level specific
    var currentLevel by remember { mutableIntStateOf(1) }

    // Power-up state
    var normalFoodCount by remember { mutableIntStateOf(0) }
    var isPowerUpActive by remember { mutableStateOf(false) }
    var powerUpTimeRemaining by remember { mutableIntStateOf(0) }
    var powerUpPos by remember { mutableStateOf(Pair(-1, -1)) }

    // Colors
    val headColor = Color(0xFFCD5959)

    // Timer for Power-up
    LaunchedEffect(isPowerUpActive, gameOver, collisionDetected) {
        while (isPowerUpActive && !gameOver && !collisionDetected) {
            delay(1000)
            if (powerUpTimeRemaining > 0) {
                powerUpTimeRemaining -= 1
            } else {
                isPowerUpActive = false
            }
        }
    }

    // Grace Period Timer
    LaunchedEffect(collisionDetected) {
        if (collisionDetected) {
            graceTimeRemaining = 10
            while (graceTimeRemaining > 0 && collisionDetected) {
                delay(100)
                graceTimeRemaining--
            }
            if (collisionDetected) {
                gameOver = true
                collisionDetected = false
            }
        }
    }

    LaunchedEffect(gameOver, speedDelay, gameStarted, collisionDetected) {
        while (!gameOver && gameStarted && !collisionDetected) {
            delay(speedDelay)
            val head = snakePath.first()
            var nextX = head.first + direction.first
            var nextY = head.second + direction.second

            // Border Logic
            if (mode == SnakeMode.NO_BORDERS) {
                if (nextX < 0) nextX = 19
                if (nextX >= 20) nextX = 0
                if (nextY < 0) nextY = 15
                if (nextY >= 16) nextY = 0
            } else {
                // Classic and Levels: Wall hit = Collision
                if (nextX < 0 || nextX >= 20 || nextY < 0 || nextY >= 16) {
                    collisionDetected = true
                    continue
                }
            }

            val newHead = Pair(nextX, nextY)

            // Self-bite check
            if (snakePath.contains(newHead)) {
                collisionDetected = true
                continue
            }

            if (!gameOver && !collisionDetected) {
                val newSnake = mutableListOf(newHead)
                newSnake.addAll(snakePath)
                
                var consumed = false
                if (newHead == food) {
                    score += 1
                    normalFoodCount += 1
                    speedDelay = (speedDelay / 1.02).toLong()
                    food = Pair((0..19).random(), (0..15).random())
                    consumed = true

                    // Level Up logic
                    if (mode == SnakeMode.LEVELS && score >= currentLevel * 100) {
                        currentLevel++
                    }

                    if (normalFoodCount >= 13) {
                        isPowerUpActive = true
                        powerUpTimeRemaining = 13
                        powerUpPos = Pair((0..19).random(), (0..15).random())
                        normalFoodCount = 0
                    }
                } 
                else if (isPowerUpActive && newHead == powerUpPos) {
                    score += powerUpTimeRemaining
                    isPowerUpActive = false
                    consumed = true
                }

                if (!consumed) {
                    newSnake.removeAt(newSnake.size - 1)
                }
                snakePath = newSnake
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        // Game Board with Pixel Border (Photo Style)
        Box(
            modifier = Modifier
                .width(320.dp)
                .height(300.dp)
                .background(background)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            // The "Dotted" Pixel Border (Hidden in No Borders Mode)
            if (mode != SnakeMode.NO_BORDERS) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val pixelSize = 4.dp.toPx()
                    val gap = 2.dp.toPx()
                    val total = pixelSize + gap
                    
                    // Draw horizontal dots
                    for (x in 0 until (size.width / total).toInt()) {
                        drawRect(foreground, Offset(x * total, 0f), Size(pixelSize, pixelSize))
                        drawRect(foreground, Offset(x * total, size.height - pixelSize), Size(pixelSize, pixelSize))
                    }
                    // Draw vertical dots
                    for (y in 0 until (size.height / total).toInt()) {
                        drawRect(foreground, Offset(0f, y * total), Size(pixelSize, pixelSize))
                        drawRect(foreground, Offset(size.width - pixelSize, y * total), Size(pixelSize, pixelSize))
                    }
                }
            }

            // Game Area
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                if (!gameStarted && !gameOver) {
                    Box(modifier = Modifier.fillMaxSize().background(background.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                        Text("TAP ANY CONTROL TO START", color = foreground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Food
                Box(
                    modifier = Modifier.offset(x = (food.first * 14).dp, y = (food.second * 14).dp).size(15.dp).background(foreground, CircleShape).border(1.dp, background, CircleShape)
                )

                if (isPowerUpActive) {
                    Box(
                        modifier = Modifier.offset(x = (powerUpPos.first * 14).dp, y = (powerUpPos.second * 14).dp).size(18.dp).background(foreground, CircleShape).padding(2.dp).background(background, CircleShape).padding(2.dp).background(foreground, CircleShape)
                    )
                }

                // Snake
                snakePath.forEachIndexed { index, (x, y) ->
                    val isHead = index == 0
                    val color = if (gameOver) Color.Red else if (isHead) headColor else foreground
                    
                    Box(modifier = Modifier.offset(x = (x * 14).dp, y = (y * 14).dp).size(14.dp).background(color, RoundedCornerShape(2.dp)).border(1.dp, background, RoundedCornerShape(2.dp))) {
                        if (isHead && !gameOver) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.align(Alignment.TopStart).padding(2.dp).size(3.dp).background(background, CircleShape))
                                Box(modifier = Modifier.align(Alignment.BottomStart).padding(2.dp).size(3.dp).background(background, CircleShape))
                            }
                        }
                    }
                }
            }

            if (gameOver) {
                Box(modifier = Modifier.fillMaxSize().background(background.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.border(2.dp, foreground, RoundedCornerShape(8.dp)).background(background, RoundedCornerShape(8.dp)).padding(24.dp)) {
                        Text("GAME OVER", color = Color.Red, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { 
                                snakePath = listOf(Pair(10, 8), Pair(10, 9), Pair(10, 10))
                                direction = Pair(0, -1)
                                score = 0
                                speedDelay = (300L / baseSpeed).toLong()
                                gameOver = false 
                                gameStarted = false
                                normalFoodCount = 0
                                isPowerUpActive = false
                                collisionDetected = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = foreground),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("RESTART", color = background, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (collisionDetected && !gameOver) {
                Box(modifier = Modifier.fillMaxSize().background(background.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("COLLISION! 1S TO SAVE", color = Color.Red, fontWeight = FontWeight.Black)
                        Text("${graceTimeRemaining * 100}ms", color = foreground)
                        Button(
                            onClick = {
                                val snakeData = snakePath.joinToString(";") { "${it.first},${it.second}" }
                                val saveData = "$score|$currentLevel|${food.first},${food.second}|$snakeData|${direction.first},${direction.second}"
                                scope.launch { prefs.saveSnakeGame(saveData) }
                                collisionDetected = false
                                gameOver = true // Stop game after saving
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow)
                        ) {
                            Text("SAVE GAME", color = Color.Black)
                        }
                    }
                }
            }
        }

        // Bottom Info Bar (Relocated from top)
        Box(
            modifier = Modifier
                .width(320.dp)
                .background(background)
                .padding(vertical = 16.dp, horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format(java.util.Locale.US, "%06d", score),
                    color = foreground, 
                    fontSize = 24.sp, 
                    fontWeight = FontWeight.Bold, 
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                
                // Progress Bar
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    val barsToFill = if (isPowerUpActive) powerUpTimeRemaining else normalFoodCount
                    repeat(13) { i ->
                        Box(modifier = Modifier.size(width = 8.dp, height = 12.dp).background(if (i < barsToFill) foreground else Color.Transparent).border(1.dp, foreground))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // Back Button to Garage
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Text("Back to Garage", color = Color.Gray)
            }
            
            val savedData by prefs.snakeSaveData.collectAsState(initial = null)
            if (savedData != null && !gameStarted && !gameOver) {
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = {
                        try {
                            val parts = savedData!!.split("|")
                            score = parts[0].toInt()
                            currentLevel = parts[1].toInt()
                            val foodParts = parts[2].split(",")
                            food = Pair(foodParts[0].toInt(), foodParts[1].toInt())
                            snakePath = parts[3].split(";").map {
                                val p = it.split(",")
                                Pair(p[0].toInt(), p[1].toInt())
                            }
                            val dirParts = parts[4].split(",")
                            direction = Pair(dirParts[0].toInt(), dirParts[1].toInt())
                            gameStarted = true
                        } catch (e: Exception) {}
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = foreground.copy(alpha = 0.5f))
                ) {
                    Text("LOAD SAVE", color = background)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Controls
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GameButton("↑") { 
                gameStarted = true
                collisionDetected = false // Allow escape from collision
                if (direction.second != 1) direction = Pair(0, -1) 
            }
            Row {
                GameButton("←") { 
                    gameStarted = true
                    collisionDetected = false
                    if (direction.first != 1) direction = Pair(-1, 0) 
                }
                Spacer(modifier = Modifier.width(64.dp))
                GameButton("→") { 
                    gameStarted = true
                    collisionDetected = false
                    if (direction.first != -1) direction = Pair(1, 0) 
                }
            }
            GameButton("↓") { 
                gameStarted = true
                collisionDetected = false
                if (direction.second != -1) direction = Pair(0, 1) 
            }
        }
    }
}

@Composable
fun GameButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(60.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}
