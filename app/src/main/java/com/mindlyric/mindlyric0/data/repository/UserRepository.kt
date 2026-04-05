package com.mindlyric.mindlyric0.data.repository

import com.mindlyric.mindlyric0.data.local.dao.UserDao
import com.mindlyric.mindlyric0.data.local.entity.UserEntity
import com.mindlyric.mindlyric0.data.local.util.HashUtil

/**
 * Kullanıcı işlemleri için tek sorumlu katman (Repository pattern).
 * Activity ve ViewModel doğrudan DAO'ya erişmez; bu sınıf üzerinden çalışır.
 */
class UserRepository(private val userDao: UserDao) {

    /**
     * Yeni kullanıcı kaydeder.
     * 1. Email daha önce alınmışsa EmailTaken döner.
     * 2. Şifreyi SHA-256 ile hash'ler, düz metin saklamaz.
     * 3. Başarılı kayıtta eklenen kullanıcının id'sini döner (auto-login için).
     */
    suspend fun register(username: String, email: String, password: String): RegisterResult {
        val existing = userDao.getUserByEmail(email)
        if (existing != null) return RegisterResult.EmailTaken

        // Şifreyi hash'le; veritabanına hiçbir zaman düz metin gitmez
        val hashedPassword = HashUtil.sha256(password)
        val newId = userDao.insertUser(
            UserEntity(username = username, email = email, password = hashedPassword)
        )
        // insertUser'ın döndürdüğü Long rowId = autoGenerate id ile aynıdır
        // newId zaten Long; toInt() kaldırıldı — tutarlılık için Long kullanıyoruz
        return RegisterResult.Success(userId = newId)
    }

    /**
     * Kullanıcı girişini doğrular.
     * Önce email var mı kontrol edilir, sonra şifre doğrulanır.
     * Böylece "kullanıcı yok" ve "şifre yanlış" ayrı hata olarak döner.
     */
    suspend fun login(email: String, password: String): LoginResult {
        // Email veritabanında var mı kontrol et
        val userByEmail = userDao.getUserByEmail(email)
        if (userByEmail == null) return LoginResult.UserNotFound

        // Email var ama şifre yanlış mı?
        val hashedPassword = HashUtil.sha256(password)
        val user = userDao.login(email, hashedPassword)
        return if (user != null) {
            LoginResult.Success(userId = user.id)
        } else {
            LoginResult.WrongPassword
        }
    }

    // Kullanıcının profil bilgilerini getirir
    suspend fun getUserById(userId: Long) = userDao.getUserById(userId)

    // Yeni avatar kaydeder
    suspend fun updateAvatar(userId: Long, avatarResId: Int) =
        userDao.updateAvatar(userId, avatarResId)

    // Şifre değiştirme: önce eski şifre doğrulanır, sonra yeni şifre hash'lenerek kaydedilir
    suspend fun changePassword(userId: Long, oldPassword: String, newPassword: String): ChangePasswordResult {
        val user = userDao.getUserById(userId) ?: return ChangePasswordResult.Error("Kullanıcı bulunamadı")
        // Eski şifreyi hash'le ve kayıttaki ile karşılaştır
        if (HashUtil.sha256(oldPassword) != user.password) return ChangePasswordResult.WrongPassword
        userDao.updatePassword(userId, HashUtil.sha256(newPassword))
        return ChangePasswordResult.Success
    }

    // Hesabı siler: önce günlükler, sonra kullanıcı kaydı silинir
    suspend fun deleteAccount(userId: Long) {
        userDao.deleteUserJournals(userId) // önce günlükleri sil
        userDao.deleteUser(userId)         // sonra kullanıcıyı sil
    }
}

// Kayıt işleminin olası sonuçları
sealed class RegisterResult {
    data class Success(val userId: Long) : RegisterResult()  // Long: Room id tipiyle uyumlu
    object EmailTaken : RegisterResult()
}

// Giriş işleminin olası sonuçları
sealed class LoginResult {
    data class Success(val userId: Long) : LoginResult()
    object UserNotFound : LoginResult()   // Bu email ile kayıtlı hesap yok
    object WrongPassword : LoginResult()  // Email var ama şifre yanlış
}

// Şifre değiştirme işleminin olası sonuçları
sealed class ChangePasswordResult {
    object Success : ChangePasswordResult()
    object WrongPassword : ChangePasswordResult()
    data class Error(val message: String) : ChangePasswordResult()
}
