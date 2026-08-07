package com.zeper.player.music.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.zeper.player.music.data.MusicPlaybackManager
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(
    playbackManager: MusicPlaybackManager,
    onBack: () -> Unit
) {
    val currentSong by playbackManager.currentSong.collectAsState()
    val isPlaying by playbackManager.isPlaying.collectAsState()
    val currentPosition by playbackManager.currentPosition.collectAsState()
    val duration by playbackManager.duration.collectAsState()
    val shuffleMode by playbackManager.shuffleModeEnabled.collectAsState()
    val repeatMode by playbackManager.repeatMode.collectAsState()
    val playbackSpeed by playbackManager.playbackSpeed.collectAsState()

    if (currentSong == null) {
        onBack()
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Equalizer placeholder */ }) {
                        Icon(Icons.Default.Tune, contentDescription = "Equalizer")
                    }
                    IconButton(onClick = { /* Menu */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Vinyl Record Visual ───────────────────────────
            VinylRecord(isPlaying = isPlaying)

            // ── Song Info ─────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { /* Alarm */ }) {
                    Icon(Icons.Default.Alarm, null, modifier = Modifier.size(24.dp))
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = currentSong!!.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentSong!!.artist ?: "<unknown>",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                IconButton(onClick = { /* Favorite */ }) {
                    Icon(Icons.Default.FavoriteBorder, null, modifier = Modifier.size(24.dp))
                }
            }

            // ── Progress Bar ──────────────────────────────────
            Column {
                Slider(
                    value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                    onValueChange = { playbackManager.seekTo((it * duration).toLong()) },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(currentPosition), style = MaterialTheme.typography.labelSmall)
                    Text(formatTime(duration), style = MaterialTheme.typography.labelSmall)
                }
            }

            // ── Main Controls ─────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { playbackManager.toggleShuffle() }) {
                    Icon(
                        Icons.Default.Shuffle, 
                        null, 
                        tint = if (shuffleMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(onClick = { playbackManager.skipToPrevious() }) {
                    Icon(Icons.Default.SkipPrevious, null, modifier = Modifier.size(36.dp))
                }

                Surface(
                    onClick = { playbackManager.playPause() },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                IconButton(onClick = { playbackManager.skipToNext() }) {
                    Icon(Icons.Default.SkipNext, null, modifier = Modifier.size(36.dp))
                }

                IconButton(onClick = { playbackManager.toggleRepeat() }) {
                    Icon(
                        imageVector = when (repeatMode) {
                            Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                            Player.REPEAT_MODE_ALL -> Icons.Default.Repeat
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = null,
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Bottom Utils ──────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { playbackManager.seekBackward10s() }) {
                    Icon(Icons.Default.History, null) // Rewind icon
                }
                
                TextButton(onClick = { /* Cycle speed */ }) {
                    Text("${playbackSpeed}X", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }

                IconButton(onClick = { playbackManager.seekForward10s() }) {
                    Icon(Icons.Default.Update, null) // Forward icon
                }
            }
        }
    }
}

@Composable
fun VinylRecord(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .size(280.dp)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Record
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .rotate(if (isPlaying) rotation else 0f),
            shape = CircleShape,
            color = Color(0xFF1A1A1A),
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(8.dp, Color(0xFF2A2A2A))
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Grooves
                repeat(5) { i ->
                    Surface(
                        modifier = Modifier.size(240.dp - (i * 30).dp),
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f))
                    ) { }
                }
                // Center Label
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = Color.DarkGray
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MusicNote, null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
        
        // Stylus Arm (Static)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 10.dp, y = (-10).dp)
        ) {
            // Arm logic would go here, simplified to a line/dot
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 120.dp)
                    .rotate(-30f)
                    .background(Color.LightGray, RoundedCornerShape(2.dp))
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
