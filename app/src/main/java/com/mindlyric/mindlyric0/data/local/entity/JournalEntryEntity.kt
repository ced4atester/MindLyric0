package com.mindlyric.mindlyric0.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val createdAt: Long,

    val text: String,

    val moodLabel: String?,

    val sentimentScore: Float?
)
