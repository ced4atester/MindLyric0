package com.mindlyric.mindlyric0.data.repository

import com.mindlyric.mindlyric0.data.local.dao.JournalDao
import com.mindlyric.mindlyric0.data.local.entity.JournalEntryEntity
import kotlinx.coroutines.flow.Flow

class JournalRepository(
    private val dao: JournalDao
) {
    // DB'deki tüm journal kayıtlarını canlı şekilde izler (liste ekranı için ideal)
    fun observeAll(): Flow<List<JournalEntryEntity>> = dao.observeAll()

    // Yeni kayıt ekler
    suspend fun insert(entry: JournalEntryEntity): Long = dao.insert(entry)

    // Kayıt siler
    suspend fun delete(entry: JournalEntryEntity) = dao.delete(entry)
}
