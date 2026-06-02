package com.mindlyric.mindlyric0

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.mindlyric.mindlyric0.data.local.db.DbProvider
import com.mindlyric.mindlyric0.data.remote.WeatherService
import com.mindlyric.mindlyric0.data.repository.JournalRepository
import com.mindlyric.mindlyric0.data.repository.UserRepository
import com.mindlyric.mindlyric0.ui.auth.LoginActivity
import com.mindlyric.mindlyric0.ui.journal.JournalAdapter
import com.mindlyric.mindlyric0.ui.journal.JournalViewModel
import com.mindlyric.mindlyric0.ui.journal.JournalViewModelFactory
import com.mindlyric.mindlyric0.ui.profile.ProfileActivity
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.airbnb.lottie.LottieAnimationView
import com.mindlyric.mindlyric0.ui.pin.PinActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: JournalViewModel
    private lateinit var adapter: JournalAdapter

    // Konum izni istek kodu
    private val KONUM_IZIN_KODU = 1001

    // PIN doğrulama istek kodu
    private val REQ_PIN_CHECK = 4001

    companion object {
        // Uygulama arka plana gittiğinde false yapılır
        // ProfileActivity gibi iç ekranlara gidince değişmez
        var pinDogrulandi = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // SharedPreferences'tan giriş yapan kullanıcının id'sini oku
        // Bu id, LoginActivity tarafından giriş başarılı olunca kaydedilmişti
        val prefs = getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val userId = prefs.getLong(LoginActivity.KEY_USER_ID, -1L)

        // Geçerli bir kullanıcı yoksa (id = -1) → Login ekranına gönder
        if (userId == -1L) {
            startActivity(
                Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            return
        }

        // ---- ViewModel kurulumu — userId ile birlikte ----
        val repository = JournalRepository(DbProvider.get(this).journalDao())
        viewModel = ViewModelProvider(
            this, JournalViewModelFactory(repository, userId)
        )[JournalViewModel::class.java]

        // ---- Profil avatar ikonu ----
        val ivProfileAvatar = findViewById<ImageView>(R.id.ivProfileAvatar)

        // Kullanıcının seçili avatarını üst barda göster
        val userRepository = UserRepository(DbProvider.get(this).userDao())
        lifecycleScope.launch {
            val user = userRepository.getUserById(userId)
            if (user?.avatarResId != null && user.avatarResId > 0) {
                // Kullanıcı avatar seçmişse onu göster
                ivProfileAvatar.setImageResource(user.avatarResId)
            } else {
                // Avatar seçilmemişse varsayılan profil ikonunu göster
                ivProfileAvatar.setImageResource(R.drawable.ic_user_profile)
            }
        }

        // Profile ikonuna basınca ProfileActivity aç
        ivProfileAvatar.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // ---- Çıkış butonu ----
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            // SharedPreferences'tan kayıtlı userId'yi sil → oturum sonlandırılır
            getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(LoginActivity.KEY_USER_ID)
                .apply()

            // LoginActivity'e yönlendir; geri tuşuyla bu ekrana dönülmesin
            startActivity(
                Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
        }

        // ---- RecyclerView kurulumu ----
        adapter = JournalAdapter(
            onDelete = { entry ->
                android.app.AlertDialog.Builder(this)
                    .setTitle(getString(R.string.main_delete_title))
                    .setMessage(getString(R.string.main_delete_message))
                    .setPositiveButton(getString(R.string.main_delete_confirm)) { _, _ -> viewModel.deleteEntry(entry) }
                    .setNegativeButton(getString(R.string.main_delete_cancel), null)
                    .show()
            }
        )

        val rvEntries = findViewById<RecyclerView>(R.id.rvJournalEntries)
        rvEntries.layoutManager = LinearLayoutManager(this)
        rvEntries.adapter = adapter

        // ---- Girdi alanları ----
        val etText    = findViewById<TextInputEditText>(R.id.etJournalText)
        val chipGroup = findViewById<ChipGroup>(R.id.chipGroupMood)
        val btnSave   = findViewById<Button>(R.id.btnSaveEntry)

        btnSave.setOnClickListener {
            val text = etText.text.toString().trim()

            if (text.isEmpty()) {
                Toast.makeText(this, getString(R.string.main_toast_empty), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedChipId = chipGroup.checkedChipId
            val moodLabel = if (selectedChipId == -1) null else {
                findViewById<com.google.android.material.chip.Chip>(selectedChipId).text.toString()
            }

            viewModel.addEntry(text, moodLabel)
            etText.text?.clear()
            chipGroup.clearCheck()
        }

        // ---- Girdileri gözlemle ----
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.entries.collect { entries ->
                    adapter.submitList(entries)
                    if (entries.isNotEmpty()) rvEntries.scrollToPosition(0)
                }
            }
        }

        // ---- Uygulama arka plana gittiğinde PIN doğrulamasını sıfırla ----
        // ProcessLifecycleOwner tüm uygulama arka plana gittiğinde tetiklenir
        // ProfileActivity'e gidip gelmede tetiklenmez — sadece gerçek arka plan geçişinde
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                pinDogrulandi = false
            }
        })

        // ---- Hava durumu widget'ı ----
        // Önce konum iznini kontrol et, yoksa kullanıcıdan iste
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            // İzin zaten verilmiş, hava durumunu çek
            havaDurumuCek()
        } else {
            // Kullanıcıdan izin iste
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                KONUM_IZIN_KODU
            )
        }
    }

    // Uygulama arka plandan geri gelince PIN kontrolü yap
    override fun onStart() {
        super.onStart()
        if (PinActivity.isPinAktif(this) && !pinDogrulandi) {
            startActivityForResult(
                Intent(this, PinActivity::class.java).apply {
                    putExtra(PinActivity.EXTRA_MODE, PinActivity.MODE_CHECK)
                },
                REQ_PIN_CHECK
            )
        }
    }

    // PIN doğrulama sonucu
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_PIN_CHECK) {
            if (resultCode == RESULT_OK) {
                // PIN doğrulandı → devam et
                pinDogrulandi = true
            } else {
                // PIN iptal edildi → uygulamayı kapat
                finishAffinity()
            }
        }
    }

    // Konum iznine kullanıcı yanıt verdikten sonra çağrılır
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == KONUM_IZIN_KODU &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // İzin verildi → hava durumunu çek
            havaDurumuCek()
        }
        // İzin reddedildiyse widget gizli kalır (visibility="gone")
    }

    // Konumu al ve hava durumunu çek
    private fun havaDurumuCek() {
        val fusedClient = LocationServices.getFusedLocationProviderClient(this)

        // İzin kontrolü (lint uyarısını bastırmak için)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return

        // Önce lastLocation dene
        Log.d("HAVA", "lastLocation isteniyor...")
        fusedClient.lastLocation
            .addOnSuccessListener { konum ->
                if (konum != null) {
                    Log.d("HAVA", "lastLocation geldi: ${konum.latitude}, ${konum.longitude}")
                    havaWidgetGuncelle(konum.latitude, konum.longitude)
                } else {
                    // lastLocation null → requestLocationUpdates ile tek seferlik konum iste
                    // Emülatörde getCurrentLocation takılıyor, bu yöntem daha güvenilir
                    Log.d("HAVA", "lastLocation null, requestLocationUpdates deneniyor...")
                    val locationRequest = LocationRequest.Builder(
                        Priority.PRIORITY_HIGH_ACCURACY, 1000L
                    ).setMaxUpdates(1).build()

                    val callback = object : LocationCallback() {
                        override fun onLocationResult(result: LocationResult) {
                            val konum2 = result.lastLocation
                            if (konum2 != null) {
                                Log.d("HAVA", "requestLocationUpdates geldi: ${konum2.latitude}, ${konum2.longitude}")
                                havaWidgetGuncelle(konum2.latitude, konum2.longitude)
                            } else {
                                Log.d("HAVA", "requestLocationUpdates da null döndü!")
                            }
                            // Tek seferlik aldık, güncellemeyi durdur
                            fusedClient.removeLocationUpdates(this)
                        }
                    }
                    fusedClient.requestLocationUpdates(locationRequest, callback, mainLooper)
                }
            }
            .addOnFailureListener { e ->
                Log.e("HAVA", "lastLocation hata: ${e.message}")
            }
    }

    // Koordinatları alıp hava durumu API'sine istek atar, widget'ı günceller
    private fun havaWidgetGuncelle(lat: Double, lon: Double) {
        lifecycleScope.launch {
            val hava = withContext(Dispatchers.IO) {
                WeatherService().getWeather(lat, lon)
            }
            if (hava != null) {
                val widget   = findViewById<LinearLayout>(R.id.weatherWidget)
                val lottie   = findViewById<LottieAnimationView>(R.id.lottieWeatherIcon)
                val tvCity   = findViewById<TextView>(R.id.tvWeatherCity)
                val tvDesc   = findViewById<TextView>(R.id.tvWeatherDesc)
                val tvTemp   = findViewById<TextView>(R.id.tvWeatherTemp)

                widget.visibility = View.VISIBLE

                // Hava durumuna göre doğru Lottie animasyonunu seç
                val animRes = ikonAnimasyonSec(hava.iconCode)
                lottie.setAnimation(animRes)
                lottie.playAnimation()

                // Hava durumuna göre widget arka planını ayarla
                val bgRes = arkaPlanSec(hava.iconCode)
                if (bgRes != null) {
                    widget.setBackgroundResource(bgRes)
                } else {
                    widget.background = null
                }

                tvCity.text = hava.cityName
                tvDesc.text = hava.description
                tvTemp.text = "${hava.temperature}°C"
            }
        }
    }

    // OpenWeatherMap ikon koduna göre Lottie raw resource id döner
    private fun ikonAnimasyonSec(iconCode: String): Int {
        return when {
            iconCode.startsWith("01") -> R.raw.weather_sunny   // açık, güneşli
            iconCode.startsWith("02") -> R.raw.weather_sunny   // az bulutlu → güneşli
            iconCode.startsWith("03") -> R.raw.weather_sunny   // parçalı bulutlu → güneşli
            iconCode.startsWith("04") -> R.raw.weather_sunny   // bulutlu → güneşli
            iconCode.startsWith("09") -> R.raw.weather_rainy   // sağanak
            iconCode.startsWith("10") -> R.raw.weather_rainy   // yağmurlu
            iconCode.startsWith("11") -> R.raw.weather_rainy   // fırtınalı → yağmurlu
            iconCode.startsWith("13") -> R.raw.weather_snowy   // karlı
            iconCode.startsWith("50") -> R.raw.weather_sunny   // sisli → güneşli
            else -> R.raw.weather_sunny
        }
    }

    // Hava durumuna göre widget arka planını döner
    // Şimdilik sadece güneşli için özel arka plan var, diğerleri null (arka plan yok)
    private fun arkaPlanSec(iconCode: String): Int? {
        return when {
            iconCode.startsWith("01") -> R.drawable.bg_weather_sunny
            iconCode.startsWith("02") -> R.drawable.bg_weather_sunny
            iconCode.startsWith("03") -> R.drawable.bg_weather_sunny
            iconCode.startsWith("04") -> R.drawable.bg_weather_sunny
            iconCode.startsWith("50") -> R.drawable.bg_weather_sunny
            else -> null  // yağmurlu ve karlı için henüz arka plan yok
        }
    }
}
