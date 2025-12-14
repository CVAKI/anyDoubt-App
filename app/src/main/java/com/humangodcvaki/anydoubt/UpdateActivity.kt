package com.humangodcvaki.anydoubt

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.*
import com.humangodcvaki.anydoubt.ui.theme.AnyDoubtTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class UpdateInfo(
    val currentVersion: String = "",
    val latestVersion: String = "",
    val versionCode: Int = 0,
    val updateSize: String = "",
    val releaseNotes: List<String> = emptyList(),
    val downloadUrl: String = "", // This will be your website URL
    val isUpdateAvailable: Boolean = false,
    val isMandatory: Boolean = false,
    val releaseDate: String = ""
)

class UpdateActivity : ComponentActivity() {

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = FirebaseDatabase.getInstance().reference

        setContent {
            AnyDoubtTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UpdateScreen(
                        onCheckUpdate = { callback -> checkForUpdates(callback) },
                        onDownloadUpdate = { updateInfo -> openWebsite(updateInfo.downloadUrl) },
                        onClose = { finish() }
                    )
                }
            }
        }
    }

    private fun checkForUpdates(callback: (UpdateInfo) -> Unit) {
        val currentVersion = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }

        val currentVersionCode = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(packageName, 0).longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0).versionCode
            }
        } catch (e: Exception) {
            1
        }

        database.child("app_updates").child("latest")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val latestVersion = snapshot.child("version_name").getValue(String::class.java) ?: currentVersion
                        val latestVersionCode = snapshot.child("version_code").getValue(Int::class.java) ?: currentVersionCode
                        val updateSize = snapshot.child("size").getValue(String::class.java) ?: "Unknown"
                        val downloadUrl = snapshot.child("download_url").getValue(String::class.java) ?: ""
                        val isMandatory = snapshot.child("is_mandatory").getValue(Boolean::class.java) ?: false
                        val releaseDate = snapshot.child("release_date").getValue(String::class.java) ?: ""

                        val releaseNotesList = mutableListOf<String>()
                        snapshot.child("release_notes").children.forEach { noteSnapshot ->
                            noteSnapshot.getValue(String::class.java)?.let { releaseNotesList.add(it) }
                        }

                        val isUpdateAvailable = latestVersionCode > currentVersionCode

                        val updateInfo = UpdateInfo(
                            currentVersion = currentVersion,
                            latestVersion = latestVersion,
                            versionCode = latestVersionCode,
                            updateSize = updateSize,
                            releaseNotes = releaseNotesList,
                            downloadUrl = downloadUrl,
                            isUpdateAvailable = isUpdateAvailable,
                            isMandatory = isMandatory,
                            releaseDate = releaseDate
                        )

                        callback(updateInfo)
                    } else {
                        callback(
                            UpdateInfo(
                                currentVersion = currentVersion,
                                latestVersion = currentVersion,
                                versionCode = currentVersionCode,
                                isUpdateAvailable = false
                            )
                        )
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@UpdateActivity,
                        "Failed to check updates: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    callback(
                        UpdateInfo(
                            currentVersion = currentVersion,
                            latestVersion = currentVersion,
                            versionCode = currentVersionCode,
                            isUpdateAvailable = false
                        )
                    )
                }
            })
    }

    private fun openWebsite(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
            Toast.makeText(this, "Opening download page...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to open browser: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun UpdateScreen(
    onCheckUpdate: ((UpdateInfo) -> Unit) -> Unit,
    onDownloadUpdate: (UpdateInfo) -> Unit,
    onClose: () -> Unit
) {
    var isChecking by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        FloatingBubblesBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Updates",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            val iconRotation by rememberInfiniteTransition(label = "icon").animateFloat(
                initialValue = 0f,
                targetValue = if (isChecking) 360f else 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "iconRotation"
            )

            Icon(
                imageVector = Icons.Default.PhoneAndroid,
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer(rotationZ = iconRotation),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (updateInfo == null) {
                Text(
                    text = "Check for Updates",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Tap below to check if a new version is available",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                )
            } else {
                if (updateInfo!!.isUpdateAvailable) {
                    UpdateAvailableCard(
                        updateInfo = updateInfo!!,
                        onDownloadUpdate = onDownloadUpdate
                    )
                } else {
                    UpdateNotAvailableCard(updateInfo!!)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (updateInfo == null || !updateInfo!!.isUpdateAvailable) {
                Button(
                    onClick = {
                        scope.launch {
                            isChecking = true
                            delay(500)
                            onCheckUpdate { info ->
                                updateInfo = info
                                isChecking = false
                            }
                        }
                    },
                    enabled = !isChecking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp
                    )
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Check for Updates",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Current Version: ${updateInfo?.currentVersion ?: "Checking..."}",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun UpdateAvailableCard(
    updateInfo: UpdateInfo,
    onDownloadUpdate: (UpdateInfo) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (updateInfo.isMandatory) Icons.Default.Warning else Icons.Default.Star,
                    contentDescription = null,
                    tint = if (updateInfo.isMandatory) Color(0xFFFFAA00) else Color(0xFFFF4444),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (updateInfo.isMandatory) "Required Update!" else "Update Available!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Version ${updateInfo.latestVersion} • ${updateInfo.updateSize}",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            if (updateInfo.releaseDate.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Released: ${updateInfo.releaseDate}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            if (updateInfo.releaseNotes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "What's New:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                updateInfo.releaseNotes.forEach { note ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "• $note",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onDownloadUpdate(updateInfo) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (updateInfo.isMandatory) Color(0xFFFFAA00) else Color(0xFFFF4444)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Go to Download Page",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun UpdateNotAvailableCard(updateInfo: UpdateInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "You're Up to Date!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Version ${updateInfo.currentVersion} is the latest version",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}