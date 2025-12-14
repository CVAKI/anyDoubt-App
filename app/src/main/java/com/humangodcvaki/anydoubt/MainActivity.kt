package com.humangodcvaki.anydoubt

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.humangodcvaki.anydoubt.ui.theme.AnyDoubtTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check internet connection on app start
        if (!requireInternet()) {
            // Internet not available, InternetCheckActivity already started
            finish() // Close this activity
            return
        }

        setContent {
            AnyDoubtTheme {
                // Professional update dialog handler
                MainUpdateDialogHandler()

                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    BakingScreen()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Optional: Check internet when app comes back to foreground
        // Uncomment if you want to check on resume
        /*
        if (!isInternetAvailable()) {
            requireInternet()
            finish()
        }
        */
    }
}

// Update Dialog Handler for MainActivity
@Composable
private fun MainUpdateDialogHandler() {
    val context = LocalContext.current
    var showUpdateDialog by remember { mutableStateOf(false) }
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
                // Don't show anything on app start
            },
            onError = { error ->
                Log.e("UpdateDialog", "Update check error: $error")
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
}