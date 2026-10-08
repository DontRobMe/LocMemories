package com.ynov.locmemories.ui.detail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ynov.locmemories.R
import com.ynov.locmemories.data.Note
import com.ynov.locmemories.databinding.ActivityNoteDetailBinding
import com.ynov.locmemories.ui.NotesMap
import com.ynov.locmemories.ui.formatCoordinates
import com.ynov.locmemories.ui.formatDate
import com.ynov.locmemories.ui.spokenCoordinates
import kotlinx.coroutines.launch
import java.io.File

/** Détail d'une note (vue de [NoteDetailViewModel]) ; se ferme si la note n'existe plus. */
class NoteDetailActivity : AppCompatActivity() {

    private val viewModel: NoteDetailViewModel by viewModels { NoteDetailViewModel.Factory }
    private lateinit var binding: ActivityNoteDetailBinding
    private lateinit var map: NotesMap
    private var note: Note? = null
    private var bars = Insets.NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityNoteDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        map = NotesMap(binding.map, interactive = false)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            applyInsets()
            insets
        }
        binding.back.setOnClickListener { finish() }
        binding.share.setOnClickListener { note?.let(::share) }
        binding.delete.setOnClickListener { note?.let(::confirmDelete) }
        binding.openMaps.setOnClickListener { note?.let(::openInMaps) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when {
                        state.closed -> finish()
                        state.note != null -> show(state.note)
                    }
                }
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

    private fun show(note: Note) {
        this.note = note
        val hasPhoto = note.photoPath != null
        binding.photoContainer.visibility = if (hasPhoto) View.VISIBLE else View.GONE
        note.photoPath?.let { binding.photo.load(File(it)) }
        binding.photo.contentDescription = getString(R.string.detail_photo_description, note.title)

        binding.title.text = note.title
        binding.date.text = formatDate(note.date)
        binding.content.text = note.content
        binding.content.visibility = if (note.content.isBlank()) View.GONE else View.VISIBLE

        val located = note.hasLocation
        binding.coordinatesRow.visibility = if (located) View.VISIBLE else View.GONE
        binding.locationCard.visibility = if (located) View.VISIBLE else View.GONE
        binding.noLocation.visibility = if (located) View.GONE else View.VISIBLE
        if (located) {
            binding.coordinates.text = formatCoordinates(note.latitude!!, note.longitude!!)
            binding.coordinatesRow.contentDescription = spokenCoordinates(note.latitude, note.longitude)
            map.setNotes(listOf(note))
        }
        applyInsets()
    }

    /** Sans photo, le contenu commence sous la barre de boutons, elle-même sous la barre d'état. */
    private fun applyInsets() {
        val density = resources.displayMetrics.density
        val hasPhoto = note?.photoPath != null
        binding.topBar.updatePadding(top = bars.top + (4 * density).toInt())
        binding.body.updatePadding(
            top = if (hasPhoto) (20 * density).toInt() else bars.top + (72 * density).toInt(),
            bottom = bars.bottom + (20 * density).toInt(),
        )
    }

    // region Actions

    private fun confirmDelete(note: Note) {
        MaterialAlertDialogBuilder(this)
            .setIcon(R.drawable.ic_delete)
            .setTitle(R.string.detail_delete_title)
            .setMessage(getString(R.string.detail_delete_message, note.title))
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.detail_delete_confirm) { _, _ -> viewModel.delete() }
            .show()
    }

    /** URI `geo:` : ouvre l'application de cartographie choisie par l'utilisateur. */
    private fun openInMaps(note: Note) {
        val lat = note.latitude ?: return
        val lon = note.longitude ?: return
        val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(${Uri.encode(note.title)})")
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.detail_no_maps_app, Toast.LENGTH_SHORT).show()
        }
    }

    private fun share(note: Note) {
        val text = buildString {
            appendLine(note.title)
            if (note.content.isNotBlank()) appendLine().appendLine(note.content)
            if (note.hasLocation) {
                appendLine()
                append("https://www.openstreetmap.org/?mlat=${note.latitude}&mlon=${note.longitude}#map=17/${note.latitude}/${note.longitude}")
            }
        }
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, note.title)
            .putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(send, getString(R.string.detail_share)))
    }

    // endregion

    companion object {
        fun start(context: Context, id: Long) {
            context.startActivity(
                Intent(context, NoteDetailActivity::class.java).putExtra(NoteDetailViewModel.EXTRA_ID, id)
            )
        }
    }
}
