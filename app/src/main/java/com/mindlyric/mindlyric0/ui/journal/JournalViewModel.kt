package com.mindlyric.mindlyric0.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindlyric.mindlyric0.data.local.entity.JournalEntryEntity
import com.mindlyric.mindlyric0.data.remote.ClaudeService
import com.mindlyric.mindlyric0.data.repository.JournalRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ViewModel artık hangi kullanıcının günlüklerini göstereceğini bilmek için userId alıyor
class JournalViewModel(
    private val repository: JournalRepository,
    private val userId: Long       // giriş yapan kullanıcının id'si
) : ViewModel() {

    // Sadece bu kullanıcıya ait günlükleri canlı olarak dinler
    // WhileSubscribed(5000): ekran kapandıktan 5 sn sonra dinlemeyi durdurur (pil tasarrufu)
    val entries: StateFlow<List<JournalEntryEntity>> =
        repository.observeByUser(userId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    // Yeni günlük kaydı ekler — önce veritabanına yazar, sonra Claude API ile duygu analizi yapar
    fun addEntry(text: String, moodLabel: String?) {
        viewModelScope.launch {
            // 1. Adım: Önce skorsuz olarak kaydet (kullanıcı beklemeden görür)
            val entryId = repository.insert(
                JournalEntryEntity(
                    userId = userId,
                    createdAt = System.currentTimeMillis(),
                    text = text,
                    moodLabel = moodLabel,
                    sentimentScore = null               // henüz analiz edilmedi
                )
            )

            // 2. Adım: Claude API ile duygu analizini arka planda yap
            val score = ClaudeService.analyzeSentiment(text)

            // 3. Adım: Skor gelirse veritabanındaki kaydı güncelle
            if (score != null) {
                repository.updateSentimentScore(entryId, score)
            }
        }
    }

    // Seçilen günlük kaydını veritabanından siler
    fun deleteEntry(entry: JournalEntryEntity) {
        viewModelScope.launch {
            repository.delete(entry)
        }
    }
}
