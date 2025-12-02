package com.humangodcvaki.anydoubt

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
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private val RedPrimary = Color(0xFFF02828)
private val RedSecondary = Color(0xFFD67474)
private val RedLight = Color(0xFFFFE5E5)

class NoteDetailActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var textToSpeech: TextToSpeech
    private var isTtsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = AppDatabase.getDatabase(this)

        val noteId = intent.getIntExtra("NOTE_ID", -1)

        // Initialize TTS
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
            }
        }

        setContent {
            var isReading by remember { mutableStateOf(false) }
            var currentLocale by remember { mutableStateOf(Locale.US) }

            AnyDoubtTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    if (noteId != -1) {
                        NoteDetailScreen(
                            database = database,
                            noteId = noteId,
                            currentLanguage = currentLocale,
                            isReading = isReading,
                            onLanguageChange = { locale ->
                                currentLocale = locale
                                val result = textToSpeech.setLanguage(locale)
                                val success = result != TextToSpeech.LANG_MISSING_DATA &&
                                        result != TextToSpeech.LANG_NOT_SUPPORTED
                                if (!success) {
                                    Toast.makeText(
                                        this@NoteDetailActivity,
                                        "Language not supported on your device",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        this@NoteDetailActivity,
                                        "Language changed successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onReadAloud = { text ->
                                if (isTtsReady) {
                                    textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                                    isReading = true
                                } else {
                                    Toast.makeText(
                                        this@NoteDetailActivity,
                                        "Text-to-Speech not ready yet",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onStopReading = {
                                if (textToSpeech.isSpeaking) {
                                    textToSpeech.stop()
                                }
                                isReading = false
                            },
                            onBack = {
                                if (textToSpeech.isSpeaking) {
                                    textToSpeech.stop()
                                }
                                finish()
                            }
                        )
                    }
                }
            }
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
fun NoteDetailScreen(
    database: AppDatabase,
    noteId: Int,
    currentLanguage: Locale,
    isReading: Boolean,
    onLanguageChange: (Locale) -> Unit,
    onReadAloud: (String) -> Unit,
    onStopReading: () -> Unit,
    onBack: () -> Unit
) {
    var note by remember { mutableStateOf<SavedNote?>(null) }
    var showLanguageDialog by remember { mutableStateOf(false) }

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

    LaunchedEffect(noteId) {
        note = database.noteDao().getNoteById(noteId)
    }

    if (note == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = RedPrimary)
        }
    } else {
        NoteDetailScreenContent(
            note = note!!,
            isReading = isReading,
            audioWave = audioWave,
            currentLanguage = currentLanguage,
            showLanguageDialog = showLanguageDialog,
            onLanguageDialogChange = { showLanguageDialog = it },
            onReadAloudToggle = {
                if (isReading) {
                    onStopReading()
                } else {
                    onReadAloud(note!!.content)
                }
            },
            onBack = onBack
        )

        // Language Dialog
        if (showLanguageDialog) {
            LanguageSelectionDialog(
                currentLanguage = currentLanguage,
                onLanguageSelected = { locale ->
                    onLanguageChange(locale)
                    showLanguageDialog = false
                },
                onDismiss = { showLanguageDialog = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    database: AppDatabase,
    noteId: Int,
    textToSpeech: TextToSpeech,
    isTtsReady: Boolean,
    onBack: () -> Unit
) {
    var note by remember { mutableStateOf<SavedNote?>(null) }
    var isReading by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var currentLanguage by remember { mutableStateOf(Locale.US) }

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

    LaunchedEffect(noteId) {
        note = database.noteDao().getNoteById(noteId)
    }

    if (note == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = RedPrimary)
        }
    } else {
        NoteDetailScreenContent(
            note = note!!,
            isReading = isReading,
            audioWave = audioWave,
            currentLanguage = currentLanguage,
            showLanguageDialog = showLanguageDialog,
            onLanguageDialogChange = { showLanguageDialog = it },
            onReadAloudToggle = {
                if (isTtsReady) {
                    if (isReading) {
                        textToSpeech.stop()
                        isReading = false
                    } else {
                        textToSpeech.speak(note!!.content, TextToSpeech.QUEUE_FLUSH, null, null)
                        isReading = true
                    }
                }
            },
            onBack = onBack
        )

        // Language Dialog
        if (showLanguageDialog) {
            LanguageSelectionDialog(
                currentLanguage = currentLanguage,
                onLanguageSelected = { locale ->
                    currentLanguage = locale
                    textToSpeech.setLanguage(locale)
                    showLanguageDialog = false
                },
                onDismiss = { showLanguageDialog = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreenContent(
    note: SavedNote,
    isReading: Boolean,
    audioWave: Float,
    currentLanguage: Locale,
    showLanguageDialog: Boolean,
    onLanguageDialogChange: (Boolean) -> Unit,
    onReadAloudToggle: () -> Unit,
    onBack: () -> Unit
) {
    var question by remember { mutableStateOf("") }
    var qaHistory by remember { mutableStateOf<List<Triple<String, String, List<ImageSearchResult>?>>>(emptyList()) }
    var isProcessingQuestion by remember { mutableStateOf(false) }

    val qaViewModel: BakingViewModel = viewModel()
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
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
                            "My Saved Note",
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
                    // Language indicator button (white square)
                    Surface(
                        onClick = { onLanguageDialogChange(true) },
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
                // Audio Reading Indicator
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

                // Main Content Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📚 ${note.title}",
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

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = RedSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                        .format(Date(note.timestamp)),
                                    fontSize = 12.sp,
                                    color = RedSecondary
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = RedPrimary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = note.language,
                                    fontSize = 12.sp,
                                    color = RedPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        HorizontalDivider(
                            color = RedSecondary.copy(alpha = 0.3f),
                            thickness = 2.dp,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )

                        FormattedExplanation(note.content)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Ask anyDoubt Section
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
                            placeholder = { Text("Ask anything or request diagrams...", color = Color.Gray) },
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
                                                            Based on this saved note:
                                                            
                                                            ${note.content}
                                                            
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
            onClick = onReadAloudToggle,
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
fun LanguageSelectionDialog(
    currentLanguage: Locale,
    onLanguageSelected: (Locale) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                        onClick = { onLanguageSelected(lang.locale) },
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
            TextButton(onClick = onDismiss) {
                Text("Close", color = RedPrimary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun NoteDetailScreenPreview() {
    AnyDoubtTheme {
        NoteDetailScreenContent(
            note = SavedNote(
                id = 1,
                title = "Quadratic Equations - Mathematics",
                content = """
                    Understanding Quadratic Equations:
                    
                    A quadratic equation is a second-degree polynomial equation in a single variable x.
                    
                    Standard Form:
                    ax² + bx + c = 0
                    
                    Key Points:
                    1. The highest power of the variable is 2
                    2. It always has two solutions (roots)
                    3. The graph is a parabola
                    
                    Methods to Solve:
                    - Factoring method
                    - Quadratic formula
                    - Completing the square
                    - Graphical method
                    
                    The discriminant (b² - 4ac) determines the nature of roots.
                """.trimIndent(),
                timestamp = System.currentTimeMillis(),
                language = "English"
            ),
            isReading = false,
            audioWave = 1f,
            currentLanguage = Locale.US,
            showLanguageDialog = false,
            onLanguageDialogChange = {},
            onReadAloudToggle = {},
            onBack = {}
        )
    }
}