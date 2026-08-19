package com.zeper.player.games.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun SandtrixGame(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    
    val width = 30
    val height = 40
    val grid = remember { mutableStateListOf<Color?>().apply { repeat(width * height) { add(null) } } }
    
    var currentBlock by remember { mutableStateOf(generateNewBlock(width)) }
    var score by remember { mutableIntStateOf(0) }
    var gameOver by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }

    // Game loop
    LaunchedEffect(gameOver, isPaused) {
        while (!gameOver && !isPaused) {
            delay(400) // Base falling speed
            
            // Try to move block down
            val nextPos = currentBlock.positions.map { it.first to it.second + 1 }
            if (isValidMove(nextPos, width, height, grid)) {
                currentBlock = currentBlock.copy(positions = nextPos)
            } else {
                // Settle block into sand
                currentBlock.positions.forEach { (x, y) ->
                    if (y >= 0 && y < height) {
                        grid[y * width + x] = currentBlock.color
                    }
                }
                
                // Check for line clears
                clearLines(grid, width, height) { rowsCleared ->
                    score += rowsCleared * 100
                }

                if (currentBlock.positions.any { it.second <= 0 }) {
                    gameOver = true
                } else {
                    currentBlock = generateNewBlock(width)
                }
            }
        }
    }

    // Physics loop (Sand falling)
    LaunchedEffect(gameOver, isPaused) {
        while (!gameOver && !isPaused) {
            delay(50) // Faster physics for sand
            var changed = false
            // Process from bottom up
            for (y in height - 2 downTo 0) {
                for (x in 0 until width) {
                    val color = grid[y * width + x]
                    if (color != null) {
                        // Check straight down
                        if (grid[(y + 1) * width + x] == null) {
                            grid[(y + 1) * width + x] = color
                            grid[y * width + x] = null
                            changed = true
                        } else {
                            // Try diagonal
                            val sides = listOf(-1, 1).shuffled()
                            var moved = false
                            for (dx in sides) {
                                val nx = x + dx
                                if (nx in 0 until width && grid[(y + 1) * width + nx] == null) {
                                    grid[(y + 1) * width + nx] = color
                                    grid[y * width + x] = null
                                    moved = true
                                    changed = true
                                    break
                                }
                            }
                        }
                    }
                }
            }
            if (changed) {
                // Re-check line clears after sand settles
                clearLines(grid, width, height) { rowsCleared ->
                    score += rowsCleared * 50
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F0F)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Score: $score", color = Color.Cyan, fontSize = 20.sp)
            Button(onClick = { isPaused = !isPaused }) {
                Text(if (isPaused) "Resume" else "Pause")
            }
        }

        Box(
            modifier = Modifier
                .size(300.dp, 400.dp)
                .background(Color.Black)
                .border(2.dp, Color.DarkGray, RoundedCornerShape(4.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellW = size.width / width
                val cellH = size.height / height

                // Draw settled sand
                grid.forEachIndexed { index, color ->
                    if (color != null) {
                        val x = index % width
                        val y = index / width
                        drawRect(
                            color = color,
                            topLeft = Offset(x * cellW, y * cellH),
                            size = Size(cellW, cellH)
                        )
                    }
                }

                // Draw falling block
                if (!gameOver) {
                    currentBlock.positions.forEach { (x, y) ->
                        if (y >= 0) {
                            drawRect(
                                color = currentBlock.color,
                                topLeft = Offset(x * cellW, y * cellH),
                                size = Size(cellW, cellH)
                            )
                        }
                    }
                }
            }

            if (gameOver) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("GAME OVER", color = Color.Red, fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            grid.fill(null)
                            score = 0
                            gameOver = false
                            currentBlock = generateNewBlock(width)
                        }) {
                            Text("Restart")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Controls
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ControlBtn("←") {
                val next = currentBlock.positions.map { it.first - 1 to it.second }
                if (isValidMove(next, width, height, grid)) currentBlock = currentBlock.copy(positions = next)
            }
            Column {
                ControlBtn("⟳") {
                    val rotated = rotateBlock(currentBlock)
                    if (isValidMove(rotated, width, height, grid)) currentBlock = currentBlock.copy(positions = rotated)
                }
                Spacer(modifier = Modifier.height(8.dp))
                ControlBtn("↓") {
                    val next = currentBlock.positions.map { it.first to it.second + 1 }
                    if (isValidMove(next, width, height, grid)) currentBlock = currentBlock.copy(positions = next)
                }
            }
            ControlBtn("→") {
                val next = currentBlock.positions.map { it.first + 1 to it.second }
                if (isValidMove(next, width, height, grid)) currentBlock = currentBlock.copy(positions = next)
            }
        }
    }
}

@Composable
fun ControlBtn(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(60.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
    ) {
        Text(text, fontSize = 24.sp, color = Color.White)
    }
}

data class SandBlock(
    val positions: List<Pair<Int, Int>>,
    val color: Color,
    val type: Int // 0-6 for Tetris shapes
)

fun generateNewBlock(width: Int): SandBlock {
    val types = listOf(
        listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1), // O
        listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0), // I
        listOf(0 to 0, 1 to 0, 2 to 0, 1 to 1), // T
        listOf(0 to 0, 1 to 0, 1 to 1, 2 to 1), // Z
        listOf(1 to 0, 2 to 0, 0 to 1, 1 to 1), // S
        listOf(0 to 0, 0 to 1, 1 to 1, 2 to 1), // L
        listOf(2 to 0, 0 to 1, 1 to 1, 2 to 1)  // J
    )
    val typeIdx = Random.nextInt(types.size)
    val color = listOf(Color.Yellow, Color.Cyan, Color.Magenta, Color.Red, Color.Green, Color.Blue, Color.White).random()
    val startX = width / 2 - 1
    return SandBlock(types[typeIdx].map { it.first + startX to it.second }, color, typeIdx)
}

fun isValidMove(pos: List<Pair<Int, Int>>, w: Int, h: Int, grid: List<Color?>): Boolean {
    return pos.all { (x, y) ->
        x in 0 until w && y < h && (y < 0 || grid[y * w + x] == null)
    }
}

fun rotateBlock(block: SandBlock): List<Pair<Int, Int>> {
    if (block.type == 0) return block.positions // O shape doesn't rotate
    val center = block.positions[1]
    return block.positions.map { (x, y) ->
        val dx = x - center.first
        val dy = y - center.second
        center.first - dy to center.second + dx
    }
}

fun clearLines(grid: MutableList<Color?>, w: Int, h: Int, onClear: (Int) -> Unit) {
    var cleared = 0
    for (y in 0 until h) {
        var filledCount = 0
        for (x in 0 until w) {
            if (grid[y * w + x] != null) filledCount++
        }
        // If row is > 90% filled, clear it (Sand physics makes it hard to fill 100%)
        if (filledCount >= w - 1) { 
            for (x in 0 until w) grid[y * w + x] = null
            cleared++
        }
    }
    if (cleared > 0) onClear(cleared)
}
