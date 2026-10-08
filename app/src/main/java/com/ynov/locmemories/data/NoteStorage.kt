package com.ynov.locmemories.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.max

/**
 * Fichiers de l'application, utilisés par [NoteRepository] :
 * les notes dans `filesDir/notes.json`, les photos dans `filesDir/photos/`.
 *
 * Les photos de la galerie y sont copiées pour rester disponibles même si l'original
 * est supprimé. Toutes les entrées / sorties s'exécutent sur [Dispatchers.IO].
 */
class NoteStorage(private val context: Context) {

    private val notesFile = File(context.filesDir, "notes.json")

    /** Partagé avec l'appareil photo via le `FileProvider` (`res/xml/file_paths.xml`). */
    val photosDir = File(context.filesDir, "photos").apply { mkdirs() }

    // region Lecture / écriture

    suspend fun load(): List<Note> = withContext(Dispatchers.IO) {
        if (!notesFile.exists()) return@withContext emptyList()
        val array = JSONArray(notesFile.readText())
        (0 until array.length()).map { array.getJSONObject(it).toNote() }
    }

    /**
     * Écrit d'abord dans un fichier temporaire, puis le renomme : un arrêt brutal
     * pendant la sauvegarde ne peut pas corrompre les notes existantes.
     */
    suspend fun save(notes: List<Note>) = withContext(Dispatchers.IO) {
        val array = JSONArray()
        notes.forEach { array.put(it.toJson()) }
        val temp = File(notesFile.parentFile, "${notesFile.name}.tmp")
        temp.writeText(array.toString())
        if (!temp.renameTo(notesFile)) {
            notesFile.writeText(array.toString())
            temp.delete()
        }
    }

    // endregion

    // region Photos

    fun newPhotoFile(): File = File(photosDir, "photo_${System.currentTimeMillis()}.jpg")

    suspend fun importPhoto(uri: Uri): File = withContext(Dispatchers.IO) {
        val target = newPhotoFile()
        context.contentResolver.openInputStream(uri)!!.use { input ->
            target.outputStream().use { input.copyTo(it) }
        }
        optimizeInPlace(target)
        target
    }

    suspend fun optimizePhoto(file: File) = withContext(Dispatchers.IO) { optimizeInPlace(file) }

    suspend fun deletePhoto(path: String) = withContext(Dispatchers.IO) { File(path).delete() }

    /**
     * Réduit la photo à [MAX_PHOTO_SIZE] pixels sur son plus grand côté, applique
     * l'orientation EXIF et la réenregistre en JPEG.
     *
     * Une photo de 12 Mpx passe ainsi d'environ 4 Mo à 400 Ko : décodage, mémoire
     * et défilement de la liste deviennent beaucoup plus légers.
     * Une image déjà petite et correctement orientée est laissée intacte.
     */
    private fun optimizeInPlace(file: File) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        val longest = max(bounds.outWidth, bounds.outHeight)
        if (longest <= 0) return

        val rotation = ExifInterface(file.path).rotationDegrees
        if (longest <= MAX_PHOTO_SIZE && rotation == 0) return

        var sampleSize = 1
        while (longest / (sampleSize * 2) >= MAX_PHOTO_SIZE) sampleSize *= 2
        val decoded = BitmapFactory.decodeFile(
            file.path,
            BitmapFactory.Options().apply { inSampleSize = sampleSize },
        ) ?: return

        val scale = (MAX_PHOTO_SIZE.toFloat() / max(decoded.width, decoded.height)).coerceAtMost(1f)
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation.toFloat())
        }
        val result = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        file.outputStream().use { result.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        if (result !== decoded) result.recycle()
        decoded.recycle()
    }

    // endregion

    // region Sérialisation JSON

    private fun Note.toJson() = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("content", content)
        put("photoPath", photoPath ?: JSONObject.NULL)
        put("date", date)
        put("latitude", latitude ?: JSONObject.NULL)
        put("longitude", longitude ?: JSONObject.NULL)
    }

    private fun JSONObject.toNote() = Note(
        id = getLong("id"),
        title = getString("title"),
        content = getString("content"),
        photoPath = if (isNull("photoPath")) null else getString("photoPath"),
        date = getLong("date"),
        latitude = if (isNull("latitude")) null else getDouble("latitude"),
        longitude = if (isNull("longitude")) null else getDouble("longitude"),
    )

    // endregion

    private companion object {
        /** Plus grand côté d'une photo enregistrée, en pixels. */
        const val MAX_PHOTO_SIZE = 2048

        /** Visuellement sans perte, environ 10 fois plus léger qu'un original. */
        const val JPEG_QUALITY = 85
    }
}
