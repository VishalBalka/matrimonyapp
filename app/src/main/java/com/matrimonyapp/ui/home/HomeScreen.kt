package com.matrimonyapp.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matrimonyapp.core.result.AppResult
import com.matrimonyapp.data.discovery.*
import com.matrimonyapp.data.notification.NotificationRepository
import com.matrimonyapp.data.remote.AuthenticatedUser
import com.matrimonyapp.ui.theme.CloudNavy
import com.matrimonyapp.ui.theme.CloudNavySoft
import com.matrimonyapp.ui.theme.AppPrimary

@Composable
fun HomeScreen(
    user: AuthenticatedUser,
    searchRepository: ProfileSearchRepository,
    notifications: NotificationRepository,
    onOpenProfile: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var profiles by remember { mutableStateOf<List<DiscoveryProfile>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        when(val result=searchRepository.search(ProfileSearchFilters(),0)){
            is AppResult.Success -> profiles=result.value.items
            is AppResult.Failure -> {}
        }
        loading=false
    }

    Column(modifier.fillMaxSize().padding(horizontal=20.dp).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(top=22.dp), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically){
            Column {
                Text("Home",color=CloudNavy,fontSize=34.sp,fontWeight=FontWeight.Bold)
                Text("People you can discover",color=CloudNavySoft,fontSize=15.sp,fontWeight=FontWeight.Medium)
            }
            Row(verticalAlignment=Alignment.CenterVertically){
                BadgedBox(badge={ if(notifications.items.collectAsState().value.any{it.readAt==null}) Badge() }){
                    IconButton(onClick=onOpenNotifications){Icon(Icons.Outlined.Notifications,"Notifications",tint=CloudNavy)}
                }
                TextButton(onClick=onSignOut){Text("Sign out",color=CloudNavy)}
            }
        }
        Spacer(Modifier.height(14.dp))
        if(loading){
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator(color=CloudNavy)}
        } else if(profiles.isEmpty()){
            Text("No public profiles are available yet.",color=CloudNavy,fontSize=16.sp,modifier=Modifier.padding(top=30.dp))
        } else {
            LazyColumn(contentPadding=PaddingValues(bottom=120.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                items(profiles,key={it.profileId}){ profile ->
                    Card(onClick={onOpenProfile(profile.profileId)},colors=CardDefaults.cardColors(containerColor=Color.White), elevation=CardDefaults.cardElevation(defaultElevation=2.dp),modifier=Modifier.fillMaxWidth()){
                        Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){
                            Column(Modifier.weight(1f)){
                                Row(verticalAlignment=Alignment.CenterVertically){
                                    Text(profile.displayName,color=CloudNavy,fontSize=19.sp,fontWeight=FontWeight.SemiBold)
                                    if(profile.verified){Spacer(Modifier.width(5.dp));Icon(Icons.Outlined.Verified,"Verified",tint=Color(0xFF1E66D0),modifier=Modifier.size(18.dp))}
                                }
                                Text("${profile.age} • ${profile.city}, ${profile.country.orEmpty()}",color=CloudNavy,fontSize=14.sp)
                                profile.profession?.takeIf{it.isNotBlank()}?.let{Text(it,color=Color.Black,fontSize=13.sp)}
                                profile.skills?.takeIf{it.isNotBlank()}?.let{Text(it,color=Color.Black,fontSize=12.sp,maxLines=1)}
                            }
                            Text("View", color = AppPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}