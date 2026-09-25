package com.matrimonyapp.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matrimonyapp.core.result.AppResult
import com.matrimonyapp.data.discovery.DiscoveryProfile
import com.matrimonyapp.data.discovery.ProfileSearchRepository
import com.matrimonyapp.data.discovery.isAuthenticationFailure
import com.matrimonyapp.data.security.SecurityRepository
import com.matrimonyapp.ui.theme.CloudNavy
import com.matrimonyapp.ui.theme.CloudNavySoft
import kotlinx.coroutines.launch

/**
 * Discovery-safe profile detail. Shows photo, name, age and location only.
 * It never displays a phone number, and it does not show internal identifiers.
 */
@Composable
fun ProfileDetailScreen(
    profileId: String,
    repository: ProfileSearchRepository,
    securityRepository: SecurityRepository,
    photoCache: PhotoCache,
    onBack: () -> Unit,
    onUnauthorized: () -> Unit,
    modifier: Modifier = Modifier
) {
    var profile by remember(profileId) { mutableStateOf<DiscoveryProfile?>(null) }
    var loading by remember(profileId) { mutableStateOf(true) }
    var error by remember(profileId) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        loading = true
        error = null
        scope.launch {
            when (val result = repository.getProfile(profileId)) {
                is AppResult.Success -> {
                    profile = result.value
                    loading = false
                }
                is AppResult.Failure -> {
                    if (result.error.isAuthenticationFailure()) {
                        onUnauthorized()
                    } else {
                        error = result.error.message
                        loading = false
                    }
                }
            }
        }
    }

    LaunchedEffect(profileId) { load() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TextButton(onClick = onBack) { Text("Back to results", color = Color.Black, fontSize = 16.sp) }

        val current = profile
        when {
            loading -> Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.Black)
            }

            error != null -> GlassCard {
                Text("Profile unavailable", color = CloudNavy, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(error.orEmpty(), color = Color(0xFFB3261E), fontSize = 14.sp)
                Button(onClick = { load() }) { Text("Retry") }
            }

            current != null -> GlassCard {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ProfilePhotoCircle(
                        profileId = current.profileId,
                        photoAvailable = current.photoAvailable,
                        size = 160.dp,
                        cache = photoCache,
                        repository = repository,
                        onUnauthorized = onUnauthorized
                    )
                }
                Text(current.displayName, color = Color.Black, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                DetailLine(Icons.Outlined.CalendarMonth, "Age", "${current.age} years")
                DetailLine(Icons.Outlined.LocationOn, "Country", current.country ?: "Not shared")
                DetailLine(Icons.Outlined.LocationOn, "State / Province", current.stateProvince ?: "Not shared")
                DetailLine(Icons.Outlined.LocationOn, "City", current.city ?: "Not shared")
                current.profession?.takeIf{it.isNotBlank()}?.let{DetailLine(Icons.Outlined.Work,"Profession",it)}
                current.skills?.takeIf{it.isNotBlank()}?.let{DetailLine(Icons.Outlined.Build,"Skills",it)}
                current.salaryRange?.takeIf{it.isNotBlank()}?.let{DetailLine(Icons.Outlined.Payments,"Salary range",it)}
                if(current.verified) Text("✓ Verified profile",color=Color(0xFF1E66D0),fontWeight=FontWeight.SemiBold)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    var actionMessage by remember(current.profileId){mutableStateOf<String?>(null)}
                    var reportOpen by remember(current.profileId){mutableStateOf(false)}
                    TextButton(onClick={scope.launch{when(val r=securityRepository.block(current.profileId)){is AppResult.Success->actionMessage=r.value;is AppResult.Failure->actionMessage=r.error.message}}}){Text("Block",color=CloudNavy)}
                    TextButton(onClick={reportOpen=true}){Text("Report",color=Color(0xFFB3261E))}
                    actionMessage?.let{Text(it,color=Color.Black,fontSize=12.sp)}
                    if(reportOpen){
                        AlertDialog(onDismissRequest={reportOpen=false},title={Text("Report profile")},text={Text("Report this profile for unsafe, abusive, fraudulent, or inappropriate behavior.")},
                            confirmButton={TextButton(onClick={scope.launch{when(val r=securityRepository.report(current.profileId,"Safety concern","Reported from profile detail.")){is AppResult.Success->actionMessage=r.value;is AppResult.Failure->actionMessage=r.error.message};reportOpen=false}}){Text("Submit")}},
                            dismissButton={TextButton(onClick={reportOpen=false}){Text("Cancel")}})
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color.Black)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, color = Color.Black, fontSize = 12.sp)
            Text(value, color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}
