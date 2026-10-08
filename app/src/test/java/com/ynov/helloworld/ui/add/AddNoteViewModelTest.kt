package com.ynov.helloworld.ui.add

import android.location.Location
import androidx.lifecycle.SavedStateHandle
import com.ynov.helloworld.MainDispatcherRule
import com.ynov.helloworld.R
import com.ynov.helloworld.TestEnvironment
import com.ynov.helloworld.app
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests de l'[AddNoteViewModel] : validation, enregistrement et état restauré. */
@RunWith(RobolectricTestRunner::class)
class AddNoteViewModelTest {

    private val repository get() = TestEnvironment.context.app.repository

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @Before
    fun setUp() = TestEnvironment.reset()

    /** ViewModel avec une localisation simulée (par défaut : position indisponible). */
    private fun viewModel(
        handle: SavedStateHandle = SavedStateHandle(),
        location: Location? = null,
    ) = AddNoteViewModel(repository, { location }, handle)

    @Test
    fun `enregistrer sans titre signale l'erreur et n'enregistre rien`() {
        val viewModel = viewModel()

        viewModel.save("   ", "Contenu")

        assertTrue(viewModel.state.value.titleError)
        assertFalse(viewModel.state.value.saved)
        assertTrue(repository.notes.value.isEmpty())
    }

    @Test
    fun `l'erreur de titre disparaît dès que l'utilisateur corrige`() {
        val viewModel = viewModel()
        viewModel.save("", "")

        viewModel.clearTitleError()

        assertFalse(viewModel.state.value.titleError)
    }

    @Test
    fun `enregistrer ajoute la note nettoyée avec sa position`() {
        val viewModel = viewModel(SavedStateHandle(mapOf("latitude" to 43.3, "longitude" to 5.4)))

        viewModel.save("  Ma note  ", " Contenu ")

        runBlocking { withTimeout(5_000) { viewModel.state.first { it.saved } } }
        val note = repository.notes.value.single()
        assertEquals("Ma note", note.title)
        assertEquals("Contenu", note.content)
        assertEquals(43.3, note.latitude!!, 0.0)
    }

    @Test
    fun `la position restaurée n'est pas redemandée`() {
        val viewModel = viewModel(SavedStateHandle(mapOf("latitude" to 43.3, "longitude" to 5.4)))

        assertFalse(viewModel.locationNeeded)
        assertEquals(5.4, viewModel.state.value.longitude!!, 0.0)
    }

    @Test
    fun `la position trouvée est affichée et conservée pour la rotation`() {
        val handle = SavedStateHandle()
        val viewModel = viewModel(handle, Location("test").apply { latitude = 48.85; longitude = 2.35 })

        viewModel.locate()

        assertEquals(48.85, viewModel.state.value.latitude!!, 0.0)
        assertFalse(viewModel.state.value.locating)
        assertEquals(2.35, handle.get<Double>("longitude")!!, 0.0)
    }

    @Test
    fun `une position indisponible affiche une erreur`() {
        val viewModel = viewModel(location = null)

        viewModel.locate()

        assertEquals(R.string.add_location_unavailable, viewModel.state.value.locationError)
    }

    @Test
    fun `un refus de localisation affiche un message explicite`() {
        val viewModel = viewModel()

        viewModel.onLocationDenied()

        assertEquals(R.string.add_location_denied, viewModel.state.value.locationError)
        assertTrue(viewModel.locationNeeded)
    }

    // region Photo

    /** Crée un fichier photo factice dans le dossier des photos. */
    private fun photoFile() = repository.newPhotoFile().apply { writeText("photo") }

    @Test
    fun `une prise de vue annulée supprime le fichier préparé`() {
        val viewModel = viewModel()
        val file = viewModel.preparePhotoFile().apply { writeText("vide") }

        viewModel.onPictureTaken(success = false)

        TestEnvironment.eventually { !file.exists() }
        assertNull(viewModel.state.value.photoPath)
    }

    @Test
    fun `retirer la photo l'efface du brouillon et du disque`() {
        val photo = photoFile()
        val viewModel = viewModel(SavedStateHandle(mapOf("photo" to photo.path)))
        assertEquals(photo.path, viewModel.state.value.photoPath)

        viewModel.removePhoto()

        assertNull(viewModel.state.value.photoPath)
        TestEnvironment.eventually { !photo.exists() }
    }

    @Test
    fun `abandonner le brouillon supprime sa photo`() {
        val photo = photoFile()
        val viewModel = viewModel(SavedStateHandle(mapOf("photo" to photo.path)))

        viewModel.discard()

        TestEnvironment.eventually { !photo.exists() }
        assertTrue(repository.notes.value.isEmpty())
    }

    // endregion
}
