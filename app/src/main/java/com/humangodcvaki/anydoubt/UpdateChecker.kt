package com.humangodcvaki.anydoubt

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.database.*

// Custom colors for update dialogs
private val UpdatePrimary = Color(0xFFFF4444)
private val UpdateWarning = Color(0xFFFFAA00)
private val UpdateSuccess = Color(0xFF4CAF50)
private val UpdateBackground = Color(0xFF1E1E1E)

object UpdateChecker {

    private const val TAG = "UpdateChecker"
    private lateinit var database: DatabaseReference

    fun initialize() {
        database = FirebaseDatabase.getInstance().reference
    }

    fun checkForUpdates(context: Context, showDialogIfNoUpdate: Boolean = false) {
        val currentVersionCode = getCurrentVersionCode(context)

        database.child("app_updates").child("latest")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val latestVersionCode = snapshot.child("version_code")
                            .getValue(Int::class.java) ?: currentVersionCode
                        val latestVersionName = snapshot.child("version_name")
                            .getValue(String::class.java) ?: ""
                        val isMandatory = snapshot.child("is_mandatory")
                            .getValue(Boolean::class.java) ?: false

                        if (latestVersionCode > currentVersionCode) {
                            Log.d(TAG, "Update available: $latestVersionName")
                            // Show professional dialog through composable
                            // You'll need to implement this in your Activity
                        } else if (showDialogIfNoUpdate) {
                            // Show no update dialog
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to check updates: ${error.message}")
                }
            })
    }

    fun checkForUpdates(
        context: Context,
        onUpdateAvailable: (String, Boolean) -> Unit,
        onNoUpdate: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentVersionCode = getCurrentVersionCode(context)

        database.child("app_updates").child("latest")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val latestVersionCode = snapshot.child("version_code")
                            .getValue(Int::class.java) ?: currentVersionCode
                        val latestVersionName = snapshot.child("version_name")
                            .getValue(String::class.java) ?: ""
                        val isMandatory = snapshot.child("is_mandatory")
                            .getValue(Boolean::class.java) ?: false

                        if (latestVersionCode > currentVersionCode) {
                            onUpdateAvailable(latestVersionName, isMandatory)
                        } else {
                            onNoUpdate()
                        }
                    } else {
                        onNoUpdate()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    onError(error.message)
                }
            })
    }

    private fun getCurrentVersionCode(context: Context): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    0
                ).longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting version code", e)
            1
        }
    }

    fun getCurrentVersionName(context: Context): String {
        return try {
            val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
            versionName ?: "1.0.0"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting version name", e)
            "1.0.0"
        }
    }

    fun openUpdateScreen(context: Context) {
        context.startActivity(Intent(context, UpdateActivity::class.java))
    }
}

// Professional Update Available Dialog
@Composable
fun UpdateAvailableDialog(
    versionName: String,
    isMandatory: Boolean,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Dialog(
        onDismissRequest = { if (!isMandatory) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isMandatory,
            dismissOnClickOutside = !isMandatory
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = UpdateBackground
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated Icon
                Surface(
                    shape = CircleShape,
                    color = if (isMandatory) UpdateWarning.copy(alpha = 0.2f) else UpdatePrimary.copy(alpha = 0.2f),
                    modifier = Modifier
                        .size(100.dp)
                        .scale(pulse)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMandatory) Icons.Default.Warning else Icons.Default.Star,
                            contentDescription = null,
                            tint = if (isMandatory) UpdateWarning else UpdatePrimary,
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Title
                Text(
                    text = if (isMandatory) "⚠️ Required Update" else "🎉 Update Available",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Version Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isMandatory) UpdateWarning.copy(alpha = 0.2f) else UpdatePrimary.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "Version $versionName",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMandatory) UpdateWarning else UpdatePrimary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Message
                Text(
                    text = if (isMandatory) {
                        "A mandatory update is required to continue using AnyDoubt. Please update now to access all features."
                    } else {
                        "A new version is now available with improvements and bug fixes. Would you like to update?"
                    },
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Update Button
                Button(
                    onClick = onUpdate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMandatory) UpdateWarning else UpdatePrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Update Now",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Later Button (only if not mandatory)
                if (!isMandatory) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Maybe Later",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

// Professional No Update Dialog
@Composable
fun NoUpdateDialog(
    currentVersion: String,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = UpdateBackground
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success Icon with rotation
                Surface(
                    shape = CircleShape,
                    color = UpdateSuccess.copy(alpha = 0.2f),
                    modifier = Modifier.size(100.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = UpdateSuccess,
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Title
                Text(
                    text = "✅ You're Up to Date!",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Version Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = UpdateSuccess.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "Version $currentVersion",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = UpdateSuccess,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Message
                Text(
                    text = "You're already using the latest version of AnyDoubt. Check back later for new updates!",
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // OK Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = UpdateSuccess
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Got it!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Usage Example in MainActivity
@Composable
fun UpdateDialogHandler() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showNoUpdateDialog by remember { mutableStateOf(false) }
    var updateVersion by remember { mutableStateOf("") }
    var isMandatory by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        UpdateChecker.initialize()
        UpdateChecker.checkForUpdates(
            context = context,
            onUpdateAvailable = { version, mandatory ->
                updateVersion = version
                isMandatory = mandatory
                showUpdateDialog = true
            },
            onNoUpdate = {
                // Only show if user explicitly checked
                // showNoUpdateDialog = true
            },
            onError = { error ->
                Log.e("UpdateDialog", "Error: $error")
            }
        )
    }

    // Show Update Available Dialog
    if (showUpdateDialog) {
        UpdateAvailableDialog(
            versionName = updateVersion,
            isMandatory = isMandatory,
            onUpdate = {
                showUpdateDialog = false
                context.startActivity(Intent(context, UpdateActivity::class.java))
            },
            onDismiss = {
                if (!isMandatory) {
                    showUpdateDialog = false
                }
            }
        )
    }

    // Show No Update Dialog
    if (showNoUpdateDialog) {
        NoUpdateDialog(
            currentVersion = UpdateChecker.getCurrentVersionName(context),
            onDismiss = { showNoUpdateDialog = false }
        )
    }
}