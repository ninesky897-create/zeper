package com.zeper.player.games.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

enum class PlatformType { REAL, FAKE }

data class Platform(
    var x: Float,
    val y: Float,
    val width: Float,
    val type: PlatformType,
    var isMoving: Boolean = false,
    var speed: Float = 0f,
    var direction: Int = 1, // 1 for right, -1 for left
    val hasPowerUp: Boolean = false,
    val color: Color = if (type == PlatformType.FAKE) Color.Gray else Color(Random.nextInt(0xFFFFFF) or (0xFF shl 24))
)

@Composable
fun WaterJumpGame(onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    var score by remember { mutableIntStateOf(0) }
    var gameStarted by remember { mutableStateOf(false) }
    var gameOver by remember { mutableStateOf(false) }
    var gameOverReason by remember { mutableStateOf("") }

    var playerY by remember { mutableFloatStateOf(800f) } // Screen coordinates (0 at top, higher at bottom)
    var playerX by remember { mutableFloatStateOf(0f) }
    var playerVy by remember { mutableFloatStateOf(0f) }
    val playerRadius = 15f
    
    var worldYOffset by remember { mutableFloatStateOf(0f) } // To simulate camera moving up
    var waterLevel by remember { mutableFloatStateOf(1000f) } // Relative to world
    
    val platforms = remember { mutableStateListOf<Platform>() }
    
    val gravity = 0.8f
    val jumpStrength = -18f
    val powerJumpStrength = -35f
    
    fun resetGame() {
        score = 0
        playerX = 500f
        playerY = 800f
        playerVy = 0f
        worldYOffset = 0f
        waterLevel = 1000f
        platforms.clear()
        
        // Initial platforms
        platforms.add(Platform(300f, 900f, 400f, PlatformType.REAL, color = Color.Green))
        
        var nextY = 700f
        repeat(10) {
            val width = 150f + Random.nextFloat() * 100f
            val x = Random.nextFloat() * (1000f - width)
            platforms.add(Platform(x, nextY, width, PlatformType.REAL))
            nextY -= 200f
        }
        
        gameOver = false
        gameStarted = true
    }

    LaunchedEffect(gameStarted, gameOver) {
        if (!gameStarted || gameOver) return@LaunchedEffect
        
        while (!gameOver) {
            delay(16) // ~60 FPS
            
            // Apply Gravity
            playerVy += gravity
            playerY += playerVy
            
            // Water Rising
            val speedFactor = (score / 1000f).coerceIn(0.1f, 3.0f)
            waterLevel -= (1.5f * speedFactor)
            
            // Check Water Collision
            if (playerY > waterLevel) {
                gameOver = true
                gameOverReason = "Drowned in Water!"
            }

            // Move Platforms
            platforms.forEach { p ->
                if (p.isMoving) {
                    p.x += p.speed * p.direction
                    if (p.x < 0 || p.x + p.width > 1000) {
                        p.direction *= -1
                    }
                }
            }

            // Platform Collision (Only when falling)
            if (playerVy > 0) {
                val landingPlatform = platforms.find { p ->
                    playerY + playerRadius >= p.y && 
                    playerY - playerRadius <= p.y + 20 &&
                    playerX >= p.x && playerX <= p.x + p.width
                }
                
                if (landingPlatform != null) {
                    if (landingPlatform.type == PlatformType.FAKE) {
                        gameOver = true
                        gameOverReason = "Stepped on a Fake Platform!"
                    } else {
                        playerVy = if (landingPlatform.hasPowerUp) powerJumpStrength else jumpStrength
                        playerY = landingPlatform.y - playerRadius
                        
                        // Score calculation
                        val heightScore = ((1000 - landingPlatform.y) / 10).toInt()
                        if (heightScore > score) {
                            score = heightScore
                        }
                    }
                }
            }
            
            // Camera follow (Shift world if player goes too high)
            if (playerY < 400) {
                val diff = 400 - playerY
                playerY = 400f
                waterLevel += diff
                platforms.forEachIndexed { i, p ->
                    // We don't shift Y because we want to generate more as we go up.
                    // Actually, let's keep everything relative to world and shift camera.
                }
                worldYOffset += diff
            }
            
            // Generate New Platforms
            val highestPlatform = platforms.minByOrNull { it.y }?.y ?: 1000f
            if (highestPlatform > -worldYOffset - 500) {
                val nextY = highestPlatform - (150f + Random.nextFloat() * 100f)
                val width = (100f + Random.nextFloat() * 100f).coerceAtLeast(80f)
                val x = Random.nextFloat() * (1000f - width)
                
                val isFake = score > 200 && Random.nextFloat() < 0.2f
                val isMoving = score > 500 && Random.nextFloat() < (score / 5000f).coerceIn(0.1f, 0.8f)
                val hasPowerUp = !isFake && Random.nextFloat() < 0.05f
                
                platforms.add(Platform(
                    x = x,
                    y = nextY,
                    width = width,
                    type = if (isFake) PlatformType.FAKE else PlatformType.REAL,
                    isMoving = isMoving,
                    speed = (2f + Random.nextFloat() * 3f) * (score / 1000f + 1),
                    hasPowerUp = hasPowerUp
                ))
                
                // Remove old platforms far below screen to save memory
                if (platforms.size > 30) {
                    platforms.removeIf { it.y > -worldYOffset + 1500 }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        if (!gameStarted) {
                            resetGame()
                        } else if (!gameOver) {
                            // Control horizontal movement by tap side
                            val screenWidth = size.width
                            if (offset.x < screenWidth / 2) {
                                playerX = (playerX - 40f).coerceAtLeast(0f)
                            } else {
                                playerX = (playerX + 40f).coerceAtMost(1000f)
                            }
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scaleX = size.width / 1000f
            
            // Draw Platforms
            platforms.forEach { p ->
                val drawY = p.y + worldYOffset
                if (drawY in -100f..size.height + 100f) {
                    drawRect(
                        color = p.color,
                        topLeft = Offset(p.x * scaleX, drawY),
                        size = Size(p.width * scaleX, 20f)
                    )
                    
                    if (p.hasPowerUp) {
                        drawCircle(
                            color = Color.Yellow,
                            radius = 10f,
                            center = Offset((p.x + p.width/2) * scaleX, drawY - 10f)
                        )
                    }
                }
            }
            
            // Draw Player
            drawCircle(
                color = if (gameOver) Color.Red else Color.Cyan,
                radius = playerRadius * scaleX,
                center = Offset(playerX * scaleX, playerY + worldYOffset)
            )
            
            // Draw Water
            val waterTop = waterLevel + worldYOffset
            drawRect(
                color = Color.Blue.copy(alpha = 0.5f),
                topLeft = Offset(0f, waterTop),
                size = Size(size.width, size.height - waterTop + 1000f)
            )
            
            // Water Waves (simple effect)
            val wavePath = Path()
            wavePath.moveTo(0f, waterTop)
            for (i in 0..10) {
                val x = i * (size.width / 10)
                val y = waterTop + if (i % 2 == 0) -10f else 10f
                wavePath.lineTo(x, y)
            }
            wavePath.lineTo(size.width, waterTop)
            wavePath.close()
            drawPath(wavePath, Color.Blue.copy(alpha = 0.3f))
        }

        // HUD
        Text(
            text = "Score: $score",
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        if (!gameStarted) {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("WATER JUMP", color = Color.Cyan, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(16.dp))
                    Text("• Tap Left/Right to move", color = Color.White)
                    Text("• Don't touch Gray platforms", color = Color.LightGray)
                    Text("• Avoid rising Water", color = Color.Blue)
                    Text("• Yellow dots = Mega Jump", color = Color.Yellow)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { resetGame() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan)) {
                        Text("START GAME", color = Color.Black)
                    }
                }
            }
        }

        if (gameOver) {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("GAME OVER", color = Color.Red, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    Text(gameOverReason, color = Color.White, fontSize = 16.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("Final Score: $score", color = Color.Cyan, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(24.dp))
                    Row {
                        Button(onClick = { resetGame() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Green)) {
                            Text("RETRY", color = Color.Black)
                        }
                        Spacer(Modifier.width(16.dp))
                        Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                            Text("EXIT", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// Helper for tap detection in Boxer pointerInput
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectTapGestures(
    onPress: (Offset) -> Unit
) {
    androidx.compose.foundation.gestures.detectTapGestures(onTap = { onPress(it) })
}
