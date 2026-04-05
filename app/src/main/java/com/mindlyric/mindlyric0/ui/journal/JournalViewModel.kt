package com.mindlyric.mindlyric0.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindlyric.mindlyric0.data.local.entity.JournalEntryEntity
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

    // Yeni günlük kaydı ekler — userId otomatik olarak eklenir
    fun addEntry(text: String, moodLabel: String?) {
        viewModelScope.launch {
            repository.insert(
                JournalEntryEntity(
                    userId = userId,                        // hangi kullanıcıya ait
                    createdAt = System.currentTimeMillis(), // ne zaman yazıldı
                    text = text,                            // günlük metni
                    moodLabel = moodLabel,                  // ruh hali (seçilmediyse null)
                    sentimentScore = null                   // duygu skoru — ileride eklenecek
                )
            )
        }
    }

    // Seçilen günlük kaydını veritabanından siler
    fun deleteEntry(entry: JournalEntryEntity) {
        viewModelScope.launch {
            repository.delete(entry)
        }
    }
}
