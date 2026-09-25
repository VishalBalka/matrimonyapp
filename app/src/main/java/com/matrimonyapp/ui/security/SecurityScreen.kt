package com.matrimonyapp.ui.security

import android.app.KeyguardManager
import android.content.Context
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.matrimonyapp.core.result.AppResult
import com.matrimonyapp.core.security.AppLockStore
import com.matrimonyapp.data.remote.PrivacyDto
import com.matrimonyapp.data.security.SecurityRepository
import kotlinx.coroutines.launch
import com.matrimonyapp.ui.theme.appOutlinedTextFieldColors

@Composable
fun SecurityScreen(
    repository: SecurityRepository,
    appLockStore: AppLockStore,
    onBack:()->Unit,
    modifier:Modifier=Modifier
){
    var state by remember { mutableStateOf<com.matrimonyapp.data.remote.SecurityStateDto?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var mfaSecret by remember { mutableStateOf<String?>(null) }
    var mfaCode by remember { mutableStateOf("") }
    var lockEnabled by remember { mutableStateOf(appLockStore.isEnabled()) }
    var loading by remember { mutableStateOf(true) }
    val scope=rememberCoroutineScope()
    val context=LocalContext.current
    LaunchedEffect(Unit){ when(val r=repository.state()){is AppResult.Success->{state=r.value;loading=false};is AppResult.Failure->{error=r.error.message;loading=false}}}

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(20.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
            Text("Security & Privacy",style=MaterialTheme.typography.headlineMedium,color=Color(0xFF102A56))
            TextButton(onClick=onBack){Text("Back",color=Color(0xFF102A56))}
        }
        if(loading){CircularProgressIndicator(color=Color(0xFF102A56));return@Column}
        error?.let{Text(it,color=Color(0xFFB3261E))}
        message?.let{Text(it,color=Color(0xFF102A56))}
        Spacer(Modifier.height(8.dp))

        Text("App lock",style=MaterialTheme.typography.titleLarge,color=Color(0xFF102A56))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
            Text("Require the device screen lock when opening the app.",modifier=Modifier.weight(1f),color=Color(0xFF1F3553))
            Switch(checked=lockEnabled,onCheckedChange={enabled->
                val km=context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                if(enabled && !km.isDeviceSecure){
                    error="Set a device screen lock before enabling app lock."
                } else {
                    appLockStore.setEnabled(enabled)
                    lockEnabled=enabled
                }
            })
        }

        Spacer(Modifier.height(18.dp))
        Text("Multi-factor authentication",style=MaterialTheme.typography.titleLarge,color=Color(0xFF102A56))
        Text(if(state?.mfaEnabled==true)"MFA is enabled." else "Use an authenticator app for a second login factor.",color=Color(0xFF1F3553))
        Spacer(Modifier.height(8.dp))
        if(state?.mfaEnabled != true){
            Button(onClick={scope.launch{when(val r=repository.setupMfa()){is AppResult.Success->{mfaSecret=r.value.secret;message=r.value.message};is AppResult.Failure->error=r.error.message}}}){Text("Set up MFA")}
            mfaSecret?.let{
                Text("Secret: $it",color=Color(0xFF102A56),modifier=Modifier.padding(top=8.dp))
                Text("Add this secret to an authenticator app.",color=Color(0xFF1F3553))
                OutlinedTextField(value=mfaCode,onValueChange={mfaCode=it.take(6)},label={Text("6-digit code")},colors=appOutlinedTextFieldColors(),modifier=Modifier.fillMaxWidth())
                Button(onClick={scope.launch{when(val r=repository.enableMfa(mfaCode)){is AppResult.Success->{message=r.value;state=state?.copy(mfaEnabled=true);mfaSecret=null};is AppResult.Failure->error=r.error.message}}}){Text("Enable MFA")}
            }
        } else {
            OutlinedTextField(value=mfaCode,onValueChange={mfaCode=it.take(6)},label={Text("Current MFA code")},colors=appOutlinedTextFieldColors(),modifier=Modifier.fillMaxWidth())
            TextButton(onClick={scope.launch{when(val r=repository.disableMfa(mfaCode)){is AppResult.Success->{message=r.value;state=state?.copy(mfaEnabled=false);mfaCode=""};is AppResult.Failure->error=r.error.message}}}){Text("Disable MFA")}
        }

        Spacer(Modifier.height(18.dp))
        Text("Profile privacy",style=MaterialTheme.typography.titleLarge,color=Color(0xFF102A56))
        var visibility by remember(state?.visibility){mutableStateOf(state?.visibility ?: "PUBLIC")}
        var showPhone by remember(state?.showPhone){mutableStateOf(state?.showPhone ?: false)}
        var showSalary by remember(state?.showSalary){mutableStateOf(state?.showSalary ?: false)}
        var showSocial by remember(state?.showSocial){mutableStateOf(state?.showSocial ?: true)}
        var profileLocked by remember(state?.profileLocked){mutableStateOf(state?.profileLocked ?: false)}
        Text("Visibility: $visibility",color=Color(0xFF1F3553))
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
            listOf("PUBLIC","PRIVATE","LOCKED").forEach{TextButton(onClick={visibility=it}){Text(it,color=Color(0xFF102A56))}}
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Show phone",color=Color(0xFF1F3553));Switch(showPhone,{showPhone=it})}
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Show salary",color=Color(0xFF1F3553));Switch(showSalary,{showSalary=it})}
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Show social links",color=Color(0xFF1F3553));Switch(showSocial,{showSocial=it})}
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Lock profile",color=Color(0xFF1F3553));Switch(profileLocked,{profileLocked=it})}
        Button(onClick={scope.launch{when(val r=repository.privacy(PrivacyDto(visibility,profileLocked,showPhone,showSalary,showSocial))){is AppResult.Success->{message=r.value;state=state?.copy(visibility=visibility,profileLocked=profileLocked,showPhone=showPhone,showSalary=showSalary,showSocial=showSocial)};is AppResult.Failure->error=r.error.message}}}){Text("Save privacy settings")}
        Spacer(Modifier.height(18.dp))
        Text("Safety",style=MaterialTheme.typography.titleLarge,color=Color(0xFF102A56))
        Text("Never share passwords, OTP codes, bank credentials, or identity documents with another member. Report suspicious requests.",color=Color(0xFF1F3553))
    }
}
