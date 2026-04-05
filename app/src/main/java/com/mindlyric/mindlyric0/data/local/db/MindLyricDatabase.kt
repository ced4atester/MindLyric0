package com.mindlyric.mindlyric0.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mindlyric.mindlyric0.data.local.dao.JournalDao
import com.mindlyric.mindlyric0.data.local.entity.JournalEntryEntity
import com.mindlyric.mindlyric0.data.local.dao.UserDao
import com.mindlyric.mindlyric0.data.local.entity.UserEntity

/**
 * Room veritabanı ana sınıfı.
 * version = 3: journal_entries tablosuna userId sütunu eklendi.
 *   (v2 → v3: günlükler artık kullanıcıya özel)
 * fallbackToDestructiveMigration kullanıldığı için uygulama silinip yeniden kurulmalı.
 */
@Database(
    entities = [JournalEntryEntity::class, UserEntity::class],
    version = 3,
    exportSchema = false
)
abstract class MindLyricDatabase : RoomDatabase() {

    abstract fun journalDao(): JournalDao
    abstract fun userDao(): UserDao
}
