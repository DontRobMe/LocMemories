package com.ynov.helloworld.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Point d'accès unique aux notes pour les ViewModels (une instance par application,
 * voir [com.ynov.helloworld.App]).
 *
 * L'état en mémoire est mis à jour immédiatement, puis persisté en arrière-plan via
 * [NoteStorage]. Les sauvegardes sont sérialisées par un [Mutex] : elles s'exécutent
 * une à une, dans l'ordre des modifications.
 */
class NoteRepository(context: Context) {

    private val storage = NoteStorage(context.applicationContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val saveMutex = Mutex()

    private val _notes = MutableStateFlow<List<Note>>(emptyList())

    /** Notes triées de la plus récente à la plus ancienne. */
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _loaded = MutableStateFlow(false)

    /** `true` une fois les notes chargées depuis le stockage. */
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    init {
        scope.launch {
            val stored = storage.load().sortedByDescending { it.date }
            _notes.update { current -> current + stored }
            _loaded.value = true
        }
    }

    // region Notes

    /** Retourne la note d'identifiant [id], ou `null` si elle n'existe pas. */
    fun getNote(id: Long): Note? = _notes.value.find { it.id == id }

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
        if (photoPath != null) discardPhoto(photoPath)
    }

    /**
     * Enregistre l'état courant en arrière-plan, après les sauvegardes déjà en attente.
     *
     * Attend la fin du chargement initial : une sauvegarde prématurée écraserait
     * les notes encore en cours de lecture.
     */
    private fun persist() {
        scope.launch {
            _loaded.first { it }
            saveMutex.withLock { storage.save(_notes.value) }
        }
    }

    // endregion

    // region Photos

    /** Fichier vide destiné à recevoir une photo de l'appareil photo. */
    fun newPhotoFile(): File = storage.newPhotoFile()

    /** Copie et optimise une image choisie dans la galerie ; renvoie son chemin local. */
    suspend fun importPhoto(uri: Uri): String = storage.importPhoto(uri).absolutePath

    /** Optimise une photo de l'appareil photo une fois la prise de vue terminée. */
    suspend fun optimizePhoto(path: String) = storage.optimizePhoto(File(path))

    /** Supprime une photo en arrière-plan (note supprimée ou brouillon abandonné). */
    fun discardPhoto(path: String) {
        scope.launch { storage.deletePhoto(path) }
    }

    // endregion
}
