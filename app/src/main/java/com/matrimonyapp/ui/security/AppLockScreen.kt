package com.matrimonyapp.ui.security

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AppLockScreen(onUnlock: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
            Text("App locked", style = MaterialTheme.typography.headlineLarge, color = Color(0xFF102A56))
            Spacer(Modifier.height(8.dp))
            Text("Use your device screen lock to continue.", color = Color(0xFF1F3553))
            Spacer(Modifier.height(20.dp))
            Button(onClick = onUnlock) { Text("Unlock") }
        }
    }
}