@file:Suppress("INFERRED_INVISIBLE_VARARG_TYPE_ARGUMENT_WARNING")

package com.humangodcvaki.anydoubt

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import java.io.InputStream
import java.util.*

// Custom colors
private val RedPrimary = Color(0xFFF02828)
private val RedSecondary = Color(0xFFD67474)
private val RedLight = Color(0xFFFFE5E5)

// Note: LanguageOption data class and AdMobHelper class are assumed to be defined elsewhere.
// LanguageOption is typically defined as:
// data class LanguageOption(val name: String, val locale: Locale, val displayName: String)

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
    val activity = context as? ComponentActivity

    // Initialize AdMob Helper
    val adMobHelper = remember { AdMobHelper(context) }

    var selectedPdfPages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var fileType by remember { mutableStateOf<String?>(null) }
    var prompt by remember { mutableStateOf("Explain what's in this document") }
    var selectedLanguage by remember { mutableStateOf(Locale.US) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showPdfErrorDialog by remember { mutableStateOf(false) }
    var pdfErrorMessage by remember { mutableStateOf("") }
    val uiState by bakingViewModel.uiState.collectAsState()

    // CRITICAL: Clean up on dispose
    DisposableEffect(Unit) {
        onDispose {
            selectedPdfPages.forEach {
                try { it.recycle() } catch (e: Exception) { }
            }
            selectedBitmap?.let {
                try { it.recycle() } catch (e: Exception) { }
            }
        }
    }

    val onClearSelection: () -> Unit = {
        // Recycle before clearing
        selectedBitmap?.let {
            try { it.recycle() } catch (e: Exception) { }
        }
        selectedPdfPages.forEach {
            try { it.recycle() } catch (e: Exception) { }
        }

        selectedBitmap = null
        selectedFileName = null
        fileType = null
        selectedPdfPages = emptyList()

        // Force garbage collection
        System.gc()
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        onClearSelection()
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
        onClearSelection()
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

    // PDF picker launcher - MEMORY OPTIMIZED
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onClearSelection()
        uri?.let {
            var parcelFileDescriptor: android.os.ParcelFileDescriptor? = null
            var pdfRenderer: PdfRenderer? = null

            try {
                parcelFileDescriptor = context.contentResolver.openFileDescriptor(it, "r")
                parcelFileDescriptor?.let { pfd ->
                    pdfRenderer = PdfRenderer(pfd)
                    val pageCount = pdfRenderer!!.pageCount

                    // UPDATED LIMITS
                    val MAX_WIDTH = 1536
                    val MAX_PAGES = 50
                    val SPACING_BETWEEN_PAGES = 10
                    val MAX_TOTAL_HEIGHT = 30000

                    if (pageCount > MAX_PAGES) {
                        pdfErrorMessage = "PDF is too large! Maximum $MAX_PAGES pages allowed. Your PDF has $pageCount pages."
                        showPdfErrorDialog = true
                        return@rememberLauncherForActivityResult
                    }

                    val pageBitmapsForPreview = mutableListOf<Bitmap>()
                    var estimatedTotalHeight = 0

                    // CRITICAL FIX: Use ARGB_8888 instead of RGB_565
                    val bitmapConfig = Bitmap.Config.ARGB_8888

                    for (i in 0 until pageCount) {
                        val page = pdfRenderer!!.openPage(i)

                        val scaleFactor = if (page.width > MAX_WIDTH) {
                            MAX_WIDTH.toFloat() / page.width.toFloat()
                        } else {
                            1f  // Keep original size if smaller
                        }

                        val scaledWidth = (page.width * scaleFactor).toInt()
                        val scaledHeight = (page.height * scaleFactor).toInt()

                        estimatedTotalHeight += scaledHeight + SPACING_BETWEEN_PAGES
                        if (estimatedTotalHeight > MAX_TOTAL_HEIGHT) {
                            page.close()
                            pageBitmapsForPreview.forEach {
                                try { it.recycle() } catch (e: Exception) { }
                            }
                            pdfErrorMessage = "PDF is too large! The combined height exceeds the limit. Try a PDF with fewer or smaller pages."
                            showPdfErrorDialog = true
                            return@rememberLauncherForActivityResult
                        }

                        val bitmap = Bitmap.createBitmap(
                            scaledWidth,
                            scaledHeight,
                            bitmapConfig  // Using ARGB_8888
                        )

                        // Render with white background
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        pageBitmapsForPreview.add(bitmap)
                        page.close()
                    }

                    // Combine pages for model input
                    if (pageBitmapsForPreview.isNotEmpty()) {
                        val totalHeight = pageBitmapsForPreview.sumOf { it.height } +
                                (SPACING_BETWEEN_PAGES * (pageCount - 1))
                        val maxWidth = pageBitmapsForPreview.maxOf { it.width }

                        val estimatedSizeBytes = maxWidth * totalHeight * 4  // ARGB_8888 = 4 bytes
                        val maxSizeBytes = 100 * 1024 * 1024 // 100MB for 50 pages

                        if (estimatedSizeBytes > maxSizeBytes) {
                            pageBitmapsForPreview.forEach {
                                try { it.recycle() } catch (e: Exception) { }
                            }
                            pdfErrorMessage = "PDF is too large! The combined size (${estimatedSizeBytes / (1024 * 1024)}MB) exceeds the 100MB limit."
                            showPdfErrorDialog = true
                            return@rememberLauncherForActivityResult
                        }

                        val combinedBitmap = Bitmap.createBitmap(
                            maxWidth,
                            totalHeight,
                            bitmapConfig
                        )
                        val canvas = android.graphics.Canvas(combinedBitmap)
                        canvas.drawColor(android.graphics.Color.WHITE)

                        var currentY = 0f
                        pageBitmapsForPreview.forEach { pageBitmap ->
                            val xOffset = (maxWidth - pageBitmap.width) / 2f
                            canvas.drawBitmap(pageBitmap, xOffset, currentY, null)
                            currentY += pageBitmap.height + SPACING_BETWEEN_PAGES
                        }

                        selectedBitmap = combinedBitmap
                        selectedPdfPages = pageBitmapsForPreview
                        selectedFileName = "document_${System.currentTimeMillis()}.pdf ($pageCount pages)"
                        fileType = "PDF"
                    }
                }
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
                pdfErrorMessage = "Out of memory! PDF is too large. Please try a smaller PDF."
                showPdfErrorDialog = true
                onClearSelection()
            } catch (e: Exception) {
                e.printStackTrace()
                pdfErrorMessage = "Error processing PDF: ${e.message}"
                showPdfErrorDialog = true
                onClearSelection()
            } finally {
                try {
                    pdfRenderer?.close()
                    parcelFileDescriptor?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Watch for successful analysis
    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) {
            val intent = Intent(context, AnswerActivity::class.java)
            intent.putExtra("ANALYSIS_RESULT", (uiState as UiState.Success).outputText)
            intent.putExtra("SELECTED_LANGUAGE", selectedLanguage.language)
            context.startActivity(intent)

            // Recycle the PDF page bitmaps and main bitmap after successful navigation
            selectedPdfPages.forEach {
                try { it.recycle() } catch (e: Exception) { }
            }
            selectedPdfPages = emptyList()
            selectedBitmap?.let {
                try { it.recycle() } catch (e: Exception) { }
            }
            selectedBitmap = null

            // Force GC
            System.gc()
        }
    }

    // Preload ad when screen is composed////////////////////////////////////ADS''''''''''
    LaunchedEffect(Unit) {
        adMobHelper.preloadAd()
    }

    BakingScreenContent(
        selectedBitmap = selectedBitmap,
        selectedPdfPages = selectedPdfPages,
        selectedFileName = selectedFileName,
        fileType = fileType,
        prompt = prompt,
        uiState = uiState,
        selectedLanguage = selectedLanguage,
        showLanguageDialog = showLanguageDialog,
        showPdfErrorDialog = showPdfErrorDialog,
        pdfErrorMessage = pdfErrorMessage,
        onPromptChange = { prompt = it },
        onCameraClick = { cameraLauncher.launch(null) },
        onImageClick = { imagePickerLauncher.launch("image/*") },
        onPdfClick = { pdfPickerLauncher.launch("application/pdf") },
        onAnalyzeClick = {
            selectedBitmap?.let { bitmap ->
                // Show ad first, then proceed with analysis
                activity?.let { act ->
                    adMobHelper.showInterstitialAd(act) {
                        // This callback runs after ad is dismissed or failed

                        // Navigate to loading screen IMMEDIATELY
                        val loadingIntent = Intent(context, AnalyzingLoadingActivity::class.java)
                        context.startActivity(loadingIntent)

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

                        // Clean up preview bitmaps immediately after starting analysis
                        selectedPdfPages.forEach {
                            try { it.recycle() } catch (e: Exception) { }
                        }
                        selectedPdfPages = emptyList()
                        System.gc()
                    }
                }
            }
        },
        onClearClick = onClearSelection,
        onQuickActionClick = { newPrompt -> prompt = newPrompt },
        onLanguageClick = { showLanguageDialog = true },
        onLanguageChange = { selectedLanguage = it },
        onDismissLanguage = { showLanguageDialog = false },
        onDismissPdfError = { showPdfErrorDialog = false },
        onLogout = {
            try {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(context, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                context.startActivity(intent)
                (context as? ComponentActivity)?.finish()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        },
        onGalleryClick = {
            val intent = Intent(context, GalleryActivity::class.java)
            context.startActivity(intent)
        }
    )
}

// NOTE: This extension function was in the first file but not used,
// and may have been a mistake as it copies a Bitmap using another copy function.
// It is omitted from the final merge as it is not present in the more complete code structure.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BakingScreenContent(
    selectedBitmap: Bitmap?,
    selectedPdfPages: List<Bitmap>,
    selectedFileName: String?,
    fileType: String?,
    prompt: String,
    uiState: UiState,
    selectedLanguage: Locale,
    showLanguageDialog: Boolean,
    showPdfErrorDialog: Boolean,
    pdfErrorMessage: String,
    onPromptChange: (String) -> Unit,
    onCameraClick: () -> Unit,
    onImageClick: () -> Unit,
    onPdfClick: () -> Unit,
    onAnalyzeClick: () -> Unit,
    onClearClick: () -> Unit,
    onQuickActionClick: (String) -> Unit,
    onLanguageClick: () -> Unit,
    onLanguageChange: (Locale) -> Unit,
    onDismissLanguage: () -> Unit,
    onDismissPdfError: () -> Unit,
    onLogout: () -> Unit,
    onGalleryClick: () -> Unit
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

    // PDF Error Dialog
    if (showPdfErrorDialog) {
        AlertDialog(
            onDismissRequest = onDismissPdfError,
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = RedPrimary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "PDF Too Large",
                    fontWeight = FontWeight.Bold,
                    color = RedPrimary
                )
            },
            text = {
                Text(
                    pdfErrorMessage,
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
            },
            confirmButton = {
                Button(
                    onClick = onDismissPdfError,
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Language Selection Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = onDismissLanguage,
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
                        "Select Language",
                        fontWeight = FontWeight.Bold,
                        color = RedPrimary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .heightIn(max = 400.dp)
                ) {
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
                                onDismissLanguage()
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
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissLanguage) {
                    Text("Close", color = RedPrimary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ------------------------------------
    // START OF MAIN CONTENT
    // ------------------------------------

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
            // Header Section with Logout Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Assuming R.drawable.anydoubt is correct
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

                // Logout Button
                IconButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEA0B0B))
                ) {
                    Icon(
                        Icons.Default.ExitToApp,
                        contentDescription = "Logout",
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

                    // File Preview - MODIFIED FOR PDF HANDLING
                    if (selectedBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp) // Fixed height for preview box
                                .clip(RoundedCornerShape(16.dp))
                                .background(RedLight.copy(alpha = 0.3f))
                        ) {
                            if (fileType == "PDF" && selectedPdfPages.isNotEmpty()) {
                                // Horizontal scrolling preview for PDF pages
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    selectedPdfPages.forEachIndexed { index, pageBitmap ->
                                        // Individual Page Card
                                        Card(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .aspectRatio(pageBitmap.width.toFloat() / pageBitmap.height.toFloat())
                                                .padding(horizontal = 4.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                Image(
                                                    bitmap = pageBitmap.asImageBitmap(),
                                                    contentDescription = "PDF Page ${index + 1}",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Fit
                                                )

                                                // Page Number Badge
                                                Surface(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomCenter)
                                                        .padding(bottom = 8.dp),
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = Color.Black.copy(alpha = 0.6f)
                                                ) {
                                                    Text(
                                                        text = "${index + 1} / ${selectedPdfPages.size}",
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        color = Color.White,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                }

                                // File Type Badge
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

                            } else {
                                // Original Image/Camera preview
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
                        } // End of outer Box

                        Text(
                            text = selectedFileName ?: "Unknown file",
                            fontSize = 12.sp,
                            color = RedSecondary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        // Empty Preview State
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

                    // Upload buttons
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Language Selection Button
                    Button(
                        onClick = onLanguageClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = availableLanguagesForSettings.find {
                                it.locale.language == selectedLanguage.language
                            }?.displayName ?: "English 🇺🇸",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Gallery Button
                    Button(
                        onClick = onGalleryClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Favorite, // Consider changing to a more appropriate icon like Icons.Default.PhotoLibrary
                            contentDescription = "My Gallery",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "My Gallery",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
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

            // Error State
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
            selectedPdfPages = emptyList(),
            selectedFileName = null,
            fileType = null,
            prompt = "Explain what's in this document",
            uiState = UiState.Initial,
            selectedLanguage = Locale.US,
            showLanguageDialog = false,
            showPdfErrorDialog = false,
            pdfErrorMessage = "",
            onPromptChange = {},
            onCameraClick = {},
            onImageClick = {},
            onPdfClick = {},
            onAnalyzeClick = {},
            onClearClick = {},
            onQuickActionClick = {},
            onLanguageClick = {},
            onLanguageChange = {},
            onDismissLanguage = {},
            onDismissPdfError = {},
            onLogout = {},
            onGalleryClick = {}
        )
    }
}