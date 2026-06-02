package com.mindlyric.mindlyric0.data.remote

import com.mindlyric.mindlyric0.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// OpenWeatherMap API'sine istek atarak hava durumu bilgisi çeker
class WeatherService {

    // Zaten projede olan OkHttp client'ı burada da kullanıyoruz
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Hava durumu verilerini tutan basit veri sınıfı
    data class WeatherInfo(
        val cityName: String,       // Şehir adı (ör: İstanbul)
        val temperature: Int,       // Sıcaklık (Celsius)
        val description: String,    // Durum açıklaması (ör: Açık ve güneşli)
        val iconCode: String        // İkon kodu (ör: 01d — güneşli gündüz)
    )

    // Enlem ve boylama göre hava durumu çeker
    // Hata olursa null döner, uygulama çökmez
    suspend fun getWeather(lat: Double, lon: Double): WeatherInfo? {
        return try {
            val url = "https://api.openweathermap.org/data/2.5/weather" +
                    "?lat=$lat&lon=$lon" +
                    "&appid=${BuildConfig.WEATHER_API_KEY}" +
                    "&units=metric" +   // Celsius için
                    "&lang=tr"          // Türkçe açıklama için

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            // Ağ isteği (IO thread'de çalışmalı)
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null

            // JSON yanıtını ayrıştır
            val json = JSONObject(body)
            val cityName = json.getString("name")
            val temp = json.getJSONObject("main").getDouble("temp").toInt()
            val desc = json.getJSONArray("weather")
                .getJSONObject(0)
                .getString("description")
                // İlk harfi büyük yap (ör: "açık hava" → "Açık hava")
                .replaceFirstChar { it.uppercase() }
            val icon = json.getJSONArray("weather")
                .getJSONObject(0)
                .getString("icon")

            WeatherInfo(cityName, temp, desc, icon)

        } catch (e: Exception) {
            // Bağlantı hatası, timeout vb. — null döner, widget gizlenir
            null
        }
    }
}
