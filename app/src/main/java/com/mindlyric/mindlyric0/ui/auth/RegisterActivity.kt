package com.mindlyric.mindlyric0.ui.auth

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mindlyric.mindlyric0.R
import com.mindlyric.mindlyric0.data.local.db.DbProvider
import com.mindlyric.mindlyric0.data.local.entity.UserEntity
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val usernameInput = findViewById<EditText>(R.id.etUsername)
        val emailInput = findViewById<EditText>(R.id.etEmail)
        val passwordInput = findViewById<EditText>(R.id.etPassword)
        val registerButton = findViewById<Button>(R.id.btnRegister)

        val db = DbProvider.get(this)
        val userDao = db.userDao()

        registerButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Tüm alanları doldurun", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val existingUser = userDao.getUserByEmail(email)

                if (existingUser != null) {
                    runOnUiThread {
                        Toast.makeText(
                            this@RegisterActivity,
                            "Bu e-posta zaten kayıtlı",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    userDao.insertUser(
                        UserEntity(
                            username = username,
                            email = email,
                            password = password
                        )
                    )

                    runOnUiThread {
                        Toast.makeText(
                            this@RegisterActivity,
                            "Kayıt başarılı",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            }
        }
    }
}