package com.mindlyric.mindlyric0.data.repository

import com.mindlyric.mindlyric0.data.local.dao.JournalDao
import com.mindlyric.mindlyric0.data.local.entity.JournalEntryEntity
import kotlinx.coroutines.flow.Flow

class JournalRepository(
    private val dao: JournalDao
) {
    // Sadece belirli kullanıcıya ait günlükleri canlı olarak izler
    // Flow: veritabanı değişince liste otomatik güncellenir
    fun observeByUser(userId: Long): Flow<List<JournalEntryEntity>> = dao.observeByUser(userId)

    // Yeni günlük kaydı ekler
    suspend fun insert(entry: JournalEntryEntity): Long = dao.insert(entry)

    // Günlük kaydını siler
    suspend fun delete(entry: JournalEntryEntity) = dao.delete(entry)

    // Kullanıcının toplam günlük sayısını döner
    suspend fun getJournalCount(userId: Long): Int = dao.getJournalCount(userId)

    // Kullanıcının en sık seçtiği ruh halini döner
    suspend fun getTopMood(userId: Long): String? = dao.getTopMood(userId)
}
