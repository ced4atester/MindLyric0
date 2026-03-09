package com.mindlyric.mindlyric0.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mindlyric.mindlyric0.data.local.dao.JournalDao
import com.mindlyric.mindlyric0.data.local.entity.JournalEntryEntity
import com.mindlyric.mindlyric0.data.local.dao.UserDao
import com.mindlyric.mindlyric0.data.local.entity.UserEntity

@Database(
    entities = [JournalEntryEntity::class, UserEntity::class],
    version = 1,
    exportSchema = true
)
abstract class MindLyricDatabase : RoomDatabase() {

    abstract fun journalDao(): JournalDao
    abstract fun userDao(): UserDao
}