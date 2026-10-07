package com.ynov.helloworld.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Persistance locale des notes.
 *
 * - Les notes sont sérialisées dans `filesDir/notes.json`.
 * - Les photos sont copiées dans `filesDir/photos/` pour rester disponibles
 *   même si l'image d'origine est supprimée de la galerie.
 *
 * @param context contexte applicatif utilisé pour accéder au stockage interne.
 */
class NoteRepository(private val context: Context) {

    private val notesFile = File(context.filesDir, "notes.json")

    /** Dossier de stockage des photos (partagé avec le `FileProvider`). */
    val photosDir = File(context.filesDir, "photos").apply { mkdirs() }

    // region Lecture / écriture

    /** Charge toutes les notes enregistrées, ou une liste vide au premier lancement. */
    fun load(): List<Note> {
        if (!notesFile.exists()) return emptyList()
        val array = JSONArray(notesFile.readText())
        return (0 until array.length()).map { array.getJSONObject(it).toNote() }
    }

    /** Remplace le contenu du fichier par [notes]. */
    fun save(notes: List<Note>) {
        val array = JSONArray()
        notes.forEach { array.put(it.toJson()) }
        notesFile.writeText(array.toString())
    }

    // endregion

    // region Photos

    /** Crée un nouveau fichier (vide) destiné à recevoir une photo de l'appareil photo. */
    fun newPhotoFile(): File = File(photosDir, "photo_${System.currentTimeMillis()}.jpg")

    /**
     * Copie l'image désignée par [uri] (galerie, sélecteur de médias) dans [photosDir].
     *
     * @return le fichier local créé.
     */
    fun importPhoto(uri: Uri): File {
        val target = newPhotoFile()
        context.contentResolver.openInputStream(uri)!!.use { input ->
            target.outputStream().use { input.copyTo(it) }
        }
        return target
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
}
