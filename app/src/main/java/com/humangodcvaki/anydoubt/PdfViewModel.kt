package com.humangodcvaki.anydoubt

import android.content.Context
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.Locale

data class PdfDocument(
    val uri: Uri,
    val name: String,
    val content: String
)

sealed interface PdfUiState {
    object Initial : PdfUiState
    object Loading : PdfUiState
    data class PdfLoaded(val documents: List<PdfDocument>) : PdfUiState
    data class NotesGenerated(val shortNotes: String, val detailedNotes: String) : PdfUiState
    data class AnswerGenerated(val answer: String) : PdfUiState
    data class Error(val errorMessage: String) : PdfUiState
}

class PdfViewModel : ViewModel() {
    private val _uiState: MutableStateFlow<PdfUiState> = MutableStateFlow(PdfUiState.Initial)
    val uiState: StateFlow<PdfUiState> = _uiState.asStateFlow()

    private val _documents = mutableListOf<PdfDocument>()
    private val _currentNotes = MutableStateFlow("")
    val currentNotes: StateFlow<String> = _currentNotes.asStateFlow()

    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.apiKey
    )

    fun initTextToSpeech(context: Context) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
                isTtsInitialized = true
            }
        }
    }

    fun loadPdfContent(context: Context, uri: Uri, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.value = PdfUiState.Loading
                val content = extractTextFromPdf(context, uri)
                val pdfDoc = PdfDocument(uri, fileName, content)
                _documents.add(pdfDoc)
                _uiState.value = PdfUiState.PdfLoaded(_documents.toList())
            } catch (e: Exception) {
                _uiState.value = PdfUiState.Error(e.localizedMessage ?: "Error loading PDF")
            }
        }
    }

    private fun extractTextFromPdf(context: Context, uri: Uri): String {
        // Simple text extraction - reads the PDF as text
        // For proper PDF parsing, you would use a library like Apache PDFBox or iText
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        return inputStream?.bufferedReader()?.use { it.readText() } ?: ""
    }

    fun generateNotes(noteType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.value = PdfUiState.Loading

                val combinedContent = _documents.joinToString("\n\n") {
                    "Document: ${it.name}\n${it.content}"
                }

                val prompt = when (noteType) {
                    "short" -> "Create concise short notes from the following documents. Use bullet points and highlight key concepts:\n\n$combinedContent"
                    "detailed" -> "Create detailed lecture notes from the following documents. Include explanations, examples, and important details:\n\n$combinedContent"
                    else -> "Summarize the following documents:\n\n$combinedContent"
                }

                val response = generativeModel.generateContent(prompt)

                response.text?.let { notes ->
                    _currentNotes.value = notes
                    _uiState.value = PdfUiState.NotesGenerated(notes, notes)
                }
            } catch (e: Exception) {
                _uiState.value = PdfUiState.Error(e.localizedMessage ?: "Error generating notes")
            }
        }
    }

    fun askQuestion(question: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.value = PdfUiState.Loading

                val combinedContent = _documents.joinToString("\n\n") {
                    "Document: ${it.name}\n${it.content}"
                }

                val prompt = "Based on the following documents, answer this question accurately: $question\n\nDocuments:\n$combinedContent"

                val response = generativeModel.generateContent(prompt)

                response.text?.let { answer ->
                    _uiState.value = PdfUiState.AnswerGenerated(answer)
                }
            } catch (e: Exception) {
                _uiState.value = PdfUiState.Error(e.localizedMessage ?: "Error answering question")
            }
        }
    }

    fun speakText(text: String) {
        if (isTtsInitialized) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
    }

    fun clearDocuments() {
        _documents.clear()
        _currentNotes.value = ""
        _uiState.value = PdfUiState.Initial
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeech?.shutdown()
    }
}