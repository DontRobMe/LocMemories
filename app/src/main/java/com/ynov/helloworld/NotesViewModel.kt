package com.ynov.helloworld

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ynov.helloworld.data.AppPreferences
import com.ynov.helloworld.data.Note
import com.ynov.helloworld.data.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Source de vérité de l'application : expose la liste des notes et les opérations associées.
 *
 * L'état en mémoire est mis à jour immédiatement (interface réactive), puis persisté
 * en arrière-plan via [NoteRepository]. Les sauvegardes sont sérialisées par un [Mutex] :
 * elles s'exécutent une à une, dans l'ordre des modifications.
 */
class NotesViewModel(application: Application) : AndroidViewModel(application) {

    /** Préférences de l'application (introduction déjà vue…). */
    val preferences = AppPreferences(application)

    /** Accès au stockage (exposé pour la gestion des photos à l'écran d'ajout). */
    val repository = NoteRepository(application)

    private val _notes = MutableStateFlow<List<Note>>(emptyList())

    /** Notes triées de la plus récente à la plus ancienne. */
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _loaded = MutableStateFlow(false)

    /** `true` une fois les notes chargées depuis le stockage. */
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val saveMutex = Mutex()

    init {
        viewModelScope.launch {
            val stored = repository.load().sortedByDescending { it.date }
            _notes.update { current -> current + stored }
            _loaded.value = true
        }
    }

    // region Lecture

    /** Retourne la note d'identifiant [id], ou `null` si elle n'existe pas. */
    fun getNote(id: Long): Note? = _notes.value.find { it.id == id }

    // endregion

    // region Écriture

    /**
     * Crée une note datée de maintenant et l'ajoute en tête de liste.
     *
     * @param photoPath chemin de la photo déjà copiée dans le stockage interne, ou `null`.
     * @param latitude latitude du lieu d'écriture, ou `null` si indisponible.
     * @param longitude longitude du lieu d'écriture, ou `null` si indisponible.
     */
    fun addNote(
        title: String,
        content: String,
        photoPath: String?,
        latitude: Double?,
        longitude: Double?,
    ) {
        val now = System.currentTimeMillis()
        val note = Note(
            id = now,
            title = title,
            content = content,
            photoPath = photoPath,
            date = now,
            latitude = latitude,
            longitude = longitude,
        )
        _notes.update { listOf(note) + it }
        persist()
    }

    /** Supprime la note d'identifiant [id] ainsi que sa photo. */
    fun deleteNote(id: Long) {
        val photoPath = getNote(id)?.photoPath
        _notes.update { list -> list.filterNot { it.id == id } }
        persist()
        if (photoPath != null) viewModelScope.launch { repository.deletePhoto(photoPath) }
    }

    /**
     * Enregistre l'état courant en arrière-plan, après les sauvegardes déjà en attente.
     *
     * Attend la fin du chargement initial : une sauvegarde prématurée écraserait
     * les notes encore en cours de lecture.
     */
    private fun persist() {
        viewModelScope.launch {
            _loaded.first { it }
            saveMutex.withLock { repository.save(_notes.value) }
        }
    }

    // endregion
}
