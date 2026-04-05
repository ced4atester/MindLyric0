package com.mindlyric.mindlyric0.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.mindlyric.mindlyric0.data.local.entity.JournalEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {

    // Yeni günlük kaydı ekler, eklenen kaydın id'sini döner
    @Insert
    suspend fun insert(entry: JournalEntryEntity): Long

    // Belirtilen kaydı veritabanından siler
    @Delete
    suspend fun delete(entry: JournalEntryEntity)

    // Sadece belirli kullanıcıya ait kayıtları getirir, en yeniden eskiye sıralar
    // :userId → dışarıdan gelen parametre, SQL injection'a karşı güvenli
    @Query("SELECT * FROM journal_entries WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeByUser(userId: Long): Flow<List<JournalEntryEntity>>
}