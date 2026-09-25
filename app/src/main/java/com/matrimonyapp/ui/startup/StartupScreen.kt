package com.matrimonyapp.ui.startup

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matrimonyapp.ui.brand.BrandEmblem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun Modifier.platformBlur(radius: androidx.compose.ui.unit.Dp): Modifier =
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        this.blur(radius)
    } else {
        this
    }

// iPhone Pro Titanium & Liquid Glass Palette
private val GlassDeepBackground = Color(0xFF0F040A)
private val GlassSurfaceBase = Color(0x33280B19)
private val GlassSurfaceHighlight = Color(0x66FFFFFF)
private val GlassSpecularBorder = Color(0x80FFFFFF)
private val GlassShadowBorder = Color(0x1AFFFFFF)
private val ChampagneGold = Color(0xFFE8C98B)
private val RoseGold = Color(0xFFC77B95)
private val DeepVelvetWine = Color(0xFF3A1024)

@Composable
fun StartupScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val animatorScale = remember {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
        }.getOrDefault(1f)
    }

    // Animation states
    val backgroundScale = remember { Animatable(1.15f) }
    val glassCardScale = remember { Animatable(0.72f) }
    val glassCardAlpha = remember { Animatable(0f) }
    val emblemScale = remember { Animatable(0.85f) }
    val emblemAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val textSlide = remember { Animatable(24f) }
    val lightSweep = remember { Animatable(-1f) }
    val exitAlpha = remember { Animatable(1f) }

    // Infinite breathing ambient caustics (iPhone 17 Pro chromatic liquid glass effect)
    val infiniteTransition = rememberInfiniteTransition(label = "LiquidGlassAmbient")
    val ambientPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambientPulse"
    )
    val ambientRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambientRotation"
    )

    LaunchedEffect(animatorScale) {
        if (startupDurationMillis(animatorScale) == 0L) {
            onFinished()
            return@LaunchedEffect
        }

        // Orchestrated entrance
        launch {
            glassCardAlpha.animateTo(1f, tween((380 * animatorScale).toInt().coerceAtLeast(1)))
        }
        launch {
            glassCardScale.animateTo(
                1f,
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            backgroundScale.animateTo(
                1f,
                tween((1400 * animatorScale).toInt().coerceAtLeast(1), easing = FastOutSlowInEasing)
            )
        }

        delay((240 * animatorScale).toLong().coerceAtLeast(1L))
        launch {
            emblemAlpha.animateTo(1f, tween((350 * animatorScale).toInt().coerceAtLeast(1)))
            emblemScale.animateTo(
                1f,
                spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        // Chromatic light sweep across glass surface
        launch {
            delay((200 * animatorScale).toLong().coerceAtLeast(1L))
            lightSweep.animateTo(
                1.5f,
                tween((750 * animatorScale).toInt().coerceAtLeast(1), easing = FastOutSlowInEasing)
            )
        }

        delay((280 * animatorScale).toLong().coerceAtLeast(1L))
        launch {
            textAlpha.animateTo(1f, tween((400 * animatorScale).toInt().coerceAtLeast(1)))
            textSlide.animateTo(
                0f,
                spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }

        // Hold for perception, then gracefully dissolve
        delay((700 * animatorScale).toLong().coerceAtLeast(1L))
        exitAlpha.animateTo(0f, tween((260 * animatorScale).toInt().coerceAtLeast(1), easing = FastOutSlowInEasing))
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(exitAlpha.value)
            .background(GlassDeepBackground),
        contentAlignment = Alignment.Center
    ) {
        // 1. Ambient Background Liquid Glow Orbs (Diffused behind dark titanium glass)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(backgroundScale.value)
        ) {
            // Ruby-Wine Caustic Orb
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = (-60).dp)
                    .scale(ambientPulse)
                    .rotate(ambientRotation)
                    .platformBlur(72.dp)
                    .background(
                        Brush.radialGradient(
                            0.0f to DeepVelvetWine.copy(alpha = 0.65f),
                            0.35f to DeepVelvetWine.copy(alpha = 0.35f),
                            0.65f to RoseGold.copy(alpha = 0.15f),
                            0.85f to RoseGold.copy(alpha = 0.04f),
                            1.0f to Color.Transparent
                        ),
                        CircleShape
                    )
            )

            // Champagne Warm Specular Orb
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .align(Alignment.Center)
                    .offset(x = 60.dp, y = 40.dp)
                    .scale(2f - ambientPulse)
                    .rotate(-ambientRotation)
                    .platformBlur(68.dp)
                    .background(
                        Brush.radialGradient(
                            0.0f to ChampagneGold.copy(alpha = 0.45f),
                            0.35f to ChampagneGold.copy(alpha = 0.22f),
                            0.65f to RoseGold.copy(alpha = 0.10f),
                            0.85f to RoseGold.copy(alpha = 0.03f),
                            1.0f to Color.Transparent
                        ),
                        CircleShape
                    )
            )
        }

        // 2. Central iPhone Pro Glass Morphic Island
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Liquid Glass Emblem Container
            Box(
                modifier = Modifier
                    .size(168.dp)
                    .scale(glassCardScale.value)
                    .alpha(glassCardAlpha.value)
                    .clip(RoundedCornerShape(44.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                GlassSurfaceHighlight.copy(alpha = 0.18f),
                                GlassSurfaceBase.copy(alpha = 0.55f),
                                Color(0x8015050E)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                GlassSpecularBorder,
                                GlassSpecularBorder.copy(alpha = 0.3f),
                                GlassShadowBorder,
                                ChampagneGold.copy(alpha = 0.4f)
                            )
                        ),
                        shape = RoundedCornerShape(44.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Internal Caustic Refraction Glow
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .blur(20.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    RoseGold.copy(alpha = 0.35f),
                                    ChampagneGold.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )

                // The New Luxury Emblem with Spring Physics
                BrandEmblem(
                    modifier = Modifier
                        .scale(emblemScale.value)
                        .alpha(emblemAlpha.value),
                    size = 112.dp
                )

                // iPhone 17 Pro Specular Diagonal Glass Light Flare Sweep
                val sweepX = (lightSweep.value * 240).dp
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 240.dp)
                        .offset(x = sweepX)
                        .rotate(28f)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.35f),
                                    ChampagneGold.copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(34.dp))

            // 3. Typographic Branding with Glass Pill Effect
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .offset(y = textSlide.value.dp)
            ) {
                Text(
                    text = "MatrimonyApp",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.75.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Frosted Glass Subtitle Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x33FFFFFF))
                        .border(
                            width = 1.dp,
                            color = Color(0x33FFFFFF),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Meaningful connections begin with intention",
                            color = ChampagneGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.2.sp
                        )
                    }
                }
            }
        }
    }
}
