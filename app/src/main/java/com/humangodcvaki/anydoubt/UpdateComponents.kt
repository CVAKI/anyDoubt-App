package com.humangodcvaki.anydoubt

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UpdateSettingsCard() {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "App Updates",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        context.startActivity(Intent(context, UpdateActivity::class.java))
                    }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Check for Updates",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Current version: ${UpdateChecker.getCurrentVersionName(context)}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = "Check Updates",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun UpdateMenuItem() {
    val context = LocalContext.current
    var showUpdateBadge by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        UpdateChecker.initialize()
        UpdateChecker.checkForUpdates(
            context = context,
            onUpdateAvailable = { _, _ -> showUpdateBadge = true },
            onNoUpdate = { showUpdateBadge = false },
            onError = { showUpdateBadge = false }
        )
    }

    IconButton(onClick = {
        context.startActivity(Intent(context, UpdateActivity::class.java))
    }) {
        BadgedBox(
            badge = {
                if (showUpdateBadge) {
                    Badge {
                        Text("1")
                    }
                }
            }
        ) {
            Icon(
                imageVector = Icons.Default.PhoneAndroid,
                contentDescription = "Updates"
            )
        }
    }
}

@Composable
fun UpdateDropdownMenuItem(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    DropdownMenuItem(
        text = { Text("Check for Updates") },
        onClick = {
            onDismiss()
            context.startActivity(Intent(context, UpdateActivity::class.java))
        },
        leadingIcon = {
            Icon(Icons.Default.PhoneAndroid, contentDescription = null)
        }
    )
}

@Composable
fun SimpleUpdateButton() {
    val context = LocalContext.current

    TextButton(
        onClick = {
            context.startActivity(Intent(context, UpdateActivity::class.java))
        }
    ) {
        Icon(
            imageVector = Icons.Default.PhoneAndroid,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Check for Updates")
    }
}