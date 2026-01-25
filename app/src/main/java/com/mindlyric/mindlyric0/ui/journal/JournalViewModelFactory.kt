package com.mindlyric.mindlyric0.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mindlyric.mindlyric0.data.repository.JournalRepository

class JournalViewModelFactory(
    private val repository: JournalRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JournalViewModel::class.java)) {
            return JournalViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
