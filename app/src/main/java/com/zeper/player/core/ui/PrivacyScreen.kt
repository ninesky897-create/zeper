package com.zeper.player.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.zeper.player.R
import com.zeper.player.core.data.PreferencesManager
import com.zeper.player.core.util.SecurityManager
import kotlinx.coroutines.launch

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val securityManager = remember { SecurityManager(context) }
    val scope = rememberCoroutineScope()
    
    val privacyPin by prefs.privacyPin.collectAsState(initial = null)
    var isUnlocked by remember { mutableStateOf(false) }
    var inputPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    if (!isUnlocked) {
        if (privacyPin == null) {
            SetPinView(onPinSet = { pin ->
                scope.launch {
                    prefs.setPrivacyPin(pin)
                    isUnlocked = true
                }
            })
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Lock, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.privacy_enter_pin), style = MaterialTheme.typography.titleLarge)
                
                Spacer(Modifier.height(32.dp))
                
                // PIN dots
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    repeat(4) { i ->
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (i < inputPin.length) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f))
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                val wrongPinText = stringResource(R.string.privacy_wrong_pin)

                // Keypad
                PinKeypad(onKeyClick = { key ->
                    if (inputPin.length < 4) {
                        inputPin += key
                        if (inputPin.length == 4) {
                            if (prefs.verifyPin(inputPin, privacyPin)) {
                                isUnlocked = true
                            } else {
                                inputPin = ""
                                errorMessage = wrongPinText
                            }
                        }
                    }
                }, onDelete = {
                    if (inputPin.isNotEmpty()) inputPin = inputPin.dropLast(1)
                })

                if (securityManager.canAuthenticate()) {
                    IconButton(onClick = {
                        securityManager.showBiometricPrompt(
                            activity = context as FragmentActivity,
                            onSuccess = { isUnlocked = true },
                            onError = { /* Handle */ }
                        )
                    }, modifier = Modifier.padding(top = 16.dp)) {
                        Icon(Icons.Default.Fingerprint, null, modifier = Modifier.size(48.dp))
                    }
                }

                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
                }
            }
        }
    } else {
        // Vault Content
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.FolderSpecial, null, modifier = Modifier.size(80.dp), tint = Color.Gray.copy(alpha = 0.5f))
                Text(stringResource(R.string.privacy_empty), color = Color.Gray)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { isUnlocked = false }) {
                    Text("Lock Vault")
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onBack) {
                    Text("Back to Library")
                }
            }
        }
    }
}

@Composable
fun SetPinView(onPinSet: (String) -> Unit) {
    var step by remember { mutableIntStateOf(1) }
    var pin1 by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(if (step == 1) stringResource(R.string.privacy_new_pin) else "Confirm PIN", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(32.dp))
        
        val currentPin = if (step == 1) pin1 else pin2
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { i ->
                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(if (i < currentPin.length) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f)))
            }
        }

        Spacer(Modifier.height(32.dp))

        PinKeypad(onKeyClick = { key ->
            if (step == 1) {
                if (pin1.length < 4) {
                    pin1 += key
                    if (pin1.length == 4) step = 2
                }
            } else {
                if (pin2.length < 4) {
                    pin2 += key
                    if (pin2.length == 4) {
                        if (pin1 == pin2) onPinSet(pin1)
                        else {
                            pin1 = ""; pin2 = ""; step = 1
                        }
                    }
                }
            }
        }, onDelete = {
            if (step == 1 && pin1.isNotEmpty()) pin1 = pin1.dropLast(1)
            if (step == 2 && pin2.isNotEmpty()) pin2 = pin2.dropLast(1)
        })
    }
}

@Composable
fun PinKeypad(onKeyClick: (String) -> Unit, onDelete: () -> Unit) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "DEL")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { key ->
                    if (key.isEmpty()) {
                        Spacer(Modifier.size(64.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .clickable { if (key == "DEL") onDelete() else onKeyClick(key) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (key == "DEL") {
                                Icon(Icons.AutoMirrored.Filled.Backspace, null)
                            } else {
                                Text(key, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
