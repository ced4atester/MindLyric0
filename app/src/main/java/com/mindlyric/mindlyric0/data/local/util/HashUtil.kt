package com.mindlyric.mindlyric0.data.local.util

import java.security.MessageDigest

object HashUtil {

    /**
     * Verilen metni SHA-256 algoritması ile hash'ler.
     * Şifreler veritabanına düz metin yerine bu hash ile kaydedilir.
     * SHA-256 tek yönlüdür: hash'ten orijinal metne geri dönülemez.
     */
    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        // Metni UTF-8 byte dizisine çevirip hash hesapla
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        // Her byte'ı 2 haneli hexadecimal karaktere çevir ve birleştir
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
