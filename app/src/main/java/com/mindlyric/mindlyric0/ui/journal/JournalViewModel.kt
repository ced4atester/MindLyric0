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

    // UI bununla DB'deki entry listesini canlı izler
    val entries: StateFlow<List<JournalEntryEntity>> =
        repository.observeAll()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun addDummyEntry() {
        viewModelScope.launch {
            repository.insert(
                JournalEntryEntity(
                    createdAt = System.currentTimeMillis(),
                    text = "MVVM üzerinden eklendi ✅",
                    moodLabel = "Mutlu",
                    sentimentScore = 0.85f
                )
            )
        }
    }

    fun deleteEntry(entry: JournalEntryEntity) {
        viewModelScope.launch {
            repository.delete(entry)
        }
    }
}
