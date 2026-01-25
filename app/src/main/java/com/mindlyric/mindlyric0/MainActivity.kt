package com.mindlyric.mindlyric0

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.mindlyric.mindlyric0.data.local.entity.JournalEntryEntity
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider
import com.mindlyric.mindlyric0.data.local.db.DbProvider
import com.mindlyric.mindlyric0.data.repository.JournalRepository
import com.mindlyric.mindlyric0.ui.journal.JournalViewModel
import com.mindlyric.mindlyric0.ui.journal.JournalViewModelFactory
import kotlinx.coroutines.launch
import android.util.Log

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val db = DbProvider.get(this)
        val repo = JournalRepository(db.journalDao())
        val factory = JournalViewModelFactory(repo)
        val vm = ViewModelProvider(this, factory)[JournalViewModel::class.java]

// 1) MVVM üzerinden insert
        vm.addDummyEntry()

// 2) DB listesini VM üzerinden dinleyip log basalım
        lifecycleScope.launch {
            vm.entries.collect { list ->
                Log.d(
                    "MVVM_TEST",
                    "entries size=${list.size} last=${list.firstOrNull()?.text}"
                )
            }
        }

    }
}