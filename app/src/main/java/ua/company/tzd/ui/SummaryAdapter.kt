package ua.company.tzd.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ua.company.tzd.databinding.ItemSummaryBinding
import ua.company.tzd.model.SummaryItem

/**
 * Адаптер показує агреговану статистику по артикулах.
 */
class SummaryAdapter : RecyclerView.Adapter<SummaryAdapter.SummaryViewHolder>() {

    private val items: MutableList<SummaryItem> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryViewHolder {
        val binding = ItemSummaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SummaryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SummaryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    /**
     * Оновлюємо весь список, коли з'являються нові підсумки.
     */
    fun submitList(newItems: List<SummaryItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class SummaryViewHolder(private val binding: ItemSummaryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SummaryItem) {
            val resources = binding.root.resources
            binding.tvSummary.text = resources.getString(
                ua.company.tzd.R.string.summary_row_fmt,
                item.article,
                item.count,
                item.kg,
                item.g
            )
        }
    }
}
