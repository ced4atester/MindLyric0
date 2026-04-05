package com.mindlyric.mindlyric0.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mindlyric.mindlyric0.data.local.dao.JournalDao
import com.mindlyric.mindlyric0.data.local.entity.JournalEntryEntity
import com.mindlyric.mindlyric0.data.local.dao.UserDao
import com.mindlyric.mindlyric0.data.local.entity.UserEntity

/**
 * Room veritabanı ana sınıfı.
 * version = 5: journal_entries tablosuna recommendation sütunu eklendi.
 *   (v4 → v5: Claude'un kişisel öneri mesajı artık saklanıyor)
 * fallbackToDestructiveMigration kullanıldığı için uygulama silinip yeniden kurulmalı.
 */
@Database(
    entities = [JournalEntryEntity::class, UserEntity::class],
    version = 5,
    exportSchema = false
)
abstract class MindLyricDatabase : RoomDatabase() {

    abstract fun journalDao(): JournalDao
    abstract fun userDao(): UserDao
}
