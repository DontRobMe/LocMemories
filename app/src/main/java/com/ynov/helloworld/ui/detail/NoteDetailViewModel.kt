package com.ynov.helloworld.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ynov.helloworld.app
import com.ynov.helloworld.data.Note
import com.ynov.helloworld.data.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * État de l'écran de détail.
 *
 * @property note note affichée, `null` pendant le chargement ou si elle n'existe plus.
 * @property closed `true` si la note n'existe pas (ou plus) : l'écran doit se fermer.
 */
data class NoteDetailState(
    val note: Note? = null,
    val closed: Boolean = false,
)

/**
 * ViewModel du détail d'une note.
 *
 * L'identifiant de la note est lu dans le [SavedStateHandle], alimenté par les extras
 * de l'intent ([NoteDetailActivity.start]).
 */
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

    /** Supprime la note et sa photo ; l'état passe alors à [NoteDetailState.closed]. */
    fun delete() = repository.deleteNote(id)

    companion object {
        /** Clé de l'identifiant de la note dans l'intent et le [SavedStateHandle]. */
        const val EXTRA_ID = "note_id"

        /** Fabrique : repository de l'[com.ynov.helloworld.App] et extras de l'intent. */
        val Factory = viewModelFactory {
            initializer { NoteDetailViewModel(this[APPLICATION_KEY]!!.app.repository, createSavedStateHandle()) }
        }
    }
}
