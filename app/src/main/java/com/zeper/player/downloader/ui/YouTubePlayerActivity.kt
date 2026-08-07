package com.zeper.player.downloader.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class YouTubePlayerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val videoId = intent.getStringExtra("VIDEO_ID") ?: ""
        val title = intent.getStringExtra("TITLE") ?: "Video"
        val channel = intent.getStringExtra("CHANNEL") ?: "Channel"

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    YouTubePlayerScreen(videoId, title, channel, onBack = { finish() })
                }
            }
        }
    }
}

@Composable
fun YouTubePlayerScreen(videoId: String, title: String, channel: String, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            // Using a sample HLS stream as a placeholder for YouTube video content
            val mediaItem = MediaItem.fromUri("https://bitdash-a.akamaihd.net/content/MI201109210084_1/m3u8s/f08e80da-911d-472c-9116-c850515cdafb.m3u8")
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Player View
        Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        player = exoPlayer
                        useController = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            IconButton(onClick = onBack, modifier = Modifier.padding(8.dp).align(Alignment.TopStart)) {
                Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            item {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("1.2M views • 2 days ago", fontSize = 12.sp, color = Color.Gray)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, modifier = Modifier.size(40.dp), color = Color.DarkGray) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.padding(8.dp), tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(channel, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("5.4M subscribers", fontSize = 12.sp, color = Color.Gray)
                    }
                    Button(
                        onClick = { /* TODO */ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    ) {
                        Text("Subscribe", fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    PlayerAction(Icons.Default.ThumbUp, "124K")
                    PlayerAction(Icons.Default.Share, "Share")
                    PlayerAction(Icons.Default.Download, "Download")
                    PlayerAction(Icons.Default.Add, "Save")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.DarkGray)
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Related Videos", fontWeight = FontWeight.Bold, color = Color.White)
            }
            
            items(5) {
                RelatedVideoItem()
            }
        }
    }
}

@Composable
fun PlayerAction(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = Color.DarkGray.copy(alpha = 0.5f),
            modifier = Modifier.size(40.dp)
        ) {
            Icon(icon, null, modifier = Modifier.padding(8.dp), tint = Color.White)
        }
        Text(label, fontSize = 10.sp, color = Color.White, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun RelatedVideoItem() {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Box(modifier = Modifier.size(width = 140.dp, height = 80.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text("Next Recommended Video Title", fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, color = Color.White, fontSize = 14.sp)
            Text("Channel Name • 200K views", fontSize = 12.sp, color = Color.Gray)
        }
    }
}
