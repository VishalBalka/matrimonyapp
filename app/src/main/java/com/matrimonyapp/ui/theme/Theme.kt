package com.matrimonyapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppLightColors = lightColorScheme(
    primary = AppPrimary,
    onPrimary = Color.White,

    secondary = Color(0xFFB34C70),
    onSecondary = Color.White,

    tertiary = Color(0xFF52637A),
    onTertiary = Color.White,

    background = AppBackground,
    onBackground = AppTextPrimary,

    surface = Color.White,
    onSurface = AppTextPrimary,

    surfaceVariant = Color(0xFFF4F6F8),
    surfaceContainer = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainerHigh = Color.White,
    onSurfaceVariant = AppTextSecondary,

    inverseSurface = AppTextPrimary,
    inverseOnSurface = Color.White,

    error = Color(0xFFB3261E),
    onError = Color.White
)

private val AppTypography = Typography(
    bodyLarge = androidx.compose.material3.Typography().bodyLarge.copy(
        color = Color.Black.copy(alpha = 1f),
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
    ),
    bodyMedium = androidx.compose.material3.Typography().bodyMedium.copy(
        color = Color.Black.copy(alpha = 1f),
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
    ),
    bodySmall = androidx.compose.material3.Typography().bodySmall.copy(
        color = Color.Black.copy(alpha = 1f)
    ),
    titleLarge = androidx.compose.material3.Typography().titleLarge.copy(
        color = Color.Black.copy(alpha = 1f)
    ),
    titleMedium = androidx.compose.material3.Typography().titleMedium.copy(
        color = Color.Black.copy(alpha = 1f)
    ),
    titleSmall = androidx.compose.material3.Typography().titleSmall.copy(
        color = Color.Black.copy(alpha = 1f)
    ),
    headlineLarge = androidx.compose.material3.Typography().headlineLarge.copy(
        color = Color.Black.copy(alpha = 1f)
    ),
    headlineMedium = androidx.compose.material3.Typography().headlineMedium.copy(
        color = Color.Black.copy(alpha = 1f)
    ),
    headlineSmall = androidx.compose.material3.Typography().headlineSmall.copy(
        color = Color.Black.copy(alpha = 1f)
    ),
    labelLarge = androidx.compose.material3.Typography().labelLarge.copy(
        color = Color.Unspecified,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
    ),
    labelMedium = androidx.compose.material3.Typography().labelMedium.copy(
        color = Color.Unspecified
    ),
    labelSmall = androidx.compose.material3.Typography().labelSmall.copy(
        color = Color.Unspecified
    )
)

@Composable
fun MatrimonyAppTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppLightColors,
        typography = AppTypography,
        content = content
    )
}