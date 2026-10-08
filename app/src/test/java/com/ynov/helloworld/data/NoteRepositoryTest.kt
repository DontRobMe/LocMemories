package com.ynov.helloworld.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.ynov.helloworld.MainDispatcherRule
import com.ynov.helloworld.TestEnvironment
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

/** Tests du [NoteRepository] : chargement, ajout, suppression et persistance. */
@RunWith(RobolectricTestRunner::class)
class NoteRepositoryTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    private val stored = listOf(
        Note(id = 2, title = "Récente", content = "", photoPath = null, date = 2_000, latitude = null, longitude = null),
        Note(id = 1, title = "Ancienne", content = "", photoPath = null, date = 1_000, latitude = 43.3, longitude = 5.4),
    )

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @Before
    fun setUp() {
        app.filesDir.deleteRecursively()
        app.filesDir.mkdirs()
    }

    // region Outils

    private fun loadedRepository(): NoteRepository = NoteRepository(app).also { repository ->
        runBlocking { withTimeout(5_000) { repository.loaded.first { it } } }
    }

    private fun storedNotes(): List<Note> = runBlocking { NoteStorage(app).load() }


    // endregion

    // region Chargement

    @Test
    fun `les notes enregistrées sont chargées de la plus récente à la plus ancienne`() {
        runBlocking { NoteStorage(app).save(stored.reversed()) }

        val repository = loadedRepository()

        assertEquals(listOf("Récente", "Ancienne"), repository.notes.value.map { it.title })
    }

    @Test
    fun `getNote retrouve une note par son identifiant`() {
        runBlocking { NoteStorage(app).save(stored) }
        val repository = loadedRepository()

        assertEquals("Ancienne", repository.getNote(1)?.title)
        assertNull(repository.getNote(42))
    }

    // endregion

    // region Écriture

    @Test
    fun `une note ajoutée apparaît en tête de liste et est enregistrée`() {
        runBlocking { NoteStorage(app).save(stored) }
        val repository = loadedRepository()

        repository.addNote("Nouvelle", "Contenu", null, 48.85, 2.35)

        val added = repository.notes.value.first()
        assertEquals("Nouvelle", added.title)
        assertEquals(48.85, added.latitude!!, 0.0)
        TestEnvironment.eventually { storedNotes().size == 3 }
        assertEquals("Nouvelle", storedNotes().first().title)
    }

    @Test
    fun `une note ajoutée pendant le chargement ne fait pas perdre les notes existantes`() {
        runBlocking { NoteStorage(app).save(stored) }

        val repository = NoteRepository(app)
        repository.addNote("Pendant le chargement", "", null, null, null)

        TestEnvironment.eventually { storedNotes().size == 3 }
        assertTrue(storedNotes().any { it.title == "Ancienne" })
    }

    @Test
    fun `supprimer une note la retire, l'efface du stockage et supprime sa photo`() {
        val photo = NoteStorage(app).newPhotoFile().apply { writeText("photo") }
        runBlocking { NoteStorage(app).save(listOf(stored[0].copy(photoPath = photo.path))) }
        val repository = loadedRepository()

        repository.deleteNote(stored[0].id)

        assertTrue(repository.notes.value.isEmpty())
        TestEnvironment.eventually { storedNotes().isEmpty() && !photo.exists() }
        assertFalse(photo.exists())
    }

    // endregion
}
