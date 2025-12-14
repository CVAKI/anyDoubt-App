package com.humangodcvaki.anydoubt

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humangodcvaki.anydoubt.ui.theme.AnyDoubtTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

private val RedPrimary = Color(0xFFF02828)
private val RedSecondary = Color(0xFFD67474)
private val RedLight = Color(0xFFFFE5E5)

class InternetCheckActivity : ComponentActivity() {

    private lateinit var connectivityManager: ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        setContent {
            var isConnected by remember { mutableStateOf(isInternetAvailable()) }
            var showRetrying by remember { mutableStateOf(false) }

            // Monitor network changes
            LaunchedEffect(Unit) {
                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()

                networkCallback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        isConnected = true
                    }

                    override fun onLost(network: Network) {
                        isConnected = false
                    }
                }

                connectivityManager.registerNetworkCallback(request, networkCallback!!)
            }

            // Navigate to main activity when connected
            LaunchedEffect(isConnected) {
                if (isConnected) {
                    delay(1000) // Small delay for smooth transition
                    navigateToMain()
                }
            }

            AnyDoubtTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    InternetCheckScreen(
                        isConnected = isConnected,
                        showRetrying = showRetrying,
                        onRetry = {
                            showRetrying = true
                            isConnected = isInternetAvailable()
                            if (!isConnected) {
                                // Use proper coroutine scope instead of GlobalScope
                                CoroutineScope(Dispatchers.Main).launch {
                                    delay(2000)
                                    showRetrying = false
                                }
                            }
                        },
                        onOpenSettings = { openWifiSettings() },
                        onGoBack = { finish() }
                    )
                }
            }
        }
    }

    private fun isInternetAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun openWifiSettings() {
        try {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        } catch (e: Exception) {
            // Fallback to general wireless settings
            startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
        }
    }
}

@Composable
fun InternetCheckScreen(
    isConnected: Boolean,
    showRetrying: Boolean,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onGoBack: () -> Unit
) {
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
            )
    ) {
        // Animated background waves
        AnimatedBackgroundWaves()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Icon
            AnimatedConnectionIcon(isConnected = isConnected, showRetrying = showRetrying)

            Spacer(modifier = Modifier.height(32.dp))

            // Status Text
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = when {
                            isConnected -> "Connected! ✓"
                            showRetrying -> "Checking..."
                            else -> "No Internet Connection"
                        },
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isConnected -> Color(0xFF4CAF50)
                            showRetrying -> Color(0xFFFFA726)
                            else -> RedPrimary
                        },
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = when {
                            isConnected -> "Redirecting to app..."
                            showRetrying -> "Please wait while we check your connection"
                            else -> "Please check your internet connection and try again"
                        },
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Action Buttons
            if (!isConnected && !showRetrying) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Retry Button
                    Button(
                        onClick = onRetry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RedPrimary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 8.dp
                        )
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Retry Connection",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Open Settings Button
                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(RedPrimary, RedSecondary)
                            )
                        )
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Open Wi-Fi Settings",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Go Back Button
                    TextButton(
                        onClick = onGoBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Go Back",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            } else if (showRetrying) {
                CircularProgressIndicator(
                    color = RedPrimary,
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Tips Card
            if (!isConnected && !showRetrying) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.1f)
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFFFA726),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Troubleshooting Tips",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TipItem("Check if Wi-Fi or mobile data is turned on")
                        TipItem("Try airplane mode on/off")
                        TipItem("Restart your router if using Wi-Fi")
                        TipItem("Move closer to your Wi-Fi router")
                    }
                }
            }
        }
    }
}

@Composable
fun TipItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "•",
            color = RedPrimary,
            fontSize = 14.sp,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun AnimatedConnectionIcon(isConnected: Boolean, showRetrying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "icon")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .size(120.dp)
                .then(
                    if (showRetrying) Modifier.rotate(rotation)
                    else Modifier.scale(scale)
                ),
            shape = CircleShape,
            color = when {
                isConnected -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                showRetrying -> Color(0xFFFFA726).copy(alpha = 0.2f)
                else -> RedPrimary.copy(alpha = 0.2f)
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = when {
                        isConnected -> Icons.Default.CheckCircle
                        showRetrying -> Icons.Default.Refresh
                        else -> Icons.Default.Warning
                    },
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = when {
                        isConnected -> Color(0xFF4CAF50)
                        showRetrying -> Color(0xFFFFA726)
                        else -> RedPrimary
                    }
                )
            }
        }
    }
}

@Composable
fun AnimatedBackgroundWaves() {
    val infiniteTransition = rememberInfiniteTransition(label = "waves")

    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Draw multiple waves
        for (i in 0..2) {
            val path = Path()
            path.moveTo(0f, height / 2)

            for (x in 0..width.toInt() step 10) {
                val y = height / 2 + sin((x + offset + i * 300) * 0.01f) * (50f + i * 20f)
                path.lineTo(x.toFloat(), y)
            }

            drawPath(
                path = path,
                color = RedPrimary.copy(alpha = 0.1f - i * 0.03f),
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun InternetCheckScreenPreview() {
    AnyDoubtTheme {
        InternetCheckScreen(
            isConnected = false,
            showRetrying = false,
            onRetry = {},
            onOpenSettings = {},
            onGoBack = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Retrying")
@Composable
fun InternetCheckScreenRetryingPreview() {
    AnyDoubtTheme {
        InternetCheckScreen(
            isConnected = false,
            showRetrying = true,
            onRetry = {},
            onOpenSettings = {},
            onGoBack = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Connected")
@Composable
fun InternetCheckScreenConnectedPreview() {
    AnyDoubtTheme {
        InternetCheckScreen(
            isConnected = true,
            showRetrying = false,
            onRetry = {},
            onOpenSettings = {},
            onGoBack = {}
        )
    }
}