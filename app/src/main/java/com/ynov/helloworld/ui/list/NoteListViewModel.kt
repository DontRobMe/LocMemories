package com.ynov.helloworld.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ynov.helloworld.app
import com.ynov.helloworld.data.AppPreferences
import com.ynov.helloworld.data.Note
import com.ynov.helloworld.data.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * @property total nombre de notes, filtre non compris.
 * @property notes notes correspondant à la recherche.
 */
data class NoteListState(
    val loading: Boolean = true,
    val total: Int = 0,
    val notes: List<Note> = emptyList(),
    val query: String = "",
) {
    val empty: Boolean get() = !loading && total == 0
}

class NoteListViewModel(
    private val repository: NoteRepository,
    private val preferences: AppPreferences,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val state: StateFlow<NoteListState> =
        combine(repository.notes, repository.loaded, query) { notes, loaded, query ->
            NoteListState(
                loading = !loaded,
                total = notes.size,
                notes = notes.filter { it.matches(query) },
                query = query,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NoteListState())

    val onboardingNeeded: Boolean get() = !preferences.onboardingDone

    /** Sur le titre et le contenu, sans tenir compte de la casse. */
    fun search(text: String) {
        query.value = text
    }

    private fun Note.matches(query: String) =
        query.isBlank() || title.contains(query, ignoreCase = true) || content.contains(query, ignoreCase = true)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!.app
                NoteListViewModel(app.repository, app.preferences)
            }
        }
    }
}
