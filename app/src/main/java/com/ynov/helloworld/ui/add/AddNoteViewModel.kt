package com.ynov.helloworld.ui.add

import android.location.Location
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ynov.helloworld.R
import com.ynov.helloworld.app
import com.ynov.helloworld.data.NoteRepository
import com.ynov.helloworld.location.fetchCurrentLocation
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * @property photoProcessing copie ou optimisation d'une photo en cours.
 * @property titleError l'utilisateur a tenté d'enregistrer sans titre.
 * @property saved note enregistrée : l'écran doit se fermer.
 */
data class AddNoteState(
    val photoPath: String? = null,
    val photoProcessing: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locating: Boolean = false,
    @param:StringRes val locationError: Int? = null,
    val titleError: Boolean = false,
    val saved: Boolean = false,
)

/**
 * La photo et la position sont aussi gardées dans le [SavedStateHandle] : Android peut tuer
 * le processus pendant que l'appareil photo est au premier plan.
 *
 * @param fetchLocation position courante, ou `null` ; simulée dans les tests.
 */
class AddNoteViewModel(
    private val repository: NoteRepository,
    private val fetchLocation: suspend () -> Location?,
    private val handle: SavedStateHandle,
) : ViewModel() {

    private var photoJob: Job? = null

    private val _state = MutableStateFlow(
        AddNoteState(
            photoPath = handle[KEY_PHOTO],
            latitude = handle[KEY_LATITUDE],
            longitude = handle[KEY_LONGITUDE],
        )
    )
    val state: StateFlow<AddNoteState> = _state.asStateFlow()

    val locationNeeded: Boolean get() = _state.value.latitude == null && !_state.value.locating

    // region Position

    /** La permission de localisation doit déjà être accordée. */
    fun locate() {
        _state.update { it.copy(locating = true, locationError = null) }
        viewModelScope.launch {
            val location = fetchLocation()
            if (location == null) {
                // Une position déjà trouvée est conservée si l'actualisation échoue.
                _state.update { it.copy(locating = false, locationError = R.string.add_location_unavailable) }
                return@launch
            }
            handle[KEY_LATITUDE] = location.latitude
            handle[KEY_LONGITUDE] = location.longitude
            _state.update {
                it.copy(locating = false, latitude = location.latitude, longitude = location.longitude)
            }
        }
    }

    fun onLocationDenied() {
        _state.update { it.copy(locating = false, locationError = R.string.add_location_denied) }
    }

    // endregion

    // region Photo

    fun preparePhotoFile(): File = repository.newPhotoFile().also { handle[KEY_PENDING_PHOTO] = it.absolutePath }

    /** Photo optimisée en arrière-plan, ou fichier supprimé si la prise de vue est annulée. */
    fun onPictureTaken(success: Boolean) {
        val pending = handle.remove<String>(KEY_PENDING_PHOTO) ?: return
        if (success) {
            processPhoto { repository.optimizePhoto(pending); pending }
        } else {
            repository.discardPhoto(pending)
        }
    }

    fun onImagePicked(uri: Uri) = processPhoto { repository.importPhoto(uri) }

    fun removePhoto() {
        _state.value.photoPath?.let(repository::discardPhoto)
        setPhoto(null)
    }

    private fun processPhoto(process: suspend () -> String) {
        photoJob = viewModelScope.launch {
            _state.update { it.copy(photoProcessing = true) }
            val path = process()
            _state.value.photoPath?.let(repository::discardPhoto)
            setPhoto(path)
        }
    }

    private fun setPhoto(path: String?) {
        handle[KEY_PHOTO] = path
        _state.update { it.copy(photoPath = path, photoProcessing = false) }
    }

    // endregion

    // region Enregistrement

    /** Attend la fin d'un éventuel traitement de photo. Sans titre, signale l'erreur et n'enregistre rien. */
    fun save(title: String, content: String) {
        if (title.isBlank()) {
            _state.update { it.copy(titleError = true) }
            return
        }
        viewModelScope.launch {
            photoJob?.join()
            val state = _state.value
            repository.addNote(title.trim(), content.trim(), state.photoPath, state.latitude, state.longitude)
            _state.update { it.copy(saved = true) }
        }
    }

    fun clearTitleError() {
        _state.update { it.copy(titleError = false) }
    }

    /** Brouillon abandonné : sa photo n'appartient à aucune note, elle est supprimée. */
    fun discard() {
        _state.value.photoPath?.let(repository::discardPhoto)
    }

    // endregion

    companion object {
        private const val KEY_PHOTO = "photo"
        private const val KEY_PENDING_PHOTO = "pending_photo"
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!.app
                AddNoteViewModel(app.repository, { fetchCurrentLocation(app) }, createSavedStateHandle())
            }
        }
    }
}
