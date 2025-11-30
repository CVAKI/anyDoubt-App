package com.humangodcvaki.anydoubt

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.humangodcvaki.anydoubt.ui.theme.AnyDoubtTheme
import kotlin.math.sin
import kotlin.random.Random

class LoginActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Check if user is already signed in
        if (auth.currentUser != null) {
            navigateToMain()
            return
        }

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            AnyDoubtTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LoginScreen(
                        onGoogleSignInClick = { signInWithGoogle() }
                    )
                }
            }
        }
    }

    private fun signInWithGoogle() {
        // Sign out first to force account picker
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Sign in successful!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

data class Bubble(
    val id: Int,
    var x: Float,
    var y: Float,
    val size: Float,
    val speed: Float,
    val wobbleSpeed: Float,
    val wobbleAmount: Float,
    var alpha: Float = 0f,
    var scale: Float = 0f,
    var lifetime: Float = 0f,
    var maxLifetime: Float
)

@Composable
fun FloatingBubblesBackground() {
    val bubbles = remember {
        mutableStateListOf<Bubble>().apply {
            repeat(15) { index ->
                add(
                    Bubble(
                        id = index,
                        x = Random.nextFloat(),
                        y = Random.nextFloat() * 1.2f + 0.2f,
                        size = Random.nextFloat() * 60f + 30f,
                        speed = Random.nextFloat() * 0.0008f + 0.0003f,
                        wobbleSpeed = Random.nextFloat() * 0.5f + 0.3f,
                        wobbleAmount = Random.nextFloat() * 0.02f + 0.008f,
                        maxLifetime = Random.nextFloat() * 3000f + 4000f
                    )
                )
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bubbles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10000f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        bubbles.forEach { bubble ->
            // Update bubble lifetime
            bubble.lifetime += 16f // Approximate frame time

            // Fade in/out logic
            when {
                bubble.lifetime < 500f -> {
                    bubble.alpha = (bubble.lifetime / 500f).coerceIn(0f, 1f)
                    bubble.scale = bubble.alpha
                }
                bubble.lifetime > bubble.maxLifetime - 1000f -> {
                    val fadeOut = (bubble.maxLifetime - bubble.lifetime) / 1000f
                    bubble.alpha = fadeOut.coerceIn(0f, 1f)
                    bubble.scale = 1f + (1f - fadeOut) * 0.5f
                }
                else -> {
                    bubble.alpha = 1f
                    bubble.scale = 1f
                }
            }

            // Reset bubble if lifetime exceeded
            if (bubble.lifetime > bubble.maxLifetime) {
                bubble.y = 1.2f
                bubble.x = Random.nextFloat()
                bubble.lifetime = 0f
                bubble.maxLifetime = Random.nextFloat() * 3000f + 4000f
            }

            // Move bubble up
            bubble.y -= bubble.speed

            // Wobble effect
            val wobbleOffset = sin(time * bubble.wobbleSpeed) * bubble.wobbleAmount

            // Calculate position
            val x = (bubble.x + wobbleOffset) * width
            val y = bubble.y * height

            // Draw bubble with gradient and glow effect
            val colors = listOf(
                Color(0xFFFF3B3B).copy(alpha = bubble.alpha * 0.6f),
                Color(0xFFFF6B6B).copy(alpha = bubble.alpha * 0.4f),
                Color(0xFFFF8787).copy(alpha = bubble.alpha * 0.2f)
            )

            scale(bubble.scale, pivot = Offset(x, y)) {
                // Outer glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = colors,
                        center = Offset(x, y),
                        radius = bubble.size * 1.3f
                    ),
                    radius = bubble.size * 1.3f,
                    center = Offset(x, y)
                )

                // Main bubble
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF4D4D).copy(alpha = bubble.alpha * 0.8f),
                            Color(0xFFFF3333).copy(alpha = bubble.alpha * 0.6f),
                            Color(0xFFCC0000).copy(alpha = bubble.alpha * 0.3f)
                        ),
                        center = Offset(x, y),
                        radius = bubble.size
                    ),
                    radius = bubble.size,
                    center = Offset(x, y)
                )

                // Highlight
                drawCircle(
                    color = Color.White.copy(alpha = bubble.alpha * 0.3f),
                    radius = bubble.size * 0.3f,
                    center = Offset(x - bubble.size * 0.3f, y - bubble.size * 0.3f)
                )
            }
        }
    }
}

@Composable
fun LoginScreen(onGoogleSignInClick: () -> Unit) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Animated background bubbles
        FloatingBubblesBackground()

        // Content with fade-in animation
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(1000)) +
                    slideInVertically(
                        animationSpec = tween(1000, easing = FastOutSlowInEasing),
                        initialOffsetY = { it / 4 }
                    )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.weight(1f))

                // App Logo with scale animation
                val logoScale by rememberInfiniteTransition(label = "logo").animateFloat(
                    initialValue = 1f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "logoScale"
                )

                Image(
                    painter = painterResource(id = R.drawable.anydoubt),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(140.dp)
                        .padding(bottom = 16.dp)
                        .graphicsLayer(
                            scaleX = logoScale,
                            scaleY = logoScale
                        )
                )

                // App Name with animated color
                val colorAnimation by rememberInfiniteTransition(label = "color").animateColor(
                    initialValue = MaterialTheme.colorScheme.primary,
                    targetValue = Color(0xFFFF4444),
                    animationSpec = infiniteRepeatable(
                        animation = tween(3000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "colorAnimation"
                )

                Text(
                    text = "AnyDoubt",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorAnimation
                )

                Text(
                    text = "Your AI Study Assistant",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
                )

                // Welcome Text
                Text(
                    text = "Welcome!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Sign in to continue",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // Animated Google Sign In Button
                var isPressed by remember { mutableStateOf(false) }
                val buttonScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.95f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "buttonScale"
                )

                Button(
                    onClick = {
                        isPressed = true
                        onGoogleSignInClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .graphicsLayer(
                            scaleX = buttonScale,
                            scaleY = buttonScale
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 4.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Sign in with Google",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Terms and Privacy
                Text(
                    text = "By signing in, you agree to our Terms of Service and Privacy Policy",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Developer Credit
                Text(
                    text = "Developed by 𝗖𝗩♞𝗞𝗜",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}

// Preview function for Android Studio
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    AnyDoubtTheme {
        LoginScreen(
            onGoogleSignInClick = { /* Preview - no action */ }
        )
    }
}