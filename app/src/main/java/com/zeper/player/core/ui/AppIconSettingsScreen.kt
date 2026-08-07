package com.zeper.player.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zeper.player.R
import com.zeper.player.core.IconManager
import com.zeper.player.core.data.PreferencesManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppIconSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager(context) }
    val selectedLogoType by prefs.selectedLogoType.collectAsState(initial = "logo1")

    val icons = listOf(
        IconOptionData("logo1", "Rowend Dark", R.drawable.ic_launcher_logo1),
        IconOptionData("logo2", "Rowend Light", R.drawable.ic_launcher_logo2),
        IconOptionData("logo3", "Soccer Dark", R.drawable.ic_launcher_logo3),
        IconOptionData("logo4", "Soccer Light", R.drawable.ic_launcher_logo4)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App icon") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(icons) { icon ->
                    IconItem(
                        data = icon,
                        isSelected = selectedLogoType == icon.id,
                        onClick = {
                            scope.launch {
                                prefs.setSelectedLogoType(icon.id)
                                IconManager.changeIcon(context, icon.id)
                            }
                        }
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
        }
    }
}

data class IconOptionData(val id: String, val name: String, val resId: Int)

@Composable
fun IconItem(data: IconOptionData, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clickable { onClick() }
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            shape = RoundedCornerShape(12.dp),
            border = if (isSelected) BorderStroke(2.dp, Color.Red) else BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
            color = Color.White
        ) {
            AsyncImage(
                model = data.resId,
                contentDescription = data.name,
                modifier = Modifier.padding(8.dp).fillMaxSize()
            )
        }
        
        if (isSelected) {
            Surface(
                shape = CircleShape,
                color = Color.Red,
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(2.dp)
                )
            }
        }
    }
}
