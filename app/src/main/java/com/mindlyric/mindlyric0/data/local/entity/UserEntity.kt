package com.mindlyric.mindlyric0.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Kullanıcı tablosu.
 * indices: email alanına veritabanı seviyesinde benzersizlik kısıtı eklenir.
 * Böylece aynı email ile iki kayıt oluşturulamaz (uygulama kontrolüne ek güvence).
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val username: String,
    val email: String,
    // Şifre SHA-256 hash olarak saklanır, düz metin değil
    val password: String
)
