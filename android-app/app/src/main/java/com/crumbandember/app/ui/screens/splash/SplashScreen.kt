package com.crumbandember.app.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crumbandember.app.ui.theme.EspressoDeep
import kotlinx.coroutines.delay

/**
 * Splash screen matching the visual design:
 * Rich espresso background, Crumb & Ember artisan typography, wheat sheaf icon,
 * warm ambient glow with artisan loaf art, and tagline: "Real Ingredients. Better Mornings."
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val scale = remember { Animatable(0.85f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(700))
        scale.animateTo(1f, animationSpec = tween(800))
        delay(1400)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1B0E07),
                        EspressoDeep,
                        Color(0xFF2C160A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(32.dp)
                .scale(scale.value)
        ) {
            // Wheat mark with radial warm ambient glow
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFFD9752B).copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B2417)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🌾", fontSize = 38.sp)
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Crumb & Ember",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFFFFF8EE),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "ARTISAN BAKERY",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFEFC988),
                letterSpacing = 3.sp
            )

            Spacer(Modifier.height(36.dp))

            // Decorative artisan loaf illustration box
            Box(
                modifier = Modifier
                    .size(width = 220.dp, height = 130.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF4A2A18),
                                Color(0xFF2A150B)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🥖", fontSize = 48.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "SLOW FERMENTED",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFEFC988).copy(alpha = 0.8f),
                        letterSpacing = 1.5.sp
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            Text(
                text = "Real Ingredients. Better Mornings.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFD8C6AE),
                textAlign = TextAlign.Center
            )
        }
    }
}
