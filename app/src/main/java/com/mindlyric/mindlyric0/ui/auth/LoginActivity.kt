package com.mindlyric.mindlyric0.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.mindlyric.mindlyric0.MainActivity
import com.mindlyric.mindlyric0.R
import com.mindlyric.mindlyric0.data.local.db.DbProvider
import com.mindlyric.mindlyric0.data.repository.UserRepository

class LoginActivity : AppCompatActivity() {

    companion object {
        // SharedPreferences dosya adı ve anahtar; RegisterActivity ile aynı değerler kullanılır
        const val PREFS_NAME = "mindlyric_prefs"
        const val KEY_USER_ID = "logged_in_user_id"
    }

    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ---- Oturum kalıcılığı kontrolü ----
        // "Beni Hatırla" ile daha önce giriş yapıldıysa userId SharedPreferences'ta saklanır.
        // Uygulama açılırken kontrol edilir; kayıtlı userId varsa login ekranı gösterilmez.
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedUserId = prefs.getInt(KEY_USER_ID, -1)
        if (savedUserId != -1) {
            navigateToMain(savedUserId)
            return // setContentView çağrılmadan activity bitirilir
        }

        setContentView(R.layout.activity_login)

        // ---- ViewModel kurulumu ----
        // UserRepository oluştur ve AuthViewModelFactory aracılığıyla ViewModel'e geç
        val repository = UserRepository(DbProvider.get(this).userDao())
        viewModel = ViewModelProvider(
            this, AuthViewModelFactory(repository)
        )[AuthViewModel::class.java]

        val tilEmail      = findViewById<TextInputLayout>(R.id.tilLoginEmail)
        val tilPassword   = findViewById<TextInputLayout>(R.id.tilLoginPassword)
        val etEmail       = findViewById<TextInputEditText>(R.id.etLoginEmail)
        val etPassword    = findViewById<TextInputEditText>(R.id.etLoginPassword)
        val btnLogin      = findViewById<Button>(R.id.btnLogin)
        val cbRememberMe  = findViewById<CheckBox>(R.id.cbRememberMe)
        val progressBar   = findViewById<ProgressBar>(R.id.progressLogin)
        val tvGoToRegister = findViewById<TextView>(R.id.tvGoToRegister)

        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnLogin.setOnClickListener {
            // Önceki hata mesajlarını temizle; her tıklamada taze başlat
            tilEmail.error = null
            tilPassword.error = null

            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Validasyon ve DB işlemi ViewModel'da; Activity sadece değerleri iletir
            viewModel.login(email, password)
        }

        // ---- Durum gözlemi ----
        // ViewModel'dan gelen her state değişikliğinde UI güncellenir
        viewModel.authState.observe(this) { state ->
            when (state) {

                is AuthState.Loading -> {
                    // İşlem başladı: buton devre dışı, spinner görünür → çift tıklama engellenir
                    btnLogin.isEnabled = false
                    progressBar.visibility = View.VISIBLE
                }

                is AuthState.LoginSuccess -> {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true

                    // "Beni Hatırla" seçildiyse userId'yi SharedPreferences'a kaydet
                    if (cbRememberMe.isChecked) {
                        prefs.edit().putInt(KEY_USER_ID, state.userId).apply()
                    }

                    navigateToMain(state.userId)
                    viewModel.resetState() // tekrar tetiklenmemesi için Idle'a dön
                }

                is AuthState.ValidationError -> {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                    // Hatayı ilgili alanın altında göster
                    when (state.field) {
                        ValidationField.EMAIL    -> tilEmail.error = state.message
                        ValidationField.PASSWORD -> tilPassword.error = state.message
                        else                     -> tilEmail.error = state.message
                    }
                    viewModel.resetState()
                }

                is AuthState.Error -> {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                    // Yanlış şifre/email hatası şifre alanında gösterilir
                    when (state.field) {
                        ValidationField.EMAIL    -> tilEmail.error = state.message
                        ValidationField.PASSWORD -> tilPassword.error = state.message
                        else                     -> tilPassword.error = state.message
                    }
                    viewModel.resetState()
                }

                else -> {
                    // Idle ve diğer durumlar: UI'ı sıfırla
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                }
            }
        }
    }

    // MainActivity'ye geçiş ve bu Activity'yi yığından kaldır
    private fun navigateToMain(userId: Int) {
        startActivity(Intent(this, MainActivity::class.java).apply {
            putExtra("USER_ID", userId)
        })
        finish()
    }
}
