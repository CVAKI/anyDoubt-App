package com.humangodcvaki.anydoubt

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humangodcvaki.anydoubt.ui.theme.AnyDoubtTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private val RedPrimary = Color(0xFFF02828)
private val RedSecondary = Color(0xFFD67474)
private val RedLight = Color(0xFFFFE5E5)

class GalleryActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var adMobHelper: AdMobHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = AppDatabase.getDatabase(this)

        // Initialize AdMob Helper
        adMobHelper = AdMobHelper(this)

        setContent {
            AnyDoubtTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    GalleryScreen(
                        database = database,
                        onNoteClick = { noteId ->
                            // Show ad before opening note detail
                            adMobHelper.showInterstitialAd(this) {
                                // Navigate after ad is dismissed
                                val intent = Intent(this, NoteDetailActivity::class.java)
                                intent.putExtra("NOTE_ID", noteId)
                                startActivity(intent)
                            }
                        },
                        onBack = {
                            // Show ad before going back
                            adMobHelper.showInterstitialAd(this) {
                                finish()
                            }
                        }
                    )
                }
            }
        }

        // Preload ad when gallery opens
        adMobHelper.preloadAd()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    database: AppDatabase,
    onNoteClick: (Int) -> Unit,
    onBack: () -> Unit
) {
    val notes by database.noteDao().getAllNotes().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var noteToDelete by remember { mutableStateOf<SavedNote?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Delete confirmation dialog
    if (showDeleteDialog && noteToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = RedPrimary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Delete Note?",
                    fontWeight = FontWeight.Bold,
                    color = RedPrimary
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete this note? This action cannot be undone.",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            noteToDelete?.let { database.noteDao().deleteNote(it) }
                            showDeleteDialog = false
                            noteToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    noteToDelete = null
                }) {
                    Text("Cancel", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

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
                            "My Gallery",
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
                )
            )

            // Content
            if (notes.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = RedSecondary,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No Saved Notes Yet",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Save notes from Answer screen to see them here",
                        fontSize = 14.sp,
                        color = RedSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Notes list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onClick = { onNoteClick(note.id) },
                            onDelete = {
                                noteToDelete = note
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NoteCard(
    note: SavedNote,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(note.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = RedPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = note.content,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            tint = RedSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formattedDate,
                            fontSize = 11.sp,
                            color = RedSecondary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RedLight.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = note.language,
                            fontSize = 10.sp,
                            color = RedPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFFEBEE))
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = RedPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NoteCardPreview() {
    AnyDoubtTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(16.dp)
        ) {
            NoteCard(
                note = SavedNote(
                    id = 1,
                    title = "Quadratic Equations",
                    content = "A quadratic equation is a polynomial equation of degree 2. The general form is ax² + bx + c = 0.\n\nKey Properties:\n\n1. It always has two solutions (roots)\n2. The graph forms a parabola",
                    timestamp = System.currentTimeMillis(),
                    language = "English"
                ),
                onClick = {},
                onDelete = {}
            )
        }
    }
}