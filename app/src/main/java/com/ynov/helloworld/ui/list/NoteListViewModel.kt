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
 * État de l'écran de liste.
 *
 * @property loading `true` tant que les notes sont en cours de chargement.
 * @property total nombre total de notes, filtre non compris.
 * @property notes notes correspondant à la recherche.
 * @property query recherche en cours (vide = toutes les notes).
 */
data class NoteListState(
    val loading: Boolean = true,
    val total: Int = 0,
    val notes: List<Note> = emptyList(),
    val query: String = "",
) {
    /** Le carnet ne contient aucune note (et le chargement est terminé). */
    val empty: Boolean get() = !loading && total == 0
}

/** ViewModel de la liste : notes filtrées par la recherche. */
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

    /** `true` tant que l'introduction n'a jamais été vue. */
    val onboardingNeeded: Boolean get() = !preferences.onboardingDone

    /** Filtre la liste sur le titre et le contenu, sans tenir compte de la casse. */
    fun search(text: String) {
        query.value = text
    }

    private fun Note.matches(query: String) =
        query.isBlank() || title.contains(query, ignoreCase = true) || content.contains(query, ignoreCase = true)

    companion object {
        /** Fabrique : fournit les dépendances portées par l'[com.ynov.helloworld.App]. */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!.app
                NoteListViewModel(app.repository, app.preferences)
            }
        }
    }
}
