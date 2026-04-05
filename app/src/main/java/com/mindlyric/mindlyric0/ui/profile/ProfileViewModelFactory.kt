package com.mindlyric.mindlyric0.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mindlyric.mindlyric0.data.repository.JournalRepository
import com.mindlyric.mindlyric0.data.repository.UserRepository

// Factory: ProfileViewModel'e repository ve userId'yi dışarıdan vermemizi sağlar
class ProfileViewModelFactory(
    private val repository: UserRepository,
    private val journalRepository: JournalRepository,
    private val userId: Long
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            return ProfileViewModel(repository, journalRepository, userId) as T
        }
        throw IllegalArgumentException("Bilinmeyen ViewModel sınıfı")
    }
}
