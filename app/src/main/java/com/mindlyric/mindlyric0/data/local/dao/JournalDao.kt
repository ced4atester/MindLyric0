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
    @Query("SELECT * FROM journal_entries WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeByUser(userId: Long): Flow<List<JournalEntryEntity>>

    // Kullanıcının toplam günlük sayısını döner
    @Query("SELECT COUNT(*) FROM journal_entries WHERE userId = :userId")
    suspend fun getJournalCount(userId: Long): Int

    // Kullanıcının en sık seçtiği ruh halini döner (null olabilir)
    @Query("SELECT moodLabel FROM journal_entries WHERE userId = :userId AND moodLabel IS NOT NULL GROUP BY moodLabel ORDER BY COUNT(*) DESC LIMIT 1")
    suspend fun getTopMood(userId: Long): String?

    // Claude API'den gelen duygu skorunu belirli kayıda yazar
    @Query("UPDATE journal_entries SET sentimentScore = :score WHERE id = :entryId")
    suspend fun updateSentimentScore(entryId: Long, score: Float)

    // Son 7 günün günlüklerini getirir — grafik için kullanılır
    // since: 7 gün öncesinin timestamp'i
    @Query("SELECT * FROM journal_entries WHERE userId = :userId AND createdAt >= :since AND sentimentScore IS NOT NULL ORDER BY createdAt ASC")
    suspend fun getEntriesSince(userId: Long, since: Long): List<JournalEntryEntity>
}