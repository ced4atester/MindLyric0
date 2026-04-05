package com.mindlyric.mindlyric0.ui.profile

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.textfield.TextInputEditText
import com.mindlyric.mindlyric0.R
import com.mindlyric.mindlyric0.data.local.db.DbProvider
import com.mindlyric.mindlyric0.data.repository.JournalRepository
import com.mindlyric.mindlyric0.data.repository.UserRepository
import com.mindlyric.mindlyric0.ui.auth.LoginActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var viewModel: ProfileViewModel

    // 20 avatar görseli — görselleri drawable klasörüne ekledikten sonra buradaki liste aktif olur
    // Şu an varsayılan Android ikonları ile placeholder olarak çalışır
    private val avatarList = listOf(
        R.drawable.avatar_1,  R.drawable.avatar_2,  R.drawable.avatar_3,
        R.drawable.avatar_4,  R.drawable.avatar_5,  R.drawable.avatar_6,
        R.drawable.avatar_7,  R.drawable.avatar_8,  R.drawable.avatar_9,
        R.drawable.avatar_10, R.drawable.avatar_11, R.drawable.avatar_12,
        R.drawable.avatar_13, R.drawable.avatar_14, R.drawable.avatar_15,
        R.drawable.avatar_16, R.drawable.avatar_17, R.drawable.avatar_18,
        R.drawable.avatar_19, R.drawable.avatar_20
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // SharedPreferences'tan giriş yapan kullanıcının id'sini oku
        val prefs = getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val userId = prefs.getLong(LoginActivity.KEY_USER_ID, -1L)

        // Geçerli kullanıcı yoksa Login'e gönder
        if (userId == -1L) {
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            return
        }

        // ---- ViewModel kurulumu ----
        val userRepository    = UserRepository(DbProvider.get(this).userDao())
        val journalRepository = JournalRepository(DbProvider.get(this).journalDao())
        viewModel = ViewModelProvider(
            this, ProfileViewModelFactory(userRepository, journalRepository, userId)
        )[ProfileViewModel::class.java]

        // ---- View referansları ----
        val ivAvatar        = findViewById<ImageView>(R.id.ivAvatar)
        val tvUsername      = findViewById<TextView>(R.id.tvUsername)
        val tvEmail         = findViewById<TextView>(R.id.tvEmail)
        val tvJournalCount  = findViewById<TextView>(R.id.tvJournalCount)
        val tvTopMood       = findViewById<TextView>(R.id.tvTopMood)
        val tvBack          = findViewById<TextView>(R.id.tvBack)
        val btnChooseAvatar  = findViewById<View>(R.id.btnChooseAvatar)
        val btnChangePass    = findViewById<View>(R.id.btnChangePassword)
        val btnDeleteAccount = findViewById<View>(R.id.btnDeleteAccount)

        // Geri butonuna basınca bu ekranı kapat
        tvBack.setOnClickListener { finish() }

        // ---- Kullanıcı bilgilerini gözlemle ----
        viewModel.user.observe(this) { user ->
            if (user == null) return@observe
            tvUsername.text = user.username
            tvEmail.text    = user.email

            // Avatarı göster — seçilmişse kullan, yoksa varsayılan ikon
            if (user.avatarResId != null) {
                ivAvatar.setImageResource(user.avatarResId)
            } else {
                ivAvatar.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }

        // ---- İstatistikleri gözlemle ----
        viewModel.journalCount.observe(this) { count ->
            tvJournalCount.text = count.toString()
        }

        viewModel.topMood.observe(this) { mood ->
            tvTopMood.text = mood ?: getString(R.string.profile_no_mood)
        }

        // ---- Trend grafiğini gözlemle ----
        val moodChart = findViewById<BarChart>(R.id.moodChart)
        viewModel.trendEntries.observe(this) { entries ->
            setupMoodChart(moodChart, entries.mapNotNull { entry ->
                val score = entry.sentimentScore ?: return@mapNotNull null
                Pair(entry.createdAt, score)
            })
        }

        // ---- Yarın tahmini gözlemle ----
        val cardPrediction = findViewById<View>(R.id.cardPrediction)
        val tvPrediction = findViewById<TextView>(R.id.tvPrediction)
        viewModel.prediction.observe(this) { prediction ->
            if (prediction != null) {
                cardPrediction.visibility = View.VISIBLE
                tvPrediction.text = prediction
            } else {
                cardPrediction.visibility = View.GONE
            }
        }



        // ---- Avatar seçme ----
        btnChooseAvatar.setOnClickListener {
            showAvatarDialog()
        }

        // ---- Şifre değiştirme ----
        btnChangePass.setOnClickListener {
            showChangePasswordDialog()
        }

        // ---- Hesap silme ----
        btnDeleteAccount.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.profile_delete_confirm_title))
                .setMessage(getString(R.string.profile_delete_confirm_msg))
                .setPositiveButton(getString(R.string.profile_delete_confirm_btn)) { _, _ ->
                    viewModel.deleteAccount()
                }
                .setNegativeButton(getString(R.string.profile_cancel), null)
                .show()
        }

        // ---- State gözlemi ----
        viewModel.state.observe(this) { state ->
            when (state) {
                is ProfileState.PasswordChanged -> {
                    Toast.makeText(this, getString(R.string.profile_password_success), Toast.LENGTH_SHORT).show()
                    viewModel.resetState()
                }
                is ProfileState.AccountDeleted -> {
                    // Hesap silindi — SharedPreferences'ı temizle ve Login'e yönlendir
                    prefs.edit().remove(LoginActivity.KEY_USER_ID).apply()
                    startActivity(Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }
                is ProfileState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    viewModel.resetState()
                }
                else -> { /* Idle ve Loading için bir şey yapma */ }
            }
        }
    }

    // Son 7 günün duygu skorlarını çubuk grafiğe yükler
    private fun setupMoodChart(chart: BarChart, data: List<Pair<Long, Float>>) {
        if (data.isEmpty()) {
            chart.setNoDataText("Henüz analiz edilmiş günlük yok")
            chart.invalidate()
            return
        }

        // Her çubuk: X = sıra indeksi, Y = skor (1-10)
        val barEntries = data.mapIndexed { index, (_, score) ->
            BarEntry(index.toFloat(), score)
        }

        // Skora göre renk belirle: 1-3 kırmızı, 4-6 sarı, 7-10 yeşil
        val colors = data.map { (_, score) ->
            when {
                score <= 3f -> Color.parseColor("#EF5350") // kırmızı — negatif
                score <= 6f -> Color.parseColor("#FFA726") // turuncu — nötr
                else        -> Color.parseColor("#66BB6A") // yeşil — pozitif
            }
        }

        val dataSet = BarDataSet(barEntries, "").apply {
            setColors(colors)
            setDrawValues(true)       // çubukların üstüne skor yaz
            valueTextSize = 11f
            // Skoru tam sayı olarak göster (7.0 yerine 7)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float) = value.toInt().toString()
            }
        }

        // X eksenine tarih yaz
        val dateFormatter = SimpleDateFormat("dd/MM", Locale("tr"))
        chart.xAxis.apply {
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    return if (index in data.indices) {
                        dateFormatter.format(Date(data[index].first))
                    } else ""
                }
            }
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
        }

        // Y eksenini 0-10 aralığına sabitle
        chart.axisLeft.apply {
            axisMinimum = 0f
            axisMaximum = 10f
            granularity = 1f
        }
        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setFitBars(true)
        chart.data = BarData(dataSet)
        chart.animateY(600)
        chart.invalidate()
    }

    // 20 avatar görseli gösteren dialog
    private fun showAvatarDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_avatar_picker, null)
        val gridLayout = dialogView.findViewById<GridLayout>(R.id.gridAvatars)

        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.profile_choose_avatar))
            .setView(dialogView)
            .setNegativeButton(getString(R.string.profile_cancel), null)
            .create()

        // Her avatar için grid'e bir ImageView ekle
        avatarList.forEach { resId ->
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_avatar, gridLayout, false)
            val ivItem = itemView.findViewById<ImageView>(R.id.ivAvatarItem)
            ivItem.setImageResource(resId)

            // Seçilen avatarı kaydet ve dialogu kapat
            ivItem.setOnClickListener {
                viewModel.updateAvatar(resId)
                dialog.dismiss()
            }
            gridLayout.addView(itemView)
        }

        dialog.show()
    }

    // Şifre değiştirme dialogu
    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.profile_change_password))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.profile_save_password)) { _, _ ->
                val oldPass = dialogView.findViewById<TextInputEditText>(R.id.etOldPassword)
                    .text.toString().trim()
                val newPass = dialogView.findViewById<TextInputEditText>(R.id.etNewPassword)
                    .text.toString().trim()
                viewModel.changePassword(oldPass, newPass)
            }
            .setNegativeButton(getString(R.string.profile_cancel), null)
            .show()
    }
}
