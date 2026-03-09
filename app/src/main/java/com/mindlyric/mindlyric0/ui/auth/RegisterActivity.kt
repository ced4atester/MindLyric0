package com.mindlyric.mindlyric0.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
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

class RegisterActivity : AppCompatActivity() {

    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // ---- ViewModel kurulumu ----
        val repository = UserRepository(DbProvider.get(this).userDao())
        viewModel = ViewModelProvider(
            this, AuthViewModelFactory(repository)
        )[AuthViewModel::class.java]

        val tilUsername    = findViewById<TextInputLayout>(R.id.tilUsername)
        val tilEmail       = findViewById<TextInputLayout>(R.id.tilEmail)
        val tilPassword    = findViewById<TextInputLayout>(R.id.tilPassword)
        val etUsername     = findViewById<TextInputEditText>(R.id.etUsername)
        val etEmail        = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword     = findViewById<TextInputEditText>(R.id.etPassword)
        val btnRegister    = findViewById<Button>(R.id.btnRegister)
        val progressBar    = findViewById<ProgressBar>(R.id.progressRegister)

        // "Giriş Yap" linkine basınca RegisterActivity kapanır, LoginActivity'e döner
        findViewById<TextView>(R.id.tvGoToLogin).setOnClickListener {
            finish()
        }

        btnRegister.setOnClickListener {
            // Her tıklamada önceki hata mesajlarını temizle
            tilUsername.error = null
            tilEmail.error = null
            tilPassword.error = null

            val username = etUsername.text.toString().trim()
            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Validasyon + DB işlemi ViewModel'da yapılır
            viewModel.register(username, email, password)
        }

        // ---- Durum gözlemi ----
        viewModel.authState.observe(this) { state ->
            when (state) {

                is AuthState.Loading -> {
                    // Kayıt işlemi başladı: buton kapat, spinner göster
                    btnRegister.isEnabled = false
                    progressBar.visibility = View.VISIBLE
                }

                is AuthState.RegisterSuccess -> {
                    progressBar.visibility = View.GONE
                    btnRegister.isEnabled = true

                    // Kayıt başarılı olduğunda userId elimizde; direkt giriş yaptır.
                    // Kullanıcıyı tekrar login ekranına yönlendirmek yerine
                    // SharedPreferences'a kaydedip MainActivity'e geç (auto-login).
                    val prefs = getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit().putInt(LoginActivity.KEY_USER_ID, state.userId).apply()

                    startActivity(Intent(this, MainActivity::class.java).apply {
                        putExtra("USER_ID", state.userId)
                        // Geri tuşuna basıldığında login/register ekranına dönülmesin
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    viewModel.resetState()
                }

                is AuthState.ValidationError -> {
                    progressBar.visibility = View.GONE
                    btnRegister.isEnabled = true
                    // Hatayı ilgili alanın altında göster
                    when (state.field) {
                        ValidationField.USERNAME -> tilUsername.error = state.message
                        ValidationField.EMAIL    -> tilEmail.error = state.message
                        ValidationField.PASSWORD -> tilPassword.error = state.message
                        else                     -> tilUsername.error = state.message
                    }
                    viewModel.resetState()
                }

                is AuthState.Error -> {
                    progressBar.visibility = View.GONE
                    btnRegister.isEnabled = true
                    // Email zaten kayıtlı hatası email alanında gösterilir
                    when (state.field) {
                        ValidationField.EMAIL    -> tilEmail.error = state.message
                        ValidationField.PASSWORD -> tilPassword.error = state.message
                        else                     -> tilEmail.error = state.message
                    }
                    viewModel.resetState()
                }

                else -> {
                    progressBar.visibility = View.GONE
                    btnRegister.isEnabled = true
                }
            }
        }
    }
}
