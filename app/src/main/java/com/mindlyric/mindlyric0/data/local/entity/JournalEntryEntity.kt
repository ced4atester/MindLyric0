package com.mindlyric.mindlyric0.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Veritabanındaki "journal_entries" tablosunu temsil eden veri sınıfı
@Entity(tableName = "journal_entries")
data class JournalEntryEntity(

    // Her kaydın benzersiz kimliği — Room otomatik üretir
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Bu günlüğün hangi kullanıcıya ait olduğunu tutar
    // UserEntity'deki id ile eşleşir
    val userId: Long,

    // Günlüğün yazıldığı zaman (milisaniye cinsinden — epoch time)
    val createdAt: Long,

    // Kullanıcının yazdığı günlük metni
    val text: String,

    // Seçilen ruh hali etiketi (ör: "Mutlu", "Üzgün") — seçilmezse null
    val moodLabel: String?,

    // Duygu analizi skoru — ileride kullanılacak, şimdilik null
    val sentimentScore: Float?
)
