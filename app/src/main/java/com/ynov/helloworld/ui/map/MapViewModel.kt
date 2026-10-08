package com.ynov.helloworld.ui.map

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
 * État de la carte.
 *
 * @property notes notes géolocalisées, de la plus récente à la plus ancienne.
 * @property selected note mise en avant dans le carrousel, ou `null`.
 */
data class MapState(
    val notes: List<Note> = emptyList(),
    val selected: Note? = null,
)

/** ViewModel de la carte : notes géolocalisées et note sélectionnée (conservée à la rotation). */
class MapViewModel(
    repository: NoteRepository,
    private val handle: SavedStateHandle,
) : ViewModel() {

    private val selectedId = handle.getStateFlow<Long?>(KEY_SELECTED, null)

    val state: StateFlow<MapState> =
        combine(repository.notes, selectedId) { notes, selectedId ->
            val located = notes.filter { it.hasLocation }
            MapState(notes = located, selected = located.find { it.id == selectedId })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapState())

    /** Met la note en avant : la carte se centre dessus. */
    fun select(note: Note) {
        handle[KEY_SELECTED] = note.id
    }

    companion object {
        private const val KEY_SELECTED = "selected"

        /** Fabrique : repository de l'[com.ynov.helloworld.App] et état sauvegardé. */
        val Factory = viewModelFactory {
            initializer { MapViewModel(this[APPLICATION_KEY]!!.app.repository, createSavedStateHandle()) }
        }
    }
}
