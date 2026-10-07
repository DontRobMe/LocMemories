package com.ynov.helloworld

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.ynov.helloworld.data.AppPreferences
import com.ynov.helloworld.data.Note
import com.ynov.helloworld.data.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

/**
 * Source de vérité de l'application : expose la liste des notes et les opérations associées.
 *
 * Chaque modification met à jour l'état en mémoire puis est immédiatement persistée
 * via [NoteRepository].
 */
class NotesViewModel(application: Application) : AndroidViewModel(application) {

    /** Préférences de l'application (introduction déjà vue…). */
    val preferences = AppPreferences(application)

    /** Accès au stockage (exposé pour la gestion des photos à l'écran d'ajout). */
    val repository = NoteRepository(application)

    private val _notes = MutableStateFlow(repository.load().sortedByDescending { it.date })

    /** Notes triées de la plus récente à la plus ancienne. */
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

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
        repository.save(_notes.value)
    }

    /** Supprime la note d'identifiant [id] ainsi que sa photo. */
    fun deleteNote(id: Long) {
        getNote(id)?.photoPath?.let { File(it).delete() }
        _notes.update { list -> list.filterNot { it.id == id } }
        repository.save(_notes.value)
    }

    // endregion
}
