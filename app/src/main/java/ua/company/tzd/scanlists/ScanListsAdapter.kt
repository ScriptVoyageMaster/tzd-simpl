package ua.company.tzd.scanlists

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ua.company.tzd.R
import ua.company.tzd.databinding.ItemScanListBinding
import ua.company.tzd.model.ScanListInfo

/**
 * Адаптер показує простий перелік списків з діями «відкрити» та «видалити».
 * Докладні коментарі допомагають зрозуміти, що саме робить кожен крок.
 */
class ScanListsAdapter(
    private val onOpen: (ScanListInfo) -> Unit,
    private val onDelete: (ScanListInfo) -> Unit
) : RecyclerView.Adapter<ScanListsAdapter.ListViewHolder>() {

    private val items: MutableList<ScanListInfo> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemScanListBinding.inflate(inflater, parent, false)
        return ListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    /**
     * Оновлюємо список повністю, бо кількість елементів невелика і так простіше.
     */
    fun submitList(newItems: List<ScanListInfo>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class ListViewHolder(private val binding: ItemScanListBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(info: ScanListInfo) {
            binding.tvName.text = info.name
            binding.root.contentDescription = binding.root.context.getString(
                R.string.scan_lists_open_cd,
                info.name
            )
            binding.root.setOnClickListener { onOpen(info) }
            binding.btnDelete.setOnClickListener { onDelete(info) }
        }
    }
}
