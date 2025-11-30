package com.humangodcvaki.anydoubt

import android.content.Intent
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

class AnswerActivity : ComponentActivity() {
    private lateinit var textToSpeech: TextToSpeech
    private var isTtsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
    onBack: () -> Unit
) {
    var isReading by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var question by remember { mutableStateOf("") }
    var qaHistory by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isProcessingQuestion by remember { mutableStateOf(false) }

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
                            "anyDoubt Result",
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
                    IconButton(onClick = { showLanguageDialog = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Language Settings",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = {
                            if (isReading) {
                                onStopReading()
                                isReading = false
                            } else {
                                onReadAloud(analysisResult)
                                isReading = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isReading) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = if (isReading) "Stop" else "Read Aloud",
                            tint = Color.White,
                            modifier = if (isReading) Modifier.scale(audioWave) else Modifier
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
                            "💬 Ask Follow-up Questions",
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
                            placeholder = { Text("Type your question here...", color = Color.Gray) },
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
                                                    val contextPrompt = """
                                                        Based on this explanation:
                                                        
                                                        $originalContent
                                                        
                                                        Answer this follow-up question briefly and clearly:
                                                        $userQuestion
                                                        
                                                        Keep the answer concise and directly related to the content above.
                                                    """.trimIndent()

                                                    val answer = qaViewModel.askFollowUpQuestion(contextPrompt)
                                                    qaHistory = qaHistory + Pair(userQuestion, answer)
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

                            qaHistory.forEachIndexed { index, (q, a) ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn() + slideInVertically()
                                ) {
                                    Column {
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
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.Top
                                                ) {
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
            Icon(
                imageVector = if (isReading) Icons.Default.Close else Icons.Default.PlayArrow,
                contentDescription = if (isReading) "Stop Reading" else "Read Aloud",
                tint = Color.White,
                modifier = if (isReading) Modifier.scale(audioWave) else Modifier
            )
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AnswerScreenPreview() {
    AnyDoubtTheme {
        AnswerScreen(
            analysisResult = "Understanding Quadratic Equations:\n\nA quadratic equation is a polynomial equation of degree 2. The general form is ax² + bx + c = 0.\n\nKey Properties:\n\n1. It always has two solutions (roots)\n2. The graph forms a parabola\n3. Can be solved using multiple methods\n\nSolution Methods:\n\n1. Factoring method - Break down into factors\n2. Completing the square - Rearrange to perfect square form\n3. Quadratic formula: x = (-b ± √(b² - 4ac)) / 2a\n\nThe discriminant (b² - 4ac) tells us about the nature of roots.",
            originalContent = "Understanding Quadratic Equations:\n\nA quadratic equation is a polynomial equation of degree 2. The general form is ax² + bx + c = 0.",
            currentLanguage = Locale.US,
            onLanguageChange = {},
            onReadAloud = {},
            onStopReading = {},
            onBack = {}
        )
    }
}