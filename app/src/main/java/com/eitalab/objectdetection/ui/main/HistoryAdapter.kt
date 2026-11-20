package com.eitalab.objectdetection.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eitalab.objectdetection.databinding.ItemDetectionHistoryBinding
import com.eitalab.objectdetection.src.services.DetectionHistoryItem

class HistoryAdapter(
    private var items: List<DetectionHistoryItem> = emptyList()
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemDetectionHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDetectionHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvItemLabel.text = item.label
        holder.binding.tvItemDate.text = item.getFormattedDate()
        holder.binding.chipItemConfidence.text = "${item.getConfidencePercent()}%"
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<DetectionHistoryItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
