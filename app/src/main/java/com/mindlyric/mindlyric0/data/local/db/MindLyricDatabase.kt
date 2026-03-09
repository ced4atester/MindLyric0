package com.mindlyric.mindlyric0.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mindlyric.mindlyric0.data.local.dao.JournalDao
import com.mindlyric.mindlyric0.data.local.entity.JournalEntryEntity
import com.mindlyric.mindlyric0.data.local.dao.UserDao
import com.mindlyric.mindlyric0.data.local.entity.UserEntity

/**
 * Room veritabanı ana sınıfı.
 * version = 2: users tablosuna email unique index eklendi (şema değişikliği).
 * exportSchema = false: geliştirme aşamasında kapalı; production'da true yapıp
 *   migration yazılmalı ve ksp { arg("room.schemaLocation", ...) } eklenmeli.
 */
@Database(
    entities = [JournalEntryEntity::class, UserEntity::class],
    version = 2,
    exportSchema = false
)
abstract class MindLyricDatabase : RoomDatabase() {

    abstract fun journalDao(): JournalDao
    abstract fun userDao(): UserDao
}
