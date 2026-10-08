package com.ynov.locmemories.ui.map

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

data class MapState(
    val notes: List<Note> = emptyList(),
    val selected: Note? = null,
)

/** La note sélectionnée est gardée dans le [SavedStateHandle] pour survivre à la rotation. */
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

    fun select(note: Note) {
        handle[KEY_SELECTED] = note.id
    }

    companion object {
        private const val KEY_SELECTED = "selected"

        val Factory = viewModelFactory {
            initializer { MapViewModel(checkNotNull(this[APPLICATION_KEY]).app.repository, createSavedStateHandle()) }
        }
    }
}
