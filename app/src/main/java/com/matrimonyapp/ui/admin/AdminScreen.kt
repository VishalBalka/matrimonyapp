package com.matrimonyapp.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.matrimonyapp.core.result.AppResult
import com.matrimonyapp.data.remote.AdminApi
import com.matrimonyapp.data.remote.AdminDashboardDto
import com.matrimonyapp.data.remote.AdminReportDto
import kotlinx.coroutines.launch

@Composable
fun AdminScreen(api: AdminApi,onBack:()->Unit,modifier:Modifier=Modifier){
    var dashboard by remember{mutableStateOf<AdminDashboardDto?>(null)}
    var reports by remember{mutableStateOf<List<AdminReportDto>>(emptyList())}
    var error by remember{mutableStateOf<String?>(null)}
    val scope=rememberCoroutineScope()
    LaunchedEffect(Unit){
        when(val r=runCatching{api.dashboard()}.getOrNull()){null->error="Unable to reach admin service.";else->if(r.isSuccessful)dashboard=r.body() else error="Admin access denied."}
        runCatching{api.reports(null)}.getOrNull()?.takeIf{it.isSuccessful}?.body()?.let{reports=it}
    }
    Column(modifier.fillMaxSize().padding(20.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
            Text("Admin Dashboard",style=MaterialTheme.typography.headlineMedium,color=Color(0xFF102A56))
            TextButton(onClick=onBack){Text("Back",color=Color(0xFF102A56))}
        }
        error?.let{Text(it,color=Color(0xFFB3261E))}
        dashboard?.let{
            Text("Users: ${it.users}",color=Color(0xFF102A56))
            Text("Profiles: ${it.profiles}",color=Color(0xFF102A56))
            Text("Open reports: ${it.openReports}",color=Color(0xFF102A56))
            Text("Pending verification: ${it.pendingVerification}",color=Color(0xFF102A56))
        }
        Spacer(Modifier.height(16.dp))
        Text("Reports",style=MaterialTheme.typography.titleLarge,color=Color(0xFF102A56))
        LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){
            items(reports,key={it.id}){report->
                Card(colors=CardDefaults.cardColors(containerColor=Color(0xF5FFFFFF)),modifier=Modifier.fillMaxWidth()){
                    Column(Modifier.padding(12.dp)){
                        Text(report.reason,color=Color(0xFF102A56),style=MaterialTheme.typography.titleMedium)
                        report.details?.let{Text(it,color=Color(0xFF1F3553))}
                        Text("Status: ${report.status}",color=Color(0xFF1F3553))
                        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                            TextButton(onClick={scope.launch{api.reportStatus(report.id,"REVIEWING")}}){Text("Review")}
                            TextButton(onClick={scope.launch{api.reportStatus(report.id,"RESOLVED")}}){Text("Resolve")}
                        }
                    }
                }
            }
        }
    }
}