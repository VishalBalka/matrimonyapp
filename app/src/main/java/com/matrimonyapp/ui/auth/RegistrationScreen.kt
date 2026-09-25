package com.matrimonyapp.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matrimonyapp.core.result.AppResult
import com.matrimonyapp.data.repository.AuthRepository
import kotlinx.coroutines.launch

// Every value here is a literal, self-contained color, and every Text() passes a fully-built
// `style` instead of loose color/fontSize params merged with a MaterialTheme.typography style or
// an ambient LocalTextStyle. This mirrors LoginScreen.kt: both screens were reported to render
// text invisibly, so both now resolve every text color the same explicit, unambiguous way.
private val FieldBackground = Color.White
private val PrimaryWine = Color(0xFF9E1B4D)
private val ErrorRed = Color(0xFFB3261E)

private val HeadingStyle = TextStyle(
    color = Color.Black,
    fontSize = 32.sp,
    fontWeight = FontWeight.SemiBold
)
private val FieldLabelStyle = TextStyle(color = Color.Black)
private val ErrorFieldStyle = TextStyle(color = ErrorRed, fontSize = 12.sp)
private val ErrorMessageStyle = TextStyle(color = ErrorRed, fontSize = 14.sp)
private val ButtonTextStyle = TextStyle(color = Color.White)
private val OutlinedButtonTextStyle = TextStyle(color = Color.Black)

@Composable
private fun registrationFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color(0xFF141E28),
    unfocusedTextColor = Color(0xFF141E28),
    disabledTextColor = Color(0xFF718096),

    focusedLabelColor = PrimaryWine,
    unfocusedLabelColor = Color(0xFF4A5568),
    disabledLabelColor = Color(0xFF718096),

    cursorColor = PrimaryWine,

    focusedBorderColor = PrimaryWine,
    unfocusedBorderColor = Color(0xFFCBD5E1),
    disabledBorderColor = Color(0xFFE2E8F0),

    focusedContainerColor = FieldBackground,
    unfocusedContainerColor = FieldBackground,
    disabledContainerColor = FieldBackground,

    errorTextColor = ErrorRed,
    errorLabelColor = ErrorRed,
    errorCursorColor = ErrorRed,
    errorBorderColor = ErrorRed,
    errorContainerColor = FieldBackground
)

@Composable
fun RegistrationScreen(
    repository: AuthRepository,
    onRegistered: () -> Unit,
    onSignIn: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var errors by remember {
        mutableStateOf(emptyList<String>())
    }

    var serverError by remember {
        mutableStateOf<String?>(null)
    }

    var loading by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()

    fun submit() {
        val result = validateRegistration(
            displayName,
            email,
            password,
            confirmPassword
        )

        errors = result.errors
        serverError = null

        if (!result.isValid) {
            return
        }

        loading = true

        scope.launch {
            when (
                val outcome = repository.register(
                    displayName = displayName,
                    email = email,
                    password = password,
                    confirmPassword = confirmPassword
                )
            ) {
                is AppResult.Success -> {
                    loading = false
                    onRegistered()
                }

                is AppResult.Failure -> {
                    loading = false
                    serverError = outcome.error.message
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // See LoginScreen.kt for the full explanation: clip() creates an implicit hardware
        // layer that, as a sibling of the OutlinedTextFields inside the same scrollable
        // Column, could go stale relative to a focused field's own invalidate() call on this
        // engine version. Surface is Material3's tested mechanism for this exact container
        // shape; imePadding() moved from the ancestor Box onto this Surface's own chain.
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 28.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.Top
        ) {

            Text(
                text = "Create your account",
                style = HeadingStyle
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            OutlinedTextField(
                value = displayName,
                onValueChange = {
                    displayName = it
                    errors = emptyList()
                    serverError = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(
                        text = "Full name",
                        style = FieldLabelStyle
                    )
                },
                singleLine = true,
                textStyle = TextStyle(color = Color.Black),
                isError = errors.any {
                    it.contains("Full name")
                },
                colors = registrationFieldColors()
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errors = emptyList()
                    serverError = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(
                        text = "Email",
                        style = FieldLabelStyle
                    )
                },
                singleLine = true,
                textStyle = TextStyle(color = Color.Black),
                isError = errors.any {
                    it.contains("Email") ||
                            it.contains("valid email")
                },
                colors = registrationFieldColors()
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errors = emptyList()
                    serverError = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(
                        text = "Password",
                        style = FieldLabelStyle
                    )
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                textStyle = TextStyle(color = Color.Black),
                isError = errors.any {
                    it.contains("Password")
                },
                colors = registrationFieldColors()
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    errors = emptyList()
                    serverError = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(
                        text = "Confirm password",
                        style = FieldLabelStyle
                    )
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                isError = errors.any {
                    it.contains("match")
                },
                textStyle = TextStyle(color = Color.Black),
                colors = registrationFieldColors()
            )

            errors.forEach { error ->
                Text(
                    text = error,
                    style = ErrorFieldStyle
                )
            }

            serverError?.let { error ->

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = error,
                    style = ErrorMessageStyle
                )
            }

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            Button(
                onClick = ::submit,
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryWine,
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFD7C5CC),
                    disabledContentColor = Color.White
                )
            ) {

                if (loading) {

                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )

                } else {

                    Text(
                        text = "Create Account",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            OutlinedButton(
                onClick = onSignIn,
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = PrimaryWine,
                    disabledContainerColor = Color.White,
                    disabledContentColor = Color.Gray
                ),
                border = BorderStroke(1.5.dp, PrimaryWine)
            ) {

                Text(
                    text = "Already have an account? Sign In",
                    color = PrimaryWine,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            OutlinedButton(
                onClick = onBack,
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF4A5568),
                    disabledContainerColor = Color.White,
                    disabledContentColor = Color.Gray
                ),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {

                Text(
                    text = "Back",
                    color = Color(0xFF4A5568),
                    fontSize = 15.sp
                )
            }
            }
        }
    }
}
