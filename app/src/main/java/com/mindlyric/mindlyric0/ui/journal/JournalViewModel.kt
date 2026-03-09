package com.mindlyric.mindlyric0.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindlyric.mindlyric0.data.local.entity.JournalEntryEntity
import com.mindlyric.mindlyric0.data.repository.JournalRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JournalViewModel(
    private val repository: JournalRepository
) : ViewModel() {

    /**
     * Veritabanındaki tüm günlük girdilerini canlı olarak izler.
     * StateFlow: Activity/Fragment yaşam döngüsüne duyarlı.
     * WhileSubscribed(5000): ekran kapandıktan 5 saniye sonra akış durur → kaynak tasarrufu.
     */
    val entries: StateFlow<List<JournalEntryEntity>> =
        repository.observeAll()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    /**
     * Yeni günlük girdisi ekler.
     * @param text      Kullanıcının yazdığı günlük metni
     * @param moodLabel Seçilen ruh hali etiketi; seçilmezse null
     */
    fun addEntry(text: String, moodLabel: String?) {
        viewModelScope.launch {
            repository.insert(
                JournalEntryEntity(
                    createdAt = System.currentTimeMillis(), // kayıt anı (epoch ms)
                    text = text,
                    moodLabel = moodLabel,
                    sentimentScore = null // ileride duygu analizi eklenebilir
                )
            )
        }
    }

    // Girdiyi veritabanından kalıcı olarak siler
    fun deleteEntry(entry: JournalEntryEntity) {
        viewModelScope.launch {
            repository.delete(entry)
        }
    }
}
