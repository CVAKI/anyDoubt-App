package com.humangodcvaki.anydoubt

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM saved_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<SavedNote>>

    @Query("SELECT * FROM saved_notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: Int): SavedNote?

    @Insert
    suspend fun insertNote(note: SavedNote): Long

    @Delete
    suspend fun deleteNote(note: SavedNote)

    @Query("DELETE FROM saved_notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Int)
}