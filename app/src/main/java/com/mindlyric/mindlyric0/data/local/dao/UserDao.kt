package com.mindlyric.mindlyric0.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mindlyric.mindlyric0.data.local.entity.UserEntity

@Dao
interface UserDao {

    /**
     * Yeni kullanıcı ekler.
     * Long dönüş değeri: eklenen satırın rowId'si (= UserEntity.id ile aynı).
     * Kayıt sonrası auto-login için userId'ye ihtiyaç duyulduğundan Long döndürülür.
     */
    @Insert
    suspend fun insertUser(user: UserEntity): Long

    // Email'e göre kullanıcı bulur; kayıt sırasında email tekrarını kontrol etmek için kullanılır
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    /**
     * Email ve şifre hash'i ile giriş sorgusu.
     * Şifre SHA-256 hash'lenerek geçilir; veritabanında da hash karşılaştırması yapılır.
     */
    @Query("SELECT * FROM users WHERE email = :email AND password = :passwordHash LIMIT 1")
    suspend fun login(email: String, passwordHash: String): UserEntity?

    // Kullanıcıyı id'ye göre getirir — profil ekranında bilgileri göstermek için
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Long): UserEntity?

    // Seçilen avatar id'sini günceller
    @Query("UPDATE users SET avatarResId = :avatarResId WHERE id = :userId")
    suspend fun updateAvatar(userId: Long, avatarResId: Int)

    // Şifreyi günceller — yeni şifre hash'lenmiş olarak geçilmeli
    @Query("UPDATE users SET password = :newPasswordHash WHERE id = :userId")
    suspend fun updatePassword(userId: Long, newPasswordHash: String)

    // Kullanıcıyı tamamen siler
    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: Long)

    // Kullanıcıya ait tüm günlük kayıtlarını siler — hesap silinirken çağrılır
    @Query("DELETE FROM journal_entries WHERE userId = :userId")
    suspend fun deleteUserJournals(userId: Long)
}
