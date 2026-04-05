package com.mindlyric.mindlyric0

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.mindlyric.mindlyric0.data.local.db.DbProvider
import com.mindlyric.mindlyric0.data.repository.JournalRepository
import com.mindlyric.mindlyric0.ui.auth.LoginActivity
import com.mindlyric.mindlyric0.ui.journal.JournalAdapter
import com.mindlyric.mindlyric0.ui.journal.JournalViewModel
import com.mindlyric.mindlyric0.ui.journal.JournalViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: JournalViewModel
    private lateinit var adapter: JournalAdapter

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
    }
}
