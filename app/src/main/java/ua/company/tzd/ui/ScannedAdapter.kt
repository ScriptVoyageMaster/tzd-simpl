package ua.company.tzd.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ua.company.tzd.databinding.ItemScannedBinding
import ua.company.tzd.model.ScannedItem

/**
 * Адаптер для списку відсканованих кодів з можливістю видалення кожного запису.
 */
class ScannedAdapter(
    private val onDelete: (ScannedItem) -> Unit
) : RecyclerView.Adapter<ScannedAdapter.ScannedViewHolder>() {

    private val items: MutableList<ScannedItem> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScannedViewHolder {
        val binding = ItemScannedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScannedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScannedViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<ScannedItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class ScannedViewHolder(private val binding: ItemScannedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ScannedItem) {
            val resources = binding.root.resources
            binding.tvCode.text = item.code
            binding.tvWeight.text = resources.getString(
                ua.company.tzd.R.string.item_weight_fmt,
                item.kg,
                item.g
            )
            binding.btnDelete.setOnClickListener { onDelete(item) }
        }
    }
}
