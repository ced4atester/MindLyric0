package com.mindlyric.mindlyric0.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mindlyric.mindlyric0.data.local.dao.JournalDao
import com.mindlyric.mindlyric0.data.local.entity.JournalEntryEntity

@Database(
    entities = [JournalEntryEntity::class],
    version = 1,
    exportSchema = true
)
abstract class MindLyricDatabase : RoomDatabase() {

    abstract fun journalDao(): JournalDao
}