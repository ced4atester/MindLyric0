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
     * Girilen şifre hash'lenerek veritabanındaki hash ile karşılaştırılır.
     */
    suspend fun login(email: String, password: String): LoginResult {
        val hashedPassword = HashUtil.sha256(password)
        val user = userDao.login(email, hashedPassword)
        return if (user != null) {
            LoginResult.Success(userId = user.id)
        } else {
            LoginResult.InvalidCredentials
        }
    }
}

// Kayıt işleminin olası sonuçları
sealed class RegisterResult {
    data class Success(val userId: Long) : RegisterResult()  // Long: Room id tipiyle uyumlu
    object EmailTaken : RegisterResult()
}

// Giriş işleminin olası sonuçları
sealed class LoginResult {
    data class Success(val userId: Long) : LoginResult()  // Long: Room id tipiyle uyumlu
    object InvalidCredentials : LoginResult()
}
