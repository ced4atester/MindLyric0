package com.mindlyric.mindlyric0.ui.journal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mindlyric.mindlyric0.R
import com.mindlyric.mindlyric0.data.local.entity.JournalEntryEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Günlük girdilerini RecyclerView'a bağlayan adapter.
 * ListAdapter kullanılır: DiffUtil sayesinde sadece değişen satırlar yeniden çizilir,
 * tüm liste resetlenmez → daha performanslı animasyon ve güncelleme.
 *
 * @param onDelete Kullanıcı uzun basınca çağrılır; silinecek entry iletilir.
 */
class JournalAdapter(
    private val onDelete: (JournalEntryEntity) -> Unit
) : ListAdapter<JournalEntryEntity, JournalAdapter.ViewHolder>(DiffCallback) {

    // DiffUtil: iki liste arasındaki farkı hesaplar, sadece değişen öğeler güncellenir
    companion object DiffCallback : DiffUtil.ItemCallback<JournalEntryEntity>() {
        override fun areItemsTheSame(oldItem: JournalEntryEntity, newItem: JournalEntryEntity) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: JournalEntryEntity, newItem: JournalEntryEntity) =
            oldItem == newItem
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvEntryDate)
        val tvMood: TextView = view.findViewById(R.id.tvEntryMood)
        val tvText: TextView = view.findViewById(R.id.tvEntryText)
        // Duygu analizi göstergeleri
        val layoutSentiment: View = view.findViewById(R.id.layoutSentiment)
        val progressSentiment: ProgressBar = view.findViewById(R.id.progressSentiment)
        val tvSentimentScore: TextView = view.findViewById(R.id.tvSentimentScore)
        // Claude öneri mesajı
        val tvRecommendation: TextView = view.findViewById(R.id.tvRecommendation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_journal_entry, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = getItem(position)

        // Tarihi okunabilir formata çevir; Türkçe locale kullanılır
        val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("tr"))
        holder.tvDate.text = formatter.format(Date(entry.createdAt))

        // Ruh hali varsa göster, yoksa alanı gizle (layout'ta yer kaplamasın)
        if (entry.moodLabel != null) {
            holder.tvMood.visibility = View.VISIBLE
            holder.tvMood.text = entry.moodLabel
        } else {
            holder.tvMood.visibility = View.GONE
        }

        holder.tvText.text = entry.text

        // Duygu skoru: Claude API analiz ettiyse göster, yoksa gizle
        val score = entry.sentimentScore
        if (score != null) {
            holder.layoutSentiment.visibility = View.VISIBLE

            // Skoru 1-10 → 0-100 arasına çevir (progress bar için)
            val progress = ((score - 1f) / 9f * 100).toInt()
            holder.progressSentiment.progress = progress

            // Her skora farklı emoji: 1-3 negatif, 4-6 nötr, 7-10 pozitif
            val emoji = when (score.toInt()) {
                1    -> "😭"
                2    -> "😢"
                3    -> "😟"
                4    -> "😕"
                5    -> "😐"
                6    -> "🙂"
                7    -> "😊"
                8    -> "😄"
                9    -> "🤩"
                10   -> "🥳"
                else -> "😐"
            }
            // Emoji + skor göster (örnek: 😊 7/10)
            holder.tvSentimentScore.text = "$emoji ${score.toInt()}/10"
        } else {
            holder.layoutSentiment.visibility = View.GONE
        }

        // Öneri mesajı: Claude ürettiyse göster, yoksa gizle
        val rec = entry.recommendation
        if (rec != null) {
            holder.tvRecommendation.visibility = View.VISIBLE
            holder.tvRecommendation.text = "💬 $rec"
        } else {
            holder.tvRecommendation.visibility = View.GONE
        }

        // Uzun basınca silme callback'i tetiklenir
        holder.itemView.setOnLongClickListener {
            onDelete(entry)
            true
        }
    }
}
