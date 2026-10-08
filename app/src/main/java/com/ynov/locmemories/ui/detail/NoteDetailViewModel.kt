package com.ynov.locmemories.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ynov.locmemories.app
import com.ynov.locmemories.data.Note
import com.ynov.locmemories.data.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * @property note `null` pendant le chargement ou si la note n'existe plus.
 * @property closed la note n'existe pas (ou plus) : l'écran doit se fermer.
 */
data class NoteDetailState(
    val note: Note? = null,
    val closed: Boolean = false,
)

/** L'identifiant de la note vient des extras de l'intent, via le [SavedStateHandle]. */
class NoteDetailViewModel(
    private val repository: NoteRepository,
    handle: SavedStateHandle,
) : ViewModel() {

    private val id: Long = handle[EXTRA_ID] ?: -1

    val state: StateFlow<NoteDetailState> =
        combine(repository.notes, repository.loaded) { notes, loaded ->
            val note = notes.find { it.id == id }
            NoteDetailState(note = note, closed = loaded && note == null)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NoteDetailState())

    fun delete() = repository.deleteNote(id)

    companion object {
        const val EXTRA_ID = "note_id"

        val Factory = viewModelFactory {
            initializer { NoteDetailViewModel(this[APPLICATION_KEY]!!.app.repository, createSavedStateHandle()) }
        }
    }
}
