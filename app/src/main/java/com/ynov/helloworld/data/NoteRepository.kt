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
 * Source de vérité des notes, partagée par tous les ViewModels (une instance dans [com.ynov.helloworld.App]).
 *
 * L'état en mémoire est mis à jour immédiatement, puis enregistré en arrière-plan par
 * [NoteStorage]. Le [Mutex] garantit que les sauvegardes s'exécutent dans l'ordre.
 */
class NoteRepository(context: Context) {

    private val storage = NoteStorage(context.applicationContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val saveMutex = Mutex()

    private val _notes = MutableStateFlow<List<Note>>(emptyList())

    /** Notes triées de la plus récente à la plus ancienne. */
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _loaded = MutableStateFlow(false)

    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    init {
        scope.launch {
            val stored = storage.load().sortedByDescending { it.date }
            _notes.update { current -> current + stored }
            _loaded.value = true
        }
    }

    // region Notes

    fun getNote(id: Long): Note? = _notes.value.find { it.id == id }

    /** Crée une note datée de maintenant ; [photoPath] doit déjà être dans le stockage interne. */
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

    /** Supprime aussi la photo de la note. */
    fun deleteNote(id: Long) {
        val photoPath = getNote(id)?.photoPath
        _notes.update { list -> list.filterNot { it.id == id } }
        persist()
        if (photoPath != null) discardPhoto(photoPath)
    }

    /**
     * Attend la fin du chargement initial : une sauvegarde lancée avant écraserait
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

    fun newPhotoFile(): File = storage.newPhotoFile()

    suspend fun importPhoto(uri: Uri): String = storage.importPhoto(uri).absolutePath

    suspend fun optimizePhoto(path: String) = storage.optimizePhoto(File(path))

    fun discardPhoto(path: String) {
        scope.launch { storage.deletePhoto(path) }
    }

    // endregion
}
