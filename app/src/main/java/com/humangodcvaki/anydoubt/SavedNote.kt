package com.humangodcvaki.anydoubt

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_notes")
data class SavedNote(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    val timestamp: Long,
    val language: String
)