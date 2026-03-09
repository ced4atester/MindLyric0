package com.mindlyric.mindlyric0.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mindlyric.mindlyric0.data.repository.UserRepository

/**
 * AuthViewModel'in constructor'ına UserRepository geçebilmek için factory gereklidir.
 * ViewModelProvider varsayılan olarak parametresiz constructor arar;
 * bu factory sayesinde bağımlılık enjeksiyonu manuel olarak yapılır.
 */
class AuthViewModelFactory(
    private val repository: UserRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Bilinmeyen ViewModel: ${modelClass.name}")
    }
}
