package com.matrimonyapp

import android.app.KeyguardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.matrimonyapp.data.session.AuthState
import com.matrimonyapp.ui.auth.AuthenticatedScreen
import com.matrimonyapp.ui.auth.LoginScreen
import com.matrimonyapp.ui.auth.MfaVerifyScreen
import com.matrimonyapp.ui.auth.RegistrationScreen
import com.matrimonyapp.ui.security.AppLockScreen
import com.matrimonyapp.ui.startup.StartupScreen
import com.matrimonyapp.ui.theme.CloudGlassBackground
import com.matrimonyapp.ui.theme.MatrimonyAppTheme
import com.matrimonyapp.ui.welcome.WelcomeScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val application = application as MatrimonyApplication
        val repository = application.authRepository

        setContent {
            val authState by repository.authState.collectAsState()

            var screenKey by remember {
                mutableStateOf("startup")
            }

            var appLocked by remember {
                mutableStateOf(false)
            }

            var notice by remember {
                mutableStateOf<String?>(null)
            }

            var mfaChallenge by remember {
                mutableStateOf<String?>(null)
            }

            val keyguard =
                getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

            val unlockLauncher =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == RESULT_OK) {
                        appLocked = false
                    }
                }

            LaunchedEffect(Unit) {
                if (
                    application.appLockStore.isEnabled() &&
                    keyguard.isDeviceSecure
                ) {
                    appLocked = true
                }
            }

            LaunchedEffect(Unit) {
                repository.restoreSession()
            }

            val authenticatedUser =
                (authState as? AuthState.Authenticated)?.user

            MatrimonyAppTheme(darkTheme = false) {

                if (appLocked) {

                    CloudGlassBackground {
                        AppLockScreen(
                            onUnlock = {
                                if (keyguard.isDeviceSecure) {
                                    unlockLauncher.launch(
                                        keyguard.createConfirmDeviceCredentialIntent(
                                            "Unlock MatrimonyApp",
                                            "Verify your device lock to continue."
                                        )
                                    )
                                } else {
                                    appLocked = false
                                }
                            }
                        )
                    }

                } else {

                    when (screenKey) {

                        "startup" -> {

                            StartupScreen(
                                onFinished = {
                                    screenKey =
                                        if (authenticatedUser != null) {
                                            "authenticated"
                                        } else {
                                            "welcome"
                                        }
                                }
                            )
                        }

                        "welcome" -> {

                            CloudGlassBackground {

                                WelcomeScreen(
                                    message = notice,

                                    onSignIn = {
                                        notice = null
                                        mfaChallenge = null
                                        screenKey = "login"
                                    },

                                    onCreateAccount = {
                                        notice = null
                                        screenKey = "registration"
                                    }
                                )
                            }
                        }

                        "login" -> {

                            CloudGlassBackground {

                                LoginScreen(
                                    repository = repository,

                                    onLoggedIn = {
                                        notice = null
                                        mfaChallenge = null
                                        screenKey = "authenticated"
                                    },

                                    onCreateAccount = {
                                        screenKey = "registration"
                                    },

                                    onMfaRequired = {
                                        /*
                                         * The current LoginScreen API does not
                                         * provide a challenge ID. MFA challenge
                                         * navigation therefore remains disabled
                                         * until the login repository/API exposes
                                         * the challenge through this callback.
                                         */
                                    }
                                )
                            }
                        }

                        "mfa" -> {

                            val challenge = mfaChallenge

                            if (challenge == null) {

                                screenKey = "login"

                            } else {

                                CloudGlassBackground {

                                    MfaVerifyScreen(
                                        repository = repository,
                                        challengeId = challenge,

                                        onAuthenticated = {
                                            mfaChallenge = null
                                            screenKey = "authenticated"
                                        },

                                        onBack = {
                                            mfaChallenge = null
                                            screenKey = "login"
                                        }
                                    )
                                }
                            }
                        }

                        "registration" -> {

                            CloudGlassBackground {

                                RegistrationScreen(
                                    repository = repository,

                                    onRegistered = {
                                        notice =
                                            "Account created. Sign in to continue."
                                        screenKey = "login"
                                    },

                                    onSignIn = {
                                        screenKey = "login"
                                    },

                                    onBack = {
                                        screenKey = "welcome"
                                    }
                                )
                            }
                        }

                        "authenticated" -> {

                            if (authenticatedUser == null) {

                                screenKey = "welcome"

                            } else {

                                AuthenticatedScreen(
                                    user = authenticatedUser,
                                    repository = repository,

                                    profileRepository =
                                        application.profileRepository,

                                    searchRepository =
                                        application.profileSearchRepository,

                                    securityRepository =
                                        application.securityRepository,

                                    notificationRepository =
                                        application.notificationRepository,

                                    appLockStore =
                                        application.appLockStore,

                                    adminApi =
                                        application.adminApi,

                                    onAuthenticationLost = {
                                        notice =
                                            "Your session has expired. Please sign in again."
                                        screenKey = "welcome"
                                    },

                                    onLoggedOut = { message ->
                                        notice = message
                                        screenKey = "welcome"
                                    }
                                )
                            }
                        }

                        else -> {
                            screenKey = "welcome"
                        }
                    }
                }
            }
        }
    }
}