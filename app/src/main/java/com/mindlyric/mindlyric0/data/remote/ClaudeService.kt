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

    // Son 7 günün ortalama skoruna göre yarın için tahmin üret
    // Hata olursa null döner
    suspend fun getMoodPrediction(averageScore: Float, entryCount: Int): String? = withContext(Dispatchers.IO) {
        try {
            val durum = when {
                averageScore <= 3f -> "çoğunlukla zor ve negatif"
                averageScore <= 6f -> "ortalama ve nötr"
                else               -> "genel olarak iyi ve pozitif"
            }

            val prompt = """
                Bir kullanıcı son 7 gün içinde $entryCount günlük yazdı.
                Bu günlüklerin ortalama duygu skoru ${String.format("%.1f", averageScore)}/10 — $durum geçti.

                Bu örüntüye dayanarak kullanıcıya Türkçe, samimi ve kısa (2-3 cümle) bir tahmin/yorum yaz.
                - Trende dikkat çek
                - "Yarın" veya "önümüzdeki günler" hakkında olumlu/gerçekçi bir öngörü sun
                - Motive edici ama abartısız ol

                SADECE tahmini yaz, başka hiçbir şey ekleme. Tırnak işareti kullanma.
            """.trimIndent()

            val requestBody = JSONObject().apply {
                put("model", MODEL)
                put("max_tokens", 150)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }.toString()

            val request = Request.Builder()
                .url(API_URL)
                .addHeader("x-api-key", BuildConfig.CLAUDE_API_KEY)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext null

            val jsonResponse = JSONObject(responseBody)
            jsonResponse
                .getJSONArray("content")
                .getJSONObject(0)
                .getString("text")
                .trim()

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Skora göre kişisel öneri üret — Türkçe, kısa ve samimi bir mesaj döner
    // Hata olursa null döner
    suspend fun getRecommendation(text: String, score: Float): String? = withContext(Dispatchers.IO) {
        try {
            val durum = when {
                score <= 3f -> "çok zor ve negatif"
                score <= 6f -> "nötr ve karışık"
                else        -> "güzel ve pozitif"
            }

            val prompt = """
                Bir kullanıcı bugün günlüğüne şunları yazdı:
                "$text"

                Duygu analizi sonucuna göre bu gün $durum geçti (skor: ${score.toInt()}/10).

                Bu kullanıcıya Türkçe, samimi, kısa (2-3 cümle) ve kişisel bir mesaj yaz.
                - Negatif günlerde: empati kur, cesaretlendir
                - Nötr günlerde: olumlu bir bakış açısı sun
                - Pozitif günlerde: tebrik et, iyi enerjiyi pekiştir

                SADECE mesajı yaz, başka hiçbir şey ekleme. Tırnak işareti kullanma.
            """.trimIndent()

            val requestBody = JSONObject().apply {
                put("model", MODEL)
                put("max_tokens", 150)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }.toString()

            val request = Request.Builder()
                .url(API_URL)
                .addHeader("x-api-key", BuildConfig.CLAUDE_API_KEY)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext null

            val jsonResponse = JSONObject(responseBody)
            jsonResponse
                .getJSONArray("content")
                .getJSONObject(0)
                .getString("text")
                .trim()

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Duygu analizi yap — skor 1-10 arası tam sayı döner
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
