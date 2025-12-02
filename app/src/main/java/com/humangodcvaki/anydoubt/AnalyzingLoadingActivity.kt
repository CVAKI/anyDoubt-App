package com.humangodcvaki.anydoubt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humangodcvaki.anydoubt.ui.theme.AnyDoubtTheme
import kotlinx.coroutines.delay

// Custom colors
private val RedPrimary = Color(0xFFF02828)
private val RedSecondary = Color(0xFFD67474)
private val RedLight = Color(0xFFFFE5E5)

class AnalyzingLoadingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AnyDoubtTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    AnalyzingLoadingScreen()
                }
            }
        }
    }
}

@Composable
fun AnalyzingLoadingScreen() {
    var rotation by remember { mutableStateOf(0f) }
    var isFastRotation by remember { mutableStateOf(true) }

    // Dynamic rotation with alternating speeds (kept from original)
    LaunchedEffect(Unit) {
        while (true) {
            val duration = if (isFastRotation) 800L else 2500L

            val startRotation = rotation
            val targetRotation = startRotation + 360f
            val startTime = System.currentTimeMillis()

            while (rotation < targetRotation) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / duration).coerceAtMost(1f)
                rotation = startRotation + (360f * progress)
                delay(16) // ~60 FPS
            }

            rotation = targetRotation % 360f
            isFastRotation = !isFastRotation
        }
    }

    // 1. Progress Animation for the Bar and Percentage Text (MODIFIED duration)
    val progressTransition = rememberInfiniteTransition(label = "progress")
    val progress by progressTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 15000, // Increased to 15 seconds to simulate a long process
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    // Pulsing animation for text (kept from original)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.Black,
                        Color(0xFF1A0000),
                        Color.Black
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Rotating Logo with dynamic speed
            Image(
                painter = painterResource(id = R.drawable.ndsilogo),
                contentDescription = "anyDoubt Logo",
                modifier = Modifier
                    .size(200.dp)
                    .rotate(rotation)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 2. Animated Text with Percentage
            Text(
                // Display the animated progress as an integer percentage
                text = "Analyzing... ${(progress * 100).toInt()}%",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = RedPrimary.copy(alpha = pulse.coerceIn(0f, 1f)),
                modifier = Modifier.padding(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle
            Text(
                text = "🔍 Processing your document",
                fontSize = 16.sp,
                color = RedSecondary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Please wait...",
                fontSize = 14.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Light
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 3. Loading indicator
            LinearProgressIndicator(
                // Use the animated progress value
                progress = { progress },
                color = RedPrimary,
                trackColor = RedLight.copy(alpha = 0.3f),
                modifier = Modifier
                    .width(200.dp)
                    .height(4.dp)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AnalyzingLoadingScreenPreview() {
    AnyDoubtTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            AnalyzingLoadingScreen()
        }
    }
}