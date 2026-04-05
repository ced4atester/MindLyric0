package com.mindlyric.mindlyric0.ui.auth

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindlyric.mindlyric0.data.repository.LoginResult
import com.mindlyric.mindlyric0.data.repository.RegisterResult
import com.mindlyric.mindlyric0.data.repository.UserRepository
import kotlinx.coroutines.launch

/**
 * Login ve Register ekranlarının paylaştığı ViewModel.
 * İş mantığı (validasyon, DB çağrısı) burada tutulur; Activity sadece UI'ı günceller.
 */
class AuthViewModel(private val repository: UserRepository) : ViewModel() {

    // UI'ın gözlemlediği durum; private MutableLiveData dışarıya sadece LiveData olarak açılır
    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState

    // -------- GİRİŞ --------

    fun login(email: String, password: String) {
        // Boş alan kontrolü
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.ValidationError("E-posta ve şifre girin", ValidationField.GENERAL)
            return
        }
        // Email format kontrolü (android.util.Patterns kullanılır, regex yazmaya gerek yok)
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authState.value = AuthState.ValidationError("Geçerli bir e-posta girin", ValidationField.EMAIL)
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = repository.login(email, password)
                _authState.value = when (result) {
                    is LoginResult.Success          -> AuthState.LoginSuccess(result.userId)
                    LoginResult.InvalidCredentials  -> AuthState.Error("E-posta veya şifre hatalı", ValidationField.PASSWORD)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Beklenmeyen hata: ${e.message}", ValidationField.GENERAL)
            }
        }
    }

    // -------- KAYIT --------

    fun register(username: String, email: String, password: String) {
        // Boş alan kontrolü
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.ValidationError("Tüm alanları doldurun", ValidationField.GENERAL)
            return
        }
        // Email format kontrolü
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authState.value = AuthState.ValidationError("Geçerli bir e-posta girin", ValidationField.EMAIL)
            return
        }
        // Şifre minimum 6 karakter
        if (password.length < 6) {
            _authState.value = AuthState.ValidationError("Şifre en az 6 karakter olmalı", ValidationField.PASSWORD)
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = repository.register(username, email, password)
                _authState.value = when (result) {
                    is RegisterResult.Success -> AuthState.RegisterSuccess(result.userId)
                    RegisterResult.EmailTaken -> AuthState.Error("Bu e-posta zaten kayıtlı", ValidationField.EMAIL)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Beklenmeyen hata: ${e.message}", ValidationField.GENERAL)
            }
        }
    }

    /**
     * Durum tüketildikten sonra Idle'a sıfırla.
     * Ekran döndürme gibi durumlarda aynı event'in tekrar tetiklenmesini engeller.
     */
    fun resetState() {
        _authState.value = AuthState.Idle
    }
}

// Hangi forma alanına hata bağlanacağını belirler
enum class ValidationField { EMAIL, PASSWORD, USERNAME, GENERAL }

// Tüm olası UI durumlarını temsil eden sealed class
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class LoginSuccess(val userId: Long) : AuthState()   // Long: Room id tipiyle uyumlu
    data class RegisterSuccess(val userId: Long) : AuthState() // Long: Room id tipiyle uyumlu
    data class ValidationError(val message: String, val field: ValidationField) : AuthState()
    data class Error(val message: String, val field: ValidationField) : AuthState()
}
