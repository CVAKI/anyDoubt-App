package com.humangodcvaki.anydoubt

import android.content.Intent
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.humangodcvaki.anydoubt.ui.theme.AnyDoubtTheme
import kotlinx.coroutines.launch
import java.util.*


// Custom colors - MUST BE PRIVATE to avoid conflicts
private val RedPrimary = Color(0xFFF02828)
private val RedSecondary = Color(0xFFD67474)
private val RedLight = Color(0xFFFFE5E5)

// Language options
data class LanguageOption(val name: String, val locale: Locale, val displayName: String)

val availableLanguages = listOf(
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

// Helper function to detect if user is asking for a diagram/image
fun isDiagramRequest(question: String): Boolean {
    val diagramKeywords = listOf(
        "diagram", "image", "picture", "graph", "chart", "illustration",
        "figure", "visual", "drawing", "sketch", "flowchart", "show me",
        "photo", "graphic", "map", "plot"
    )
    return diagramKeywords.any { question.lowercase().contains(it) }
}

// Data class for image search result
data class ImageSearchResult(
    val imageUrl: String,
    val title: String,
    val source: String
)

// Function to search for images using Google Custom Search API or Bing API
suspend fun searchDiagramImages(query: String): List<ImageSearchResult> {
    return withContext(Dispatchers.IO) {
        try {
            // Using Bing Image Search API (you can also use Google Custom Search)
            val searchQuery = "$query diagram illustration"
            val apiKey = "YOUR_BING_API_KEY_HERE" // Get free key from Azure
            val url = "https://api.bing.microsoft.com/v7.0/images/search?q=${
                java.net.URLEncoder.encode(searchQuery, "UTF-8")
            }&count=3&imageType=Photo"

            val connection = URL(url).openConnection() as HttpURLConnection
            connection.setRequestProperty("Ocp-Apim-Subscription-Key", apiKey)
            connection.requestMethod = "GET"

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonObject = org.json.JSONObject(response)
                val results = mutableListOf<ImageSearchResult>()

                val values = jsonObject.getJSONArray("value")
                for (i in 0 until minOf(3, values.length())) {
                    val item = values.getJSONObject(i)
                    results.add(
                        ImageSearchResult(
                            imageUrl = item.getString("contentUrl"),
                            title = item.getString("name"),
                            source = item.getString("hostPageUrl")
                        )
                    )
                }
                results
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}

class AnswerActivity : ComponentActivity() {
    private lateinit var textToSpeech: TextToSpeech
    private var isTtsReady = false
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = AppDatabase.getDatabase(this)

        // Get and clean the analysis result - remove ** and * symbols
        val rawResult = intent.getStringExtra("ANALYSIS_RESULT") ?: "No result available"
        val cleanedResult = rawResult
            .replace("**", "")
            .replace("*", "")
            .trim()

        // Initialize TTS
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
            }
        }

        setContent {
            var currentLocale by remember { mutableStateOf(Locale.US) }
            var translatedResult by remember { mutableStateOf(cleanedResult) }
            var showSaveDialog by remember { mutableStateOf(false) }

            AnyDoubtTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    AnswerScreen(
                        analysisResult = translatedResult,
                        originalContent = cleanedResult,
                        currentLanguage = currentLocale,
                        onLanguageChange = { locale ->
                            currentLocale = locale
                            val result = textToSpeech.setLanguage(locale)
                            val success = result != TextToSpeech.LANG_MISSING_DATA &&
                                    result != TextToSpeech.LANG_NOT_SUPPORTED
                            if (!success) {
                                Toast.makeText(
                                    this@AnswerActivity,
                                    "Language not supported on your device",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                translatedResult = translateText(cleanedResult, locale)
                                Toast.makeText(
                                    this@AnswerActivity,
                                    "Language changed successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onReadAloud = { text ->
                            if (isTtsReady) {
                                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                            } else {
                                Toast.makeText(
                                    this@AnswerActivity,
                                    "Text-to-Speech not ready yet",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onStopReading = {
                            if (textToSpeech.isSpeaking) {
                                textToSpeech.stop()
                            }
                        },
                        onSaveNote = {
                            showSaveDialog = true
                        },
                        showSaveDialog = showSaveDialog,
                        onDismissSaveDialog = { showSaveDialog = false },
                        onConfirmSave = { title ->
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                val note = SavedNote(
                                    title = title,
                                    content = translatedResult,
                                    timestamp = System.currentTimeMillis(),
                                    language = currentLocale.displayLanguage
                                )
                                database.noteDao().insertNote(note)

                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    Toast.makeText(
                                        this@AnswerActivity,
                                        "Note saved successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    showSaveDialog = false
                                }
                            }
                        },
                        onBack = {
                            if (textToSpeech.isSpeaking) {
                                textToSpeech.stop()
                            }
                            val intent = Intent(this, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }
                            startActivity(intent)
                            finish()
                        }
                    )
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed() // Added super call
        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun translateText(text: String, locale: Locale): String {
        return when (locale.language) {
            "hi" -> text
            "ml" -> text
            "es" -> text
            else -> text
        }
    }

    override fun onDestroy() {
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnswerScreen(
    analysisResult: String,
    originalContent: String,
    currentLanguage: Locale,
    onLanguageChange: (Locale) -> Unit,
    onReadAloud: (String) -> Unit,
    onStopReading: () -> Unit,
    onSaveNote: () -> Unit,
    showSaveDialog: Boolean,
    onDismissSaveDialog: () -> Unit,
    onConfirmSave: (String) -> Unit,
    onBack: () -> Unit
) {
    var isReading by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var question by remember { mutableStateOf("") }
    var qaHistory by remember { mutableStateOf<List<Triple<String, String, List<ImageSearchResult>?>>>(emptyList()) }
    var isProcessingQuestion by remember { mutableStateOf(false) }
    var noteTitle by remember { mutableStateOf("") }

    val qaViewModel: BakingViewModel = viewModel()
    val scope = rememberCoroutineScope()

    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val audioWave by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "audioWave"
    )

    // Save Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = onDismissSaveDialog,
            icon = {
                Icon(
                    Icons.Default.Create,
                    contentDescription = null,
                    tint = RedPrimary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Save Note",
                    fontWeight = FontWeight.Bold,
                    color = RedPrimary
                )
            },
            text = {
                Column {
                    Text(
                        "Enter a title for this note:",
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        placeholder = { Text("e.g., Math Notes - Quadratic Equations") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RedPrimary,
                            cursorColor = RedPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noteTitle.isNotBlank()) {
                            onConfirmSave(noteTitle)
                            noteTitle = ""
                        }
                    },
                    enabled = noteTitle.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onDismissSaveDialog()
                    noteTitle = ""
                }) {
                    Text("Cancel", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
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
                        "Select Voice Language",
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
                    availableLanguages.forEach { lang ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            onClick = {
                                onLanguageChange(lang.locale)
                                showLanguageDialog = false
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (lang.locale.language == currentLanguage.language)
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
                                    fontWeight = if (lang.locale.language == currentLanguage.language)
                                        FontWeight.Bold
                                    else
                                        FontWeight.Normal,
                                    color = if (lang.locale.language == currentLanguage.language)
                                        RedPrimary
                                    else
                                        Color.DarkGray
                                )
                                if (lang.locale.language == currentLanguage.language) {
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
                TextButton(onClick = { showLanguageDialog = false }) {
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
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "anyDoubt",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RedPrimary
                ),
                actions = {
                    // Save button
                    IconButton(onClick = onSaveNote) {
                        Icon(
                            Icons.Default.Create,
                            contentDescription = "Save Note",
                            tint = Color.White
                        )
                    }
                    // Language indicator button (white square)
                    Surface(
                        onClick = { showLanguageDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = availableLanguages.find { it.locale.language == currentLanguage.language }?.locale?.language?.uppercase() ?: "EN",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            fontWeight = FontWeight.Bold,
                            color = RedPrimary,
                            fontSize = 14.sp
                        )
                    }
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                AnimatedVisibility(
                    visible = isReading,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = RedPrimary.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = RedPrimary,
                                modifier = Modifier
                                    .size(32.dp)
                                    .scale(audioWave)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "🎧 Audio Assistant Active",
                                    fontWeight = FontWeight.Bold,
                                    color = RedPrimary,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "Reading in ${availableLanguages.find { it.locale.language == currentLanguage.language }?.displayName ?: "English 🇺🇸"}",
                                    fontSize = 12.sp,
                                    color = RedSecondary
                                )
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "📚 Teacher Explanation",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = RedPrimary
                            )
                            Surface(
                                shape = CircleShape,
                                color = RedPrimary.copy(alpha = 0.1f)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = RedPrimary,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(20.dp)
                                )
                            }
                        }

                        HorizontalDivider(
                            color = RedSecondary.copy(alpha = 0.3f),
                            thickness = 2.dp,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )

                        FormattedExplanation(analysisResult)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "💬 Ask anyDoubt & meaning",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedPrimary
                        )

                        Text(
                            "Have doubts? Ask anything about this topic!",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        OutlinedTextField(
                            value = question,
                            onValueChange = { question = it },
                            placeholder = { Text("Ask anything......", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = RedPrimary)
                            },
                            trailingIcon = {
                                if (question.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            if (question.isNotBlank() && !isProcessingQuestion) {
                                                isProcessingQuestion = true
                                                val userQuestion = question
                                                question = ""

                                                scope.launch {
                                                    // Check if user is asking for a diagram
                                                    if (isDiagramRequest(userQuestion)) {
                                                        // Search for diagram images
                                                        val images = searchDiagramImages(userQuestion)

                                                        if (images.isNotEmpty()) {
                                                            val answer = "Here are some relevant diagrams I found:"
                                                            qaHistory = qaHistory + Triple(userQuestion, answer, images)
                                                        } else {
                                                            qaHistory = qaHistory + Triple(
                                                                userQuestion,
                                                                "Sorry, I couldn't find relevant diagrams. Could you try rephrasing your request?",
                                                                null
                                                            )
                                                        }
                                                    } else {
                                                        // Regular text-based answer
                                                        val contextPrompt = """
                                    Based on this explanation:
                                    
                                    $originalContent
                                    
                                    Answer this follow-up question briefly and clearly:
                                    $userQuestion
                                    
                                    Keep the answer concise and directly related to the content above.
                                """.trimIndent()

                                                        val answer = qaViewModel.askFollowUpQuestion(contextPrompt)
                                                        qaHistory = qaHistory + Triple(userQuestion, answer, null)
                                                    }
                                                    isProcessingQuestion = false
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = "Send", tint = RedPrimary)
                                    }
                                }
                            },
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RedPrimary,
                                unfocusedBorderColor = Color.LightGray,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                cursorColor = RedPrimary
                            )
                        )

                        if (isProcessingQuestion) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = RedPrimary,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Processing your question...",
                                    fontSize = 14.sp,
                                    color = RedSecondary
                                )
                            }
                        }

                        if (qaHistory.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = RedSecondary.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(16.dp))

                            qaHistory.forEachIndexed { index, (q, a, images) ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn() + slideInVertically()
                                ) {
                                    Column {
                                        // User Question Card
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(0.85f),
                                                shape = RoundedCornerShape(
                                                    topStart = 16.dp,
                                                    topEnd = 16.dp,
                                                    bottomStart = 16.dp,
                                                    bottomEnd = 4.dp
                                                ),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = RedPrimary
                                                )
                                            ) {
                                                Text(
                                                    text = q,
                                                    modifier = Modifier.padding(12.dp),
                                                    color = Color.White,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Answer Card with potential diagrams
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Start
                                        ) {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(0.85f),
                                                shape = RoundedCornerShape(
                                                    topStart = 16.dp,
                                                    topEnd = 16.dp,
                                                    bottomStart = 4.dp,
                                                    bottomEnd = 16.dp
                                                ),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = RedLight.copy(alpha = 0.5f)
                                                )
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(verticalAlignment = Alignment.Top) {
                                                        Icon(
                                                            Icons.Default.Star,
                                                            contentDescription = null,
                                                            tint = RedPrimary,
                                                            modifier = Modifier
                                                                .size(20.dp)
                                                                .padding(top = 2.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = a,
                                                            color = Color.DarkGray,
                                                            fontSize = 14.sp
                                                        )
                                                    }

                                                    // Display diagrams if available
                                                    images?.let { imageList ->
                                                        if (imageList.isNotEmpty()) {
                                                            Spacer(modifier = Modifier.height(12.dp))
                                                            DiagramDisplay(images = imageList)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (index < qaHistory.size - 1) {
                                            Spacer(modifier = Modifier.height(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // FloatingActionButton with emoji
        FloatingActionButton(
            onClick = {
                if (isReading) {
                    onStopReading()
                    isReading = false
                } else {
                    onReadAloud(analysisResult)
                    isReading = true
                }
            },
            containerColor = RedPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(56.dp)
        ) {
            if (isReading) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Stop Reading",
                    tint = Color.White,
                    modifier = Modifier.scale(audioWave)
                )
            } else {
                Text(
                    text = "🗣",
                    fontSize = 32.sp,
                    color = Color.White
                )
            }
        }
    }
}
@Composable
fun FormattedExplanation(text: String) {
    val lines = text.split("\n")

    Column {
        lines.forEach { line ->
            when {
                line.trim().endsWith(":") -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = line.trim(),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        color = RedPrimary,
                        lineHeight = 28.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                line.trim().matches(Regex("^\\d+\\..*")) -> {
                    Row(
                        modifier = Modifier.padding(start = 8.dp, top = 6.dp, bottom = 6.dp)
                    ) {
                        Text(
                            text = line.trim(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray,
                            lineHeight = 23.sp
                        )
                    }
                }
                line.trim().startsWith("-") || line.trim().startsWith("•") -> {
                    Row(
                        modifier = Modifier.padding(start = 8.dp, top = 6.dp, bottom = 6.dp)
                    ) {
                        Text(
                            text = line.trim(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray,
                            lineHeight = 23.sp
                        )
                    }
                }
                line.isNotBlank() -> {
                    Text(
                        text = line.trim(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.DarkGray,
                        lineHeight = 23.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                else -> {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun DiagramDisplay(images: List<ImageSearchResult>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        images.forEach { imageResult ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    // Display the image using Coil
                    AsyncImage(
                        model = imageResult.imageUrl,
                        contentDescription = imageResult.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Fit
                    )

                    // Image title and source
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = imageResult.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Source: ${imageResult.source}",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AnswerScreenPreview() {
    AnyDoubtTheme {
        AnswerScreen(
            analysisResult = "The first law of thermodynamics is a statement of the conservation of energy.",
            originalContent = "The first law of thermodynamics is a statement of the conservation of energy.",
            currentLanguage = Locale.US,
            onLanguageChange = {},
            onReadAloud = {},
            onStopReading = {},
            onSaveNote = {},
            showSaveDialog = false,
            onDismissSaveDialog = {},
            onConfirmSave = { _ -> },
            onBack = {}
        )
    }
}