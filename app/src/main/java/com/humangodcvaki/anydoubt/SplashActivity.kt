package com.humangodcvaki.anydoubt

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humangodcvaki.anydoubt.ui.theme.AnyDoubtTheme
import kotlinx.coroutines.delay

// Custom colors
private val RedBackground = Color(0xFFCE0000)
private val BlueText = Color(0xFF003EFF)
private val WhiteText = Color(0xFFFFFFFF)

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AnyDoubtTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = RedBackground
                ) {
                    SplashScreen {
                        // Navigate to Login Activity after animation
                        startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit = {}) {
    // Rotation animation with variable speed (fast-slow-fast)
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                0f at 0 with FastOutSlowInEasing // Fast start
                180f at 1000 with LinearEasing // Slow middle
                360f at 2000 with FastOutLinearInEasing // Fast end
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Navigate after 3 seconds
    LaunchedEffect(Unit) {
        delay(3500)
        onTimeout()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RedBackground)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo
        Image(
            painter = painterResource(id = R.drawable.transp),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(180.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // App Name - WHITE
        Text(
            text = "anyDoubt",
            fontSize = 36.sp,
            color = WhiteText,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(80.dp))

        // "Powered by" text
        Text(
            text = "Powered by",
            fontSize = 16.sp,
            color = WhiteText.copy(alpha = 0.8f),
            fontWeight = FontWeight.Light
        )

        Spacer(modifier = Modifier.height(16.dp))

        // NDSI Company Logo with rotation animation
        Image(
            painter = painterResource(id = R.drawable.ndsilogo),
            contentDescription = "NDSI Logo",
            modifier = Modifier
                .size(80.dp)
                .rotate(rotation)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // NDSI text - BLUE
        Text(
            text = "NDSI",
            fontSize = 24.sp,
            color = BlueText,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SplashScreenPreview() {
    AnyDoubtTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = RedBackground
        ) {
            SplashScreen(onTimeout = {})
        }
    }
}

// Preview without animation for static view
@Preview(showBackground = true, showSystemUi = true, name = "Static Preview")
@Composable
fun SplashScreenStaticPreview() {
    AnyDoubtTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = RedBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(RedBackground)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // App Logo placeholder
                Surface(
                    modifier = Modifier.size(180.dp),
                    shape = CircleShape,
                    color = WhiteText.copy(alpha = 0.2f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "anyDoubt\nLogo",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = WhiteText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // App Name - WHITE
                Text(
                    text = "anyDoubt",
                    fontSize = 36.sp,
                    color = WhiteText,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(80.dp))

                // "Powered by" text
                Text(
                    text = "Powered by",
                    fontSize = 16.sp,
                    color = WhiteText.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Light
                )

                Spacer(modifier = Modifier.height(16.dp))

                // NDSI Company Logo placeholder
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = BlueText.copy(alpha = 0.2f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NDSI\nLogo",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlueText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // NDSI text - BLUE
                Text(
                    text = "NDSI",
                    fontSize = 24.sp,
                    color = BlueText,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}