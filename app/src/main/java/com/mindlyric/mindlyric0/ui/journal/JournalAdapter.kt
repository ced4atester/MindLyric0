package com.mindlyric.mindlyric0.ui.journal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        // Uzun basınca silme callback'i tetiklenir
        holder.itemView.setOnLongClickListener {
            onDelete(entry)
            true
        }
    }
}
