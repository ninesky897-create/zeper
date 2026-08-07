package com.zeper.player

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.zeper.player.core.ui.theme.ZeperTheme
import com.zeper.player.core.data.PreferencesManager
import com.zeper.player.video.ui.ShareViewModel

class ShareHandlerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val sharedUrl = if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)
        } else {
            null
        }

        if (sharedUrl == null) {
            finish()
            return
        }

        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val prefs = remember { PreferencesManager(context) }
            val themeMode by prefs.themeMode.collectAsState(initial = "auto")
            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            ZeperTheme(darkTheme = darkTheme, prefs = prefs) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val viewModel: ShareViewModel = viewModel()
                    
                    LaunchedEffect(sharedUrl) {
                        viewModel.fetchInfo(sharedUrl)
                    }

                    SharePopupContent(
                        viewModel = viewModel,
                        onDismiss = { finish() },
                        onDownloadStarted = {
                            // Keep activity alive for a moment or just finish
                            // The download happens in a background worker
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePopupContent(
    viewModel: ShareViewModel,
    onDismiss: () -> Unit,
    onDownloadStarted: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
        ) {
            Text(
                text = "Zeper Video Downloader",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            when (val s = state) {
                is ShareViewModel.UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ShareViewModel.UiState.Error -> {
                    Text(text = "Error: ${s.message}", color = MaterialTheme.colorScheme.error)
                    Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                        Text("Close")
                    }
                }
                is ShareViewModel.UiState.Success -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (s.thumbnail != null) {
                            AsyncImage(
                                model = s.thumbnail,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(120.dp, 70.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Gray.copy(alpha = 0.2f)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        
                        Text(
                            text = s.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 3,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Text(text = "Music", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        s.audioOptions.forEach { opt ->
                            QualityButton(label = opt.label, size = opt.size, onClick = {
                                viewModel.startDownload(opt, "audio")
                                onDownloadStarted()
                            })
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Video", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    FlowRow(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        s.videoOptions.forEach { opt ->
                            QualityButton(label = opt.label, size = opt.size, onClick = {
                                viewModel.startDownload(opt, "video")
                                onDownloadStarted()
                            })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        content = { content() }
    )
}

@Composable
fun QualityButton(label: String, size: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.widthIn(min = 100.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Text(text = size, style = MaterialTheme.typography.labelSmall)
        }
    }
}
