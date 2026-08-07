package com.zeper.player.core.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZprScreen(onBack: () -> Unit) {
    var isScanning by remember { mutableStateOf(false) }
    var currentMode by remember { mutableStateOf<String?>(null) } // "send" or "receive"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ZPR Share", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Pulse Animation for Search Icon
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = if (currentMode != null) 1.2f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                if (currentMode != null) "Searching for nearby devices..." else "Fast & Secure File Sharing",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "No Internet Required",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(60.dp))

            if (currentMode == null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ShareActionButton(
                        modifier = Modifier.weight(1f),
                        text = "SEND",
                        icon = Icons.Default.FileUpload,
                        color = Color(0xFF2196F3),
                        onClick = { currentMode = "send"; isScanning = true }
                    )
                    
                    ShareActionButton(
                        modifier = Modifier.weight(1f),
                        text = "RECEIVE",
                        icon = Icons.Default.FileDownload,
                        color = Color(0xFF4CAF50),
                        onClick = { currentMode = "receive"; isScanning = true }
                    )
                }

                Spacer(modifier = Modifier.height(64.dp))
                
                Text(
                    "Connected via",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ConnectionIcon(Icons.Default.Bluetooth, "Bluetooth")
                    ConnectionIcon(Icons.Default.Wifi, "Wi-Fi")
                    ConnectionIcon(Icons.Default.SettingsInputAntenna, "Hotspot")
                }
            } else {
                ScanningView(
                    mode = currentMode!!,
                    onCancel = { currentMode = null; isScanning = false }
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            
            // History Button
            TextButton(
                onClick = { /* TODO: Show History */ },
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Icon(Icons.Default.History, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Transfer History")
            }
        }
    }
}

@Composable
fun ShareActionButton(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(160.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(48.dp), tint = Color.White)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
        }
    }
}

@Composable
fun ScanningView(mode: String, onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(80.dp),
            strokeWidth = 6.dp,
            color = if (mode == "send") Color(0xFF2196F3) else Color(0xFF4CAF50)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            if (mode == "send") "Scanning for Receivers..." else "Waiting for Sender...",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedButton(
            onClick = onCancel,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Cancel")
        }
    }
}

@Composable
fun ConnectionIcon(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
        }
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
    }
}
