package com.matrimonyapp.ui.notification

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.matrimonyapp.data.notification.NotificationRepository
import kotlinx.coroutines.launch

@Composable
fun NotificationScreen(repository: NotificationRepository,onBack:()->Unit,modifier:Modifier=Modifier){
    val items by repository.items.collectAsState()
    val scope=rememberCoroutineScope()
    LaunchedEffect(Unit){repository.refresh()}
    Column(modifier.fillMaxSize().padding(20.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
            Text("Notifications",style=MaterialTheme.typography.headlineMedium,color=Color(0xFF102A56))
            TextButton(onClick=onBack){Text("Back",color=Color(0xFF102A56))}
        }
        Spacer(Modifier.height(12.dp))
        if(items.isEmpty()) Text("No notifications yet.",color=Color(0xFF1F3553))
        else LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){
            items(items,key={it.id}){item->
                Card(colors=CardDefaults.cardColors(containerColor=Color(0xF5FFFFFF)),modifier=Modifier.fillMaxWidth()){
                    Column(Modifier.padding(14.dp)){
                        Text(item.title,color=Color(0xFF102A56),style=MaterialTheme.typography.titleMedium)
                        Text(item.body,color=Color(0xFF1F3553))
                        if(item.readAt==null) TextButton(onClick={scope.launch{repository.markRead(item.id)} }){Text("Mark read",color=Color(0xFF102A56))}
                    }
                }
            }
        }
    }
}