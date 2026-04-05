package com.mindlyric.mindlyric0.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mindlyric.mindlyric0.data.repository.JournalRepository

// Factory: ViewModel'e repository ve userId'yi dışarıdan vermemizi sağlar
class JournalViewModelFactory(
    private val repository: JournalRepository,
    private val userId: Long       // giriş yapan kullanıcının id'si
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JournalViewModel::class.java)) {
            // ViewModel'i repository ve userId ile oluştur
            return JournalViewModel(repository, userId) as T
        }
        throw IllegalArgumentException("Bilinmeyen ViewModel sınıfı")
    }
}
