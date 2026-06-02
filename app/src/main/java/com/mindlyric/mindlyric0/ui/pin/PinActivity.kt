package com.mindlyric.mindlyric0.ui.pin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.mindlyric.mindlyric0.R
import com.mindlyric.mindlyric0.data.local.util.HashUtil

// PIN ekranı — iki modda çalışır:
// MODE_SET   → kullanıcı yeni PIN belirliyor (profil sayfasından toggle açılınca)
// MODE_CHECK → kullanıcı PIN giriyor (uygulama açılınca)
class PinActivity : AppCompatActivity() {

    companion object {
        const val MODE_SET   = "mode_set"
        const val MODE_CHECK = "mode_check"
        const val EXTRA_MODE = "extra_mode"

        const val PREFS_NAME   = "pin_prefs"
        const val KEY_PIN_HASH = "pin_hash"
        const val KEY_PIN_ON   = "pin_on"

        // PIN aktif mi?
        fun isPinAktif(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_PIN_ON, false)
        }

        // PIN'i kaydet ve kilidi aç
        fun pinKaydet(context: Context, pin: String) {
            val hash = HashUtil.sha256(pin)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PIN_HASH, hash)
                .putBoolean(KEY_PIN_ON, true)
                .apply()
        }

        // PIN'i sil ve kilidi kapat
        fun pinSil(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_PIN_HASH)
                .putBoolean(KEY_PIN_ON, false)
                .apply()
        }

        // Girilen PIN doğru mu?
        fun pinDogru(context: Context, pin: String): Boolean {
            val kayitliHash = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_PIN_HASH, null) ?: return false
            return HashUtil.sha256(pin) == kayitliHash
        }
    }

    // Kullanıcının şu ana kadar girdiği rakamlar
    private val girilenRakamlar = StringBuilder()

    // 6 daire görünümü
    private lateinit var daireler: List<View>

    private lateinit var mod: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)

        mod = intent.getStringExtra(EXTRA_MODE) ?: MODE_CHECK
        geriTusuAyarla()

        // Daireleri listeye al
        daireler = listOf(
            findViewById(R.id.dot1), findViewById(R.id.dot2), findViewById(R.id.dot3),
            findViewById(R.id.dot4), findViewById(R.id.dot5), findViewById(R.id.dot6)
        )

        // Moda göre başlık ve geri butonu ayarla
        val tvBaslik   = findViewById<TextView>(R.id.tvPinTitle)
        val tvAltBaslik = findViewById<TextView>(R.id.tvPinSubtitle)
        val tvGeri     = findViewById<TextView>(R.id.tvPinBack)

        if (mod == MODE_SET) {
            tvBaslik.text    = "PIN Belirleyin"
            tvAltBaslik.text = "6 haneli yeni PIN kodunuzu girin"
            tvGeri.visibility = View.VISIBLE
            tvGeri.setOnClickListener { finish() }
        } else {
            tvBaslik.text    = "PIN Giriniz"
            tvAltBaslik.text = "Güvenliğiniz için PIN kodunuzu girin"
            tvGeri.visibility = View.GONE
        }

        // Klavye tuşlarını bağla
        val tuslar = mapOf(
            R.id.key0 to "0", R.id.key1 to "1", R.id.key2 to "2",
            R.id.key3 to "3", R.id.key4 to "4", R.id.key5 to "5",
            R.id.key6 to "6", R.id.key7 to "7", R.id.key8 to "8",
            R.id.key9 to "9"
        )

        tuslar.forEach { (id, rakam) ->
            findViewById<TextView>(id).setOnClickListener {
                rakamEkle(rakam)
            }
        }

        // Silme butonu
        findViewById<TextView>(R.id.keyDelete).setOnClickListener {
            rakamSil()
        }
    }

    // Rakam ekle → daireyi doldur → 6 dolunca işlemi tamamla
    private fun rakamEkle(rakam: String) {
        if (girilenRakamlar.length >= 6) return

        girilenRakamlar.append(rakam)
        daireGuncelle()

        if (girilenRakamlar.length == 6) {
            pinTamamlandi()
        }
    }

    // Son rakamı sil → daireyi boşalt
    private fun rakamSil() {
        if (girilenRakamlar.isEmpty()) return
        girilenRakamlar.deleteCharAt(girilenRakamlar.length - 1)
        daireGuncelle()
    }

    // Girilen rakam sayısına göre daireleri doldur/boşalt
    private fun daireGuncelle() {
        daireler.forEachIndexed { index, daire ->
            daire.setBackgroundResource(
                if (index < girilenRakamlar.length) R.drawable.bg_pin_dot_filled
                else R.drawable.bg_pin_dot_empty
            )
        }
    }

    // 6 rakam girildi — moda göre kaydet veya doğrula
    private fun pinTamamlandi() {
        val pin = girilenRakamlar.toString()

        if (mod == MODE_SET) {
            // Yeni PIN kaydet → aktiviteyi başarıyla kapat
            pinKaydet(this, pin)
            setResult(RESULT_OK)
            finish()

        } else {
            // PIN doğrulama — doğruysa ana ekrana geç, yanlışsa sıfırla
            if (pinDogru(this, pin)) {
                setResult(RESULT_OK)
                finish()
            } else {
                Toast.makeText(this, "Hatalı PIN! Tekrar deneyin.", Toast.LENGTH_SHORT).show()
                girilenRakamlar.clear()
                daireGuncelle()
            }
        }
    }

    // Geri tuşuna basınca sadece MODE_SET'te çıkış izni ver
    // MODE_CHECK'te geri tuşu çalışmasın (PIN atlatılmasın)
    private fun geriTusuAyarla() {
        onBackPressedDispatcher.addCallback(this) {
            if (mod == MODE_SET) {
                finish()
            }
            // MODE_CHECK'te hiçbir şey yapma
        }
    }
}
