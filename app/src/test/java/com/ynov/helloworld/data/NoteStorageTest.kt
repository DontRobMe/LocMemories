package com.ynov.helloworld.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.math.max

/** Tests des fichiers : notes en JSON et traitement des photos. */
@RunWith(RobolectricTestRunner::class)
class NoteStorageTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var storage: NoteStorage

    private val notes = listOf(
        Note(
            id = 2,
            title = "Avec tout",
            content = "Photo et position",
            photoPath = "/data/photos/photo.jpg",
            date = 2_000,
            latitude = 43.29512,
            longitude = 5.37432,
        ),
        Note(
            id = 1,
            title = "Sans rien",
            content = "",
            photoPath = null,
            date = 1_000,
            latitude = null,
            longitude = null,
        ),
    )

    @Before
    fun setUp() {
        context.filesDir.deleteRecursively()
        context.filesDir.mkdirs()
        storage = NoteStorage(context)
    }

    // region Lecture / écriture

    @Test
    fun `load renvoie une liste vide au premier lancement`() = runTest {
        assertEquals(emptyList<Note>(), storage.load())
    }

    @Test
    fun `les notes enregistrées sont relues à l'identique, valeurs nulles comprises`() = runTest {
        storage.save(notes)

        assertEquals(notes, NoteStorage(context).load())
    }

    @Test
    fun `une sauvegarde remplace entièrement la précédente`() = runTest {
        storage.save(notes)
        storage.save(notes.take(1))

        assertEquals(notes.take(1), storage.load())
    }

    @Test
    fun `la sauvegarde ne laisse aucun fichier temporaire`() = runTest {
        storage.save(notes)

        val leftovers = context.filesDir.listFiles().orEmpty().filter { it.name.endsWith(".tmp") }
        assertTrue(leftovers.isEmpty())
    }

    // endregion

    // region Photos

    @Test
    fun `les nouvelles photos sont créées dans le dossier dédié`() {
        val file = storage.newPhotoFile()

        assertEquals(storage.photosDir, file.parentFile)
        assertTrue(file.name.endsWith(".jpg"))
    }

    @Test
    fun `deletePhoto supprime le fichier`() = runTest {
        val file = storage.newPhotoFile().apply { writeText("photo") }

        storage.deletePhoto(file.path)

        assertFalse(file.exists())
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `une photo importée est réduite à 2048 pixels au plus`() = runTest {
        val source = jpeg(width = 4000, height = 3000)

        val imported = storage.importPhoto(Uri.fromFile(source))

        val size = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            .also { BitmapFactory.decodeFile(imported.path, it) }
        assertEquals(2048, max(size.outWidth, size.outHeight))
        assertEquals(4f / 3f, size.outWidth.toFloat() / size.outHeight, 0.01f)
        assertEquals(storage.photosDir, imported.parentFile)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `une petite photo n'est pas recompressée`() = runTest {
        val source = jpeg(width = 800, height = 600)

        val imported = storage.importPhoto(Uri.fromFile(source))

        assertEquals(source.length(), imported.length())
    }

    /** Crée une image JPEG unie de la taille demandée dans le cache. */
    private fun jpeg(width: Int, height: Int): File {
        val file = File(context.cacheDir, "source_${width}x$height.jpg")
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(0xFF006A60.toInt())
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        return file
    }

    // endregion
}
