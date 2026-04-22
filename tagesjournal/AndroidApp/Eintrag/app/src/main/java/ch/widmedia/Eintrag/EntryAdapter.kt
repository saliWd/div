package ch.widmedia.Eintrag

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ch.widmedia.Eintrag.databinding.ItemEntryBinding

class EntryAdapter(
    private val onClick: (Entry) -> Unit
) : ListAdapter<Entry, EntryAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val b: ItemEntryBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(entry: Entry) {
            b.tvDate.text = entry.date
            b.tvTitle.text = entry.title
            b.tvPreview.text = entry.text.replace('\n', ' ').trim()
            b.root.setOnClickListener { onClick(entry) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Entry>() {
            override fun areItemsTheSame(a: Entry, b: Entry) = a.id == b.id
            override fun areContentsTheSame(a: Entry, b: Entry) = a == b
        }
    }
}
