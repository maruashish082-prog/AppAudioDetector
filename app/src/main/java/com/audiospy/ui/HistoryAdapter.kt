package com.audiospy.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.audiospy.databinding.ItemHistoryBinding
import com.audiospy.db.AudioLogEntity
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter : ListAdapter<AudioLogEntity, HistoryAdapter.ViewHolder>(DIFF) {

    private val fmt = SimpleDateFormat("MMM dd  HH:mm:ss", Locale.getDefault())

    inner class ViewHolder(private val b: ItemHistoryBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: AudioLogEntity) {
            val icon = if (item.state == "RECORDING") "🎙" else "🔊"
            b.tvAppName.text = "$icon ${item.appName}"
            b.tvState.text = item.state
            b.tvTimestamp.text = fmt.format(Date(item.timestamp))
            b.root.setBackgroundColor(
                if (item.isAlert) Color.parseColor("#FFEBEE") else Color.TRANSPARENT
            )
            b.tvAlert.text = if (item.isAlert) "⚠️ NEW" else ""
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<AudioLogEntity>() {
            override fun areItemsTheSame(a: AudioLogEntity, b: AudioLogEntity) = a.id == b.id
            override fun areContentsTheSame(a: AudioLogEntity, b: AudioLogEntity) = a == b
        }
    }
}
