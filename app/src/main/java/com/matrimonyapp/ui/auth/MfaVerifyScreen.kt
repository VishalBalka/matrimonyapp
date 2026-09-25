package com.matrimonyapp.ui.auth
import com.matrimonyapp.ui.theme.AppTextPrimary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.matrimonyapp.core.result.AppResult
import com.matrimonyapp.data.repository.AuthRepository
import kotlinx.coroutines.launch
import com.matrimonyapp.ui.theme.AppPrimary
import com.matrimonyapp.ui.theme.appOutlinedTextFieldColors

@Composable
fun MfaVerifyScreen(
    repository: AuthRepository,
    challengeId: String,
    onAuthenticated: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center) {
        Text("Two-step verification", style = MaterialTheme.typography.headlineLarge, color = AppTextPrimary)
        Spacer(Modifier.height(8.dp))
        Text("Enter the 6-digit code from your authenticator app.", color = AppTextPrimary)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = code,
            onValueChange = { if (it.all(Char::isDigit) && it.length <= 6) { code = it; error = null } },
            label = { Text("MFA code") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = appOutlinedTextFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = Color(0xFFB3261E)) }
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = {
                if (code.length != 6) { error = "Enter the 6-digit code."; return@Button }
                loading = true
                scope.launch {
                    when (val result = repository.verifyMfa(challengeId, code)) {
                        is AppResult.Success -> onAuthenticated()
                        is AppResult.Failure -> { loading = false; error = result.error.message }
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppPrimary,
                contentColor = Color.White
            )
        ) {
            Text(
                text = if (loading) "Verifying..." else "Verify",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onBack,
            enabled = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = AppPrimary
            ),
            border = BorderStroke(1.5.dp, AppPrimary)
        ) {
            Text("Back", color = AppPrimary, fontWeight = FontWeight.SemiBold)
        }
    }
}
