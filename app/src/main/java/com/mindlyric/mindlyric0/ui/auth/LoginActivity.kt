package com.mindlyric.mindlyric0.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.mindlyric.mindlyric0.MainActivity
import com.mindlyric.mindlyric0.R
import com.mindlyric.mindlyric0.data.local.db.DbProvider
import com.mindlyric.mindlyric0.data.repository.UserRepository
import com.mindlyric.mindlyric0.ui.pin.PinActivity

class LoginActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME  = "mindlyric_prefs"
        const val KEY_USER_ID = "logged_in_user_id"
    }

    // PIN doğrulama ekranından dönüş kodu
    private val REQ_PIN_CHECK = 3001

    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Daha önce "Beni Hatırla" seçildiyse direkt ana ekrana geç
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedUserId = prefs.getLong(KEY_USER_ID, -1L)
        if (savedUserId != -1L) {
            // Uygulama kilidi aktifse önce PIN ekranına yönlendir
            if (PinActivity.isPinAktif(this)) {
                startActivityForResult(
                    Intent(this, PinActivity::class.java).apply {
                        putExtra(PinActivity.EXTRA_MODE, PinActivity.MODE_CHECK)
                    },
                    REQ_PIN_CHECK
                )
            } else {
                navigateToMain(savedUserId)
            }
            return
        }

        setContentView(R.layout.activity_login)

        // ---- ViewModel kurulumu ----
        val repository = UserRepository(DbProvider.get(this).userDao())
        viewModel = ViewModelProvider(
            this, AuthViewModelFactory(repository)
        )[AuthViewModel::class.java]

        // ---- View referansları ----
        val tilEmail        = findViewById<TextInputLayout>(R.id.tilLoginEmail)
        val tilPassword     = findViewById<TextInputLayout>(R.id.tilLoginPassword)
        val etEmail         = findViewById<TextInputEditText>(R.id.etLoginEmail)
        val etPassword      = findViewById<TextInputEditText>(R.id.etLoginPassword)
        val btnLogin        = findViewById<Button>(R.id.btnLogin)
        val btnLoginEmail   = findViewById<Button>(R.id.btnLoginEmail)
        val btnLoginGoogle  = findViewById<Button>(R.id.btnLoginGoogle)
        val cbRememberMe    = findViewById<CheckBox>(R.id.cbRememberMe)
        val progressBar     = findViewById<ProgressBar>(R.id.progressLogin)
        val tvGoToRegister  = findViewById<TextView>(R.id.tvGoToRegister)
        val layoutEmailForm = findViewById<LinearLayout>(R.id.layoutEmailForm)

        // Kayıt ol linkine git
        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Google ile giriş — şimdilik yakında gelecek mesajı
        btnLoginGoogle.setOnClickListener {
            Toast.makeText(this, "Google girişi yakında eklenecek!", Toast.LENGTH_SHORT).show()
        }

        // E-posta ile giriş butonuna basınca form alanlarını göster/gizle
        btnLoginEmail.setOnClickListener {
            if (layoutEmailForm.visibility == View.GONE) {
                layoutEmailForm.visibility = View.VISIBLE
            } else {
                layoutEmailForm.visibility = View.GONE
            }
        }

        // Giriş yap butonuna basınca doğrulama ve login işlemi
        btnLogin.setOnClickListener {
            tilEmail.error = null
            tilPassword.error = null

            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            viewModel.login(email, password)
        }

        // ---- Durum gözlemi ----
        viewModel.authState.observe(this) { state ->
            when (state) {

                is AuthState.Loading -> {
                    btnLogin.isEnabled = false
                    progressBar.visibility = View.VISIBLE
                }

                is AuthState.LoginSuccess -> {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true

                    // "Beni Hatırla" seçiliyse kalıcı, seçilmemişse yine de oturum için geçici kaydet
                    // MainActivity userId'yi SharedPreferences'tan okuduğu için her durumda yazılmalı
                    prefs.edit().putLong(KEY_USER_ID, state.userId).apply()

                    // PIN aktifse giriş sonrasında da PIN ekranı göster
                    if (PinActivity.isPinAktif(this)) {
                        startActivityForResult(
                            Intent(this, PinActivity::class.java).apply {
                                putExtra(PinActivity.EXTRA_MODE, PinActivity.MODE_CHECK)
                            },
                            REQ_PIN_CHECK
                        )
                    } else {
                        navigateToMain(state.userId)
                    }
                    viewModel.resetState()
                }

                is AuthState.ValidationError -> {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
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
                    when (state.field) {
                        ValidationField.EMAIL    -> tilEmail.error = state.message
                        ValidationField.PASSWORD -> tilPassword.error = state.message
                        else                     -> tilPassword.error = state.message
                    }
                    viewModel.resetState()
                }

                else -> {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                }
            }
        }
    }

    // PIN doğrulama ekranından dönünce — doğruysa ana ekrana geç, yanlışsa kapat
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_PIN_CHECK) {
            if (resultCode == RESULT_OK) {
                // PIN doğrulandı → ana ekrana geç
                MainActivity.pinDogrulandi = true
                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val savedUserId = prefs.getLong(KEY_USER_ID, -1L)
                navigateToMain(savedUserId)
            } else {
                // PIN iptal edildi — uygulama kapansın
                finishAffinity()
            }
        }
    }

    // MainActivity'ye geçiş
    private fun navigateToMain(userId: Long) {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
