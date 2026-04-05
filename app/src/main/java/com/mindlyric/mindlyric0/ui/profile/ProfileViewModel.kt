package com.mindlyric.mindlyric0.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindlyric.mindlyric0.data.local.entity.UserEntity
import com.mindlyric.mindlyric0.data.repository.ChangePasswordResult
import com.mindlyric.mindlyric0.data.repository.JournalRepository
import com.mindlyric.mindlyric0.data.repository.UserRepository
import kotlinx.coroutines.launch

// Profil ekranının tüm iş mantığını yönetir
class ProfileViewModel(
    private val repository: UserRepository,
    private val journalRepository: JournalRepository,
    private val userId: Long
) : ViewModel() {

    // İstatistik verileri
    private val _journalCount = MutableLiveData<Int>(0)
    val journalCount: LiveData<Int> = _journalCount

    private val _topMood = MutableLiveData<String?>()
    val topMood: LiveData<String?> = _topMood

    // Kullanıcı bilgilerini tutar — Activity bu LiveData'yı gözlemler
    private val _user = MutableLiveData<UserEntity?>()
    val user: LiveData<UserEntity?> = _user

    // Ekranın anlık durumunu tutar (yükleniyor, başarılı, hata vs.)
    private val _state = MutableLiveData<ProfileState>(ProfileState.Idle)
    val state: LiveData<ProfileState> = _state

    // ViewModel oluşturulunca tüm verileri yükle
    init {
        loadUser()
        loadStats()
    }

    // Veritabanından kullanıcı bilgilerini çeker
    fun loadUser() {
        viewModelScope.launch {
            _user.value = repository.getUserById(userId)
        }
    }

    // İstatistikleri yükler: toplam günlük sayısı ve en sık ruh hali
    private fun loadStats() {
        viewModelScope.launch {
            _journalCount.value = journalRepository.getJournalCount(userId)
            _topMood.value = journalRepository.getTopMood(userId)
        }
    }

    // Seçilen avatarı veritabanına kaydeder
    fun updateAvatar(avatarResId: Int) {
        viewModelScope.launch {
            repository.updateAvatar(userId, avatarResId)
            loadUser() // Ekranı güncellemek için kullanıcıyı yeniden yükle
        }
    }

    // Şifre değiştirme işlemi
    fun changePassword(oldPassword: String, newPassword: String) {
        // Yeni şifre en az 6 karakter olmalı
        if (newPassword.length < 6) {
            _state.value = ProfileState.Error("Yeni şifre en az 6 karakter olmalı")
            return
        }
        _state.value = ProfileState.Loading
        viewModelScope.launch {
            val result = repository.changePassword(userId, oldPassword, newPassword)
            _state.value = when (result) {
                ChangePasswordResult.Success      -> ProfileState.PasswordChanged
                ChangePasswordResult.WrongPassword -> ProfileState.Error("Mevcut şifre hatalı")
                is ChangePasswordResult.Error      -> ProfileState.Error(result.message)
            }
        }
    }

    // Hesabı siler — kullanıcı + tüm günlükleri veritabanından kaldırır
    fun deleteAccount() {
        _state.value = ProfileState.Loading
        viewModelScope.launch {
            repository.deleteAccount(userId)
            _state.value = ProfileState.AccountDeleted
        }
    }

    // State tüketildikten sonra Idle'a sıfırla
    fun resetState() {
        _state.value = ProfileState.Idle
    }
}

// Profil ekranının olası durumları
sealed class ProfileState {
    object Idle : ProfileState()
    object Loading : ProfileState()
    object PasswordChanged : ProfileState()  // Şifre başarıyla değiştirildi
    object AccountDeleted : ProfileState()   // Hesap silindi → Login'e yönlendir
    data class Error(val message: String) : ProfileState()
}
