package com.mindlyric.mindlyric0.data.remote

import com.mindlyric.mindlyric0.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// Claude API ile iletişim kuran servis sınıfı
// Günlük metni alır, duygu analizi yaparak -1.0 ile +1.0 arasında skor döner
object ClaudeService {

    // HTTP istekleri için OkHttp istemcisi (30 saniye zaman aşımı)
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Claude API endpoint'i
    private const val API_URL = "https://api.anthropic.com/v1/messages"

    // Kullanılacak model
    private const val MODEL = "claude-opus-4-6"

    // Duygu analizi yap — skor: -1.0 (çok negatif) ile +1.0 (çok pozitif)
    // Hata olursa null döner
    suspend fun analyzeSentiment(text: String): Float? = withContext(Dispatchers.IO) {
        try {
            // Claude'a gönderilecek istek mesajı
            val prompt = """
                Aşağıdaki Türkçe günlük metnini analiz et ve duygu skorunu ver.
                Skor 1 ile 10 arasında tam sayı olmalı:
                  1-3 = negatif/üzücü/kötü
                  4-6 = nötr/karışık
                  7-10 = pozitif/mutlu/iyi

                SADECE sayıyı döndür, başka hiçbir şey yazma. Örnek: 7

                Metin: "$text"
            """.trimIndent()

            // JSON isteği oluştur
            val requestBody = JSONObject().apply {
                put("model", MODEL)
                put("max_tokens", 10) // Sadece bir sayı bekliyoruz, az token yeterli
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }.toString()

            // HTTP isteği hazırla
            val request = Request.Builder()
                .url(API_URL)
                .addHeader("x-api-key", BuildConfig.CLAUDE_API_KEY)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            // İsteği gönder ve yanıtı al
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext null

            // Yanıtı parse et — Claude'un döndürdüğü metni al
            val jsonResponse = JSONObject(responseBody)
            val content = jsonResponse
                .getJSONArray("content")
                .getJSONObject(0)
                .getString("text")
                .trim()

            // Sayıya çevir
            content.toFloatOrNull()

        } catch (e: Exception) {
            // Herhangi bir hata olursa null dön (skor kaydedilmez)
            e.printStackTrace()
            null
        }
    }
}
