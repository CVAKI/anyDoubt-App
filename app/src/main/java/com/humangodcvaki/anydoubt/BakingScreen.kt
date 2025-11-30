package com.humangodcvaki.anydoubt

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.humangodcvaki.anydoubt.ui.theme.AnyDoubtTheme
import kotlinx.coroutines.delay
import java.io.InputStream
import java.util.*

// Custom colors
private val RedPrimary = Color(0xFFF02828)
private val RedSecondary = Color(0xFFD67474)
private val RedLight = Color(0xFFFFE5E5)

// Language options for settings - PRIVATE to avoid conflicts
private val availableLanguagesForSettings = listOf(
    LanguageOption("English (US)", Locale.US, "English 🇺🇸"),
    LanguageOption("English (UK)", Locale.UK, "English 🇬🇧"),
    LanguageOption("Hindi", Locale("hi", "IN"), "हिंदी 🇮🇳"),
    LanguageOption("Malayalam", Locale("ml", "IN"), "മലയാളം 🇮🇳"),
    LanguageOption("Spanish", Locale("es", "ES"), "Español 🇪🇸"),
    LanguageOption("French", Locale.FRENCH, "Français 🇫🇷"),
    LanguageOption("German", Locale.GERMAN, "Deutsch 🇩🇪"),
    LanguageOption("Italian", Locale.ITALIAN, "Italiano 🇮🇹"),
    LanguageOption("Japanese", Locale.JAPANESE, "日本語 🇯🇵"),
    LanguageOption("Korean", Locale.KOREAN, "한국어 🇰🇷"),
    LanguageOption("Chinese", Locale.CHINESE, "中文 🇨🇳")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BakingScreen(
    bakingViewModel: BakingViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var fileType by remember { mutableStateOf<String?>(null) }
    var prompt by remember { mutableStateOf("Explain what's in this document") }
    var selectedLanguage by remember { mutableStateOf(Locale.US) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    val uiState by bakingViewModel.uiState.collectAsState()

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            selectedBitmap = it
            selectedFileName = "camera_${System.currentTimeMillis()}.jpg"
            fileType = "CAMERA"
        }
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                selectedBitmap = BitmapFactory.decodeStream(inputStream)
                selectedFileName = "image_${System.currentTimeMillis()}.jpg"
                fileType = "IMAGE"
                inputStream?.close()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    // PDF picker launcher
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val parcelFileDescriptor = context.contentResolver.openFileDescriptor(it, "r")
                parcelFileDescriptor?.let { pfd ->
                    val pdfRenderer = PdfRenderer(pfd)
                    val page = pdfRenderer.openPage(0)

                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    selectedBitmap = bitmap
                    selectedFileName = "document_${System.currentTimeMillis()}.pdf"
                    fileType = "PDF"

                    page.close()
                    pdfRenderer.close()
                    pfd.close()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    // Watch for successful analysis
    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) {
            // Wait 3 seconds on loading screen, then navigate to answer activity
            delay(3000)
            val intent = Intent(context, AnswerActivity::class.java)
            intent.putExtra("ANALYSIS_RESULT", (uiState as UiState.Success).outputText)
            intent.putExtra("SELECTED_LANGUAGE", selectedLanguage.language)
            context.startActivity(intent)
        }
    }

    BakingScreenContent(
        selectedBitmap = selectedBitmap,
        selectedFileName = selectedFileName,
        fileType = fileType,
        prompt = prompt,
        uiState = uiState,
        selectedLanguage = selectedLanguage,
        showSettingsDialog = showSettingsDialog,
        onPromptChange = { prompt = it },
        onCameraClick = { cameraLauncher.launch(null) },
        onImageClick = { imagePickerLauncher.launch("image/*") },
        onPdfClick = { pdfPickerLauncher.launch("application/pdf") },
        onAnalyzeClick = {
            selectedBitmap?.let { bitmap ->
                // Navigate to loading screen IMMEDIATELY
                val loadingIntent = Intent(context, AnalyzingLoadingActivity::class.java)
                context.startActivity(loadingIntent)

                // Get language name for the prompt
                val languageName = when (selectedLanguage.language) {
                    "en" -> "English"
                    "hi" -> "Hindi"
                    "ml" -> "Malayalam"
                    "es" -> "Spanish"
                    "fr" -> "French"
                    "de" -> "German"
                    "it" -> "Italian"
                    "ja" -> "Japanese"
                    "ko" -> "Korean"
                    "zh" -> "Chinese"
                    else -> "English"
                }

                // Create enhanced prompt with language instruction
                val enhancedPrompt = if (selectedLanguage.language != "en") {
                    """
                    IMPORTANT: You must respond ENTIRELY in $languageName language.
                    All explanations, descriptions, and text must be written in $languageName.
                    
                    User's question: $prompt
                    """.trimIndent()
                } else {
                    prompt
                }

                // Start the analysis with language-enhanced prompt
                bakingViewModel.sendPrompt(bitmap, enhancedPrompt)
            }
        },
        onClearClick = {
            selectedBitmap = null
            selectedFileName = null
            fileType = null
        },
        onQuickActionClick = { newPrompt -> prompt = newPrompt },
        onSettingsClick = { showSettingsDialog = true },
        onLanguageChange = { selectedLanguage = it },
        onDismissSettings = { showSettingsDialog = false },
        onLogout = {
            try {
                // IMPORTANT: Sign out from Firebase first
                FirebaseAuth.getInstance().signOut()

                // Then navigate to LoginActivity and clear all previous activities
                val intent = Intent(context, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                context.startActivity(intent)

                // Finish current activity
                (context as? ComponentActivity)?.finish()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BakingScreenContent(
    selectedBitmap: Bitmap?,
    selectedFileName: String?,
    fileType: String?,
    prompt: String,
    uiState: UiState,
    selectedLanguage: Locale,
    showSettingsDialog: Boolean,
    onPromptChange: (String) -> Unit,
    onCameraClick: () -> Unit,
    onImageClick: () -> Unit,
    onPdfClick: () -> Unit,
    onAnalyzeClick: () -> Unit,
    onClearClick: () -> Unit,
    onQuickActionClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onLanguageChange: (Locale) -> Unit,
    onDismissSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Settings Dialog
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = onDismissSettings,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = RedPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Settings",
                        fontWeight = FontWeight.Bold,
                        color = RedPrimary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Language Selection",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = RedPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        "Analysis results will be in the selected language",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    availableLanguagesForSettings.forEach { lang ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            onClick = {
                                onLanguageChange(lang.locale)
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (lang.locale.language == selectedLanguage.language)
                                    RedPrimary.copy(alpha = 0.15f)
                                else
                                    Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    lang.displayName,
                                    fontSize = 16.sp,
                                    fontWeight = if (lang.locale.language == selectedLanguage.language)
                                        FontWeight.Bold
                                    else
                                        FontWeight.Normal,
                                    color = if (lang.locale.language == selectedLanguage.language)
                                        RedPrimary
                                    else
                                        Color.DarkGray
                                )
                                if (lang.locale.language == selectedLanguage.language) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = RedPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = RedSecondary.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            onDismissSettings()
                            onLogout()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Logout", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissSettings) {
                    Text("Close", color = RedPrimary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Section with Settings Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.anydoubt),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "anyDoubt AI",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedPrimary
                        )
                        Text(
                            text = "Your Smart Study Assistant",
                            fontSize = 14.sp,
                            color = RedSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(RedPrimary)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }

            HorizontalDivider(
                color = RedSecondary.copy(alpha = 0.3f),
                thickness = 2.dp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Upload Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "📚 Upload Your Study Material",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = RedPrimary
                    )

                    Text(
                        text = "Camera, Images, PDFs - We handle it all!",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    // File Preview
                    if (selectedBitmap != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = RedLight.copy(alpha = 0.3f)
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Image(
                                    bitmap = selectedBitmap.asImageBitmap(),
                                    contentDescription = "Selected file",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )

                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    color = RedPrimary
                                ) {
                                    Text(
                                        text = fileType ?: "FILE",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Text(
                            text = selectedFileName ?: "Unknown file",
                            fontSize = 12.sp,
                            color = RedSecondary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .scale(scale),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = RedLight.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(80.dp),
                                    tint = RedSecondary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Tap below to upload",
                                    fontSize = 16.sp,
                                    color = RedSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // All buttons same color (RedPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onCameraClick,
                            modifier = Modifier.weight(1f).height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(24.dp))
                                Text("Camera", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = onImageClick,
                            modifier = Modifier.weight(1f).height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Create, contentDescription = null, modifier = Modifier.size(24.dp))
                                Text("Image", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = onPdfClick,
                            modifier = Modifier.weight(1f).height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(24.dp))
                                Text("PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (selectedBitmap != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onClearClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = RedPrimary
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear & Upload New")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Actions
            AnimatedVisibility(visible = selectedBitmap != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "⚡ Quick Actions",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onQuickActionClick("Explain what's in this document") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.Black
                                )
                            ) {
                                Text("Explain", color = Color.Black)
                            }
                            OutlinedButton(
                                onClick = { onQuickActionClick("Solve this problem step by step") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.Black
                                )
                            ) {
                                Text("Solve", color = Color.Black)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = prompt,
                            onValueChange = onPromptChange,
                            label = { Text("Ask anything...", color = Color.Blue) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RedPrimary,
                                focusedLabelColor = Color.Blue,
                                unfocusedLabelColor = Color.Blue,
                                cursorColor = RedPrimary,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onAnalyzeClick,
                            enabled = selectedBitmap != null && prompt.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Analyze Now", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Error State (Loading is handled by separate activity)
            when (uiState) {
                is UiState.Error -> {
                    Spacer(modifier = Modifier.height(20.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = RedPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Error: ${(uiState as UiState.Error).errorMessage}", color = RedPrimary)
                        }
                    }
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BakingScreenPreview() {
    AnyDoubtTheme {
        BakingScreenContent(
            selectedBitmap = null,
            selectedFileName = null,
            fileType = null,
            prompt = "Explain what's in this document",
            uiState = UiState.Initial,
            selectedLanguage = Locale.US,
            showSettingsDialog = false,
            onPromptChange = {},
            onCameraClick = {},
            onImageClick = {},
            onPdfClick = {},
            onAnalyzeClick = {},
            onClearClick = {},
            onQuickActionClick = {},
            onSettingsClick = {},
            onLanguageChange = {},
            onDismissSettings = {},
            onLogout = {}
        )
    }
}