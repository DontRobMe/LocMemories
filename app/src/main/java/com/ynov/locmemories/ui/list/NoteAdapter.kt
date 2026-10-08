package com.ynov.locmemories.ui.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.ynov.locmemories.data.Note
import com.ynov.locmemories.databinding.ItemNoteBinding
import com.ynov.locmemories.ui.formatShortDate
import java.io.File

class NoteAdapter(private val onClick: (Note) -> Unit) :
    ListAdapter<Note, NoteAdapter.Holder>(NoteDiff) {

    class Holder(val binding: ItemNoteBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val note = getItem(position)
        with(holder.binding) {
            root.setOnClickListener { onClick(note) }
            photo.visibility = if (note.photoPath != null) View.VISIBLE else View.GONE
            note.photoPath?.let { photo.load(File(it)) }
            title.text = note.title
            content.text = note.content
            content.visibility = if (note.content.isBlank()) View.GONE else View.VISIBLE
            date.text = formatShortDate(note.date)
            val located = if (note.hasLocation) View.VISIBLE else View.GONE
            locatedIcon.visibility = located
            this.located.visibility = located
        }
    }
}

object NoteDiff : DiffUtil.ItemCallback<Note>() {
    override fun areItemsTheSame(oldItem: Note, newItem: Note) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Note, newItem: Note) = oldItem == newItem
}
