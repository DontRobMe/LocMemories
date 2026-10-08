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
 * État de l'écran de création.
 *
 * @property photoPath photo jointe (déjà optimisée), ou `null`.
 * @property photoProcessing `true` pendant la copie / l'optimisation d'une photo.
 * @property locating `true` pendant la recherche de la position.
 * @property locationError message à afficher si la position est indisponible.
 * @property titleError `true` si l'utilisateur a tenté d'enregistrer sans titre.
 * @property saved `true` une fois la note enregistrée : l'écran doit se fermer.
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
 * ViewModel de la création d'une note : photo, position, validation et enregistrement.
 *
 * La photo et la position sont aussi conservées dans le [SavedStateHandle] : elles survivent
 * à la destruction du processus quand l'appareil photo passe au premier plan.
 *
 * @param fetchLocation récupère la position courante (`null` si indisponible) ;
 *   remplaçable par une position simulée dans les tests.
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

    /** La position doit être demandée (pas encore connue ni en cours de recherche). */
    val locationNeeded: Boolean get() = _state.value.latitude == null && !_state.value.locating

    // region Position

    /** Recherche la position (la permission de localisation doit être accordée). */
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

    /** L'utilisateur a refusé la localisation. */
    fun onLocationDenied() {
        _state.update { it.copy(locating = false, locationError = R.string.add_location_denied) }
    }

    // endregion

    // region Photo

    /** Prépare le fichier qui recevra la prise de vue de l'appareil photo. */
    fun preparePhotoFile(): File = repository.newPhotoFile().also { handle[KEY_PENDING_PHOTO] = it.absolutePath }

    /** Résultat de l'appareil photo : la photo est optimisée en arrière-plan, ou supprimée si annulée. */
    fun onPictureTaken(success: Boolean) {
        val pending = handle.remove<String>(KEY_PENDING_PHOTO) ?: return
        if (success) {
            processPhoto { repository.optimizePhoto(pending); pending }
        } else {
            repository.discardPhoto(pending)
        }
    }

    /** Image choisie dans la galerie : copiée puis optimisée en arrière-plan. */
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

    /**
     * Enregistre la note, après la fin d'un éventuel traitement de photo.
     * Sans titre, rien n'est enregistré et [AddNoteState.titleError] passe à `true`.
     */
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

    /** Abandon du brouillon : la photo, qui n'appartient à aucune note, est supprimée. */
    fun discard() {
        _state.value.photoPath?.let(repository::discardPhoto)
    }

    // endregion

    companion object {
        private const val KEY_PHOTO = "photo"
        private const val KEY_PENDING_PHOTO = "pending_photo"
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"

        /** Fabrique : repository et localisation réelle de l'[com.ynov.helloworld.App]. */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!.app
                AddNoteViewModel(app.repository, { fetchCurrentLocation(app) }, createSavedStateHandle())
            }
        }
    }
}
