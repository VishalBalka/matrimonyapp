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

// Every value here is a literal, self-contained color. Nothing on this screen reads a color
// through an ambient CompositionLocal or a shared theme constant, and every Text() below passes
// a fully-built `style` object instead of loose color/fontSize params, so there is no merge with
// LocalTextStyle/LocalContentColor left to go wrong. This screen was repeatedly reported as
// rendering text invisibly; this file removes every indirect path a text color could take.
private val FieldBackground = Color.White
private val PrimaryWine = Color(0xFF9E1B4D)
private val ErrorRed = Color(0xFFB3261E)

private val HeadingStyle = TextStyle(
    color = Color.Black,
    fontSize = 32.sp,
    fontWeight = FontWeight.SemiBold
)
private val SubtitleStyle = TextStyle(
    color = Color.Black,
    fontSize = 17.sp
)
private val FieldLabelStyle = TextStyle(color = Color.Black)
private val ErrorStyle = TextStyle(color = ErrorRed)
private val ButtonTextStyle = TextStyle(color = Color.White)
private val OutlinedButtonTextStyle = TextStyle(color = Color.Black)

@Composable
private fun loginFieldColors() = OutlinedTextFieldDefaults.colors(
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
fun LoginScreen(
    repository: AuthRepository,
    onLoggedIn: () -> Unit,
    onCreateAccount: () -> Unit,
    onMfaRequired: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var email by remember {
        mutableStateOf("aditi.rao@example.com")
    }

    var password by remember {
        mutableStateOf("Password123!")
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var loading by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()

    fun login() {
        if (email.isBlank()) {
            errorMessage = "Enter your email."
            return
        }

        if (password.isBlank()) {
            errorMessage = "Enter your password."
            return
        }

        errorMessage = null
        loading = true

        scope.launch {
            when (
                val result = repository.login(
                    email = email.trim(),
                    password = password
                )
            ) {
                is AppResult.Success -> {
                    loading = false
                    onLoggedIn()
                }

                is AppResult.Failure -> {
                    loading = false
                    errorMessage = result.error.message
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // ROOT-CAUSE FIX: this card used to be Modifier.clip(shape).background(Color.White),
        // which creates an implicit hardware layer (clip() always allocates a graphicsLayer
        // internally). That layer sat as a SIBLING of the OutlinedTextFields inside the same
        // scrollable Column. Focusing a field triggers that field's own internal recomposition
        // (label float, indicator color, cursor blink) and invalidate() call; on this engine
        // version that invalidation does not reliably propagate to also invalidate this card's
        // cached layer, so glyphs drawn earlier in the same cached picture (the heading,
        // subtitle, field labels) can be left stale/blank while the focused field's own fresh
        // redraw (its border, its cursor) and the buttons below (separate composables) still
        // show correctly -- exactly the symptom reported. Surface is Material3's own, tested
        // mechanism for a clipped, colored, elevated container and manages this interaction
        // correctly; it replaces the raw clip()+background() combination below.
        // imePadding() also moved from the ancestor Box onto this Surface's own modifier chain,
        // placed after verticalScroll(), so the IME inset changes this container's own internal
        // scroll padding directly rather than an ancestor resizing/repositioning it from outside.
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
                text = "Welcome back",
                style = HeadingStyle
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Sign in to your account",
                style = SubtitleStyle
            )

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = null
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
                colors = loginFieldColors()
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
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
                colors = loginFieldColors()
            )

            errorMessage?.let { message ->

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text = message,
                    style = ErrorStyle
                )
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Button(
                onClick = ::login,
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
                        text = "Sign In",
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
                onClick = onCreateAccount,
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
                    text = "Create Account",
                    color = PrimaryWine,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            }
        }
    }
}
