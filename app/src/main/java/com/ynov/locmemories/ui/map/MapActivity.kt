package com.ynov.locmemories.ui.map

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.R as MaterialR
import com.google.android.material.color.MaterialColors
import com.ynov.locmemories.R
import com.ynov.locmemories.data.Note
import com.ynov.locmemories.databinding.ActivityMapBinding
import com.ynov.locmemories.databinding.ItemMapNoteBinding
import com.ynov.locmemories.ui.NotesMap
import com.ynov.locmemories.ui.detail.NoteDetailActivity
import com.ynov.locmemories.ui.formatShortDate
import com.ynov.locmemories.ui.list.NoteDiff
import kotlinx.coroutines.launch
import java.io.File

// region Écran

/**
 * Carte plein écran (vue de [MapViewModel]). Le carrousel du bas centre la carte sur une note ;
 * il sert aussi d'alternative accessible aux marqueurs, que TalkBack ne peut pas parcourir.
 */
class MapActivity : AppCompatActivity() {

    private val viewModel: MapViewModel by viewModels { MapViewModel.Factory }
    private lateinit var binding: ActivityMapBinding
    private lateinit var map: NotesMap
    private var focusedId: Long? = null
    private val adapter = MapNoteAdapter(
        onFocus = { note -> viewModel.select(note) },
        onOpen = { note -> NoteDetailActivity.start(this, note.id) },
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        map = NotesMap(binding.map) { note -> NoteDetailActivity.start(this, note.id) }

        binding.back.setOnClickListener { finish() }
        binding.carousel.adapter = adapter

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val padding = (12 * resources.displayMetrics.density).toInt()
            binding.topBar.updatePadding(top = bars.top + padding)
            binding.carousel.updatePadding(bottom = bars.bottom + 2 * padding)
            binding.carousel.doOnLayout {
                map.setCoveredInsets(bars.left, binding.topBar.bottom, it.height)
            }
            insets
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.map.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.map.onDetach()
    }

    private fun render(state: MapState) {
        val count = state.notes.size
        binding.count.text = if (count == 0) {
            getString(R.string.map_none)
        } else {
            resources.getQuantityString(R.plurals.map_count, count, count)
        }
        adapter.submitList(state.notes)
        map.setNotes(state.notes)

        val selected = state.selected
        if (selected != null && selected.id != focusedId) {
            focusedId = selected.id
            adapter.selectedId = selected.id
            map.focus(selected)
        }
    }
}

// endregion

// region Carrousel

private class MapNoteAdapter(
    private val onFocus: (Note) -> Unit,
    private val onOpen: (Note) -> Unit,
) : ListAdapter<Note, MapNoteAdapter.Holder>(NoteDiff) {

    class Holder(val binding: ItemMapNoteBinding) : RecyclerView.ViewHolder(binding.root)

    var selectedId: Long? = null
        set(value) {
            field = value
            notifyItemRangeChanged(0, itemCount)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemMapNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val note = getItem(position)
        val selected = note.id == selectedId
        with(holder.binding) {
            root.setOnClickListener { onFocus(note) }
            open.setOnClickListener { onOpen(note) }
            open.contentDescription = root.context.getString(R.string.map_open_note, note.title)
            photo.visibility = if (note.photoPath != null) View.VISIBLE else View.GONE
            note.photoPath?.let { photo.load(File(it)) }
            title.text = note.title
            date.text = formatShortDate(note.date)
            root.setCardBackgroundColor(
                ColorStateList.valueOf(
                    MaterialColors.getColor(
                        root,
                        if (selected) MaterialR.attr.colorPrimaryContainer else MaterialR.attr.colorSurfaceContainerHigh,
                    )
                )
            )
            ViewCompat.setStateDescription(root, if (selected) root.context.getString(R.string.map_selected) else null)
        }
    }
}

// endregion
