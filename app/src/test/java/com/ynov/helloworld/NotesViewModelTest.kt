package com.ynov.helloworld

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.ynov.helloworld.data.Note
import com.ynov.helloworld.data.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests du [NotesViewModel] : chargement, ajout, suppression et persistance. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class NotesViewModelTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    private val stored = listOf(
        Note(id = 2, title = "Récente", content = "", photoPath = null, date = 2_000, latitude = null, longitude = null),
        Note(id = 1, title = "Ancienne", content = "", photoPath = null, date = 1_000, latitude = 43.3, longitude = 5.4),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        app.filesDir.deleteRecursively()
        app.filesDir.mkdirs()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region Outils

    private fun loadedViewModel(): NotesViewModel = NotesViewModel(app).also { viewModel ->
        runBlocking { withTimeout(5_000) { viewModel.loaded.first { it } } }
    }

    private fun storedNotes(): List<Note> = runBlocking { NoteRepository(app).load() }

    /**
     * Attend que [condition] devienne vraie : les sauvegardes s'exécutent en arrière-plan.
     *
     * Une lecture peut croiser une écriture en cours (sous Windows, le renommage du fichier
     * n'est pas atomique) : une erreur d'entrée / sortie compte alors comme « pas encore ».
     */
    private fun eventually(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!(runCatching(condition).getOrNull() ?: false)) {
            check(System.currentTimeMillis() < deadline) { "Condition non remplie après 5 s" }
            Thread.sleep(20)
        }
    }

    // endregion

    // region Chargement

    @Test
    fun `les notes enregistrées sont chargées de la plus récente à la plus ancienne`() {
        runBlocking { NoteRepository(app).save(stored.reversed()) }

        val viewModel = loadedViewModel()

        assertEquals(listOf("Récente", "Ancienne"), viewModel.notes.value.map { it.title })
    }

    @Test
    fun `getNote retrouve une note par son identifiant`() {
        runBlocking { NoteRepository(app).save(stored) }
        val viewModel = loadedViewModel()

        assertEquals("Ancienne", viewModel.getNote(1)?.title)
        assertNull(viewModel.getNote(42))
    }

    // endregion

    // region Écriture

    @Test
    fun `une note ajoutée apparaît en tête de liste et est enregistrée`() {
        runBlocking { NoteRepository(app).save(stored) }
        val viewModel = loadedViewModel()

        viewModel.addNote("Nouvelle", "Contenu", null, 48.85, 2.35)

        val added = viewModel.notes.value.first()
        assertEquals("Nouvelle", added.title)
        assertEquals(48.85, added.latitude!!, 0.0)
        eventually { storedNotes().size == 3 }
        assertEquals("Nouvelle", storedNotes().first().title)
    }

    @Test
    fun `une note ajoutée pendant le chargement ne fait pas perdre les notes existantes`() {
        runBlocking { NoteRepository(app).save(stored) }

        val viewModel = NotesViewModel(app)
        viewModel.addNote("Pendant le chargement", "", null, null, null)

        eventually { storedNotes().size == 3 }
        assertTrue(storedNotes().any { it.title == "Ancienne" })
    }

    @Test
    fun `supprimer une note la retire, l'efface du stockage et supprime sa photo`() {
        val photo = NoteRepository(app).newPhotoFile().apply { writeText("photo") }
        runBlocking { NoteRepository(app).save(listOf(stored[0].copy(photoPath = photo.path))) }
        val viewModel = loadedViewModel()

        viewModel.deleteNote(stored[0].id)

        assertTrue(viewModel.notes.value.isEmpty())
        eventually { storedNotes().isEmpty() && !photo.exists() }
        assertFalse(photo.exists())
    }

    // endregion
}
