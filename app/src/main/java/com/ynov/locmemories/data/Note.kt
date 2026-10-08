package com.ynov.locmemories.data

/**
 * Note géolocalisée du carnet.
 *
 * @property id horodatage de création en millisecondes, unique par note.
 * @property photoPath chemin absolu dans le stockage interne de l'application.
 * @property date date de création (epoch, en millisecondes).
 */
data class Note(
    val id: Long,
    val title: String,
    val content: String,
    val photoPath: String?,
    val date: Long,
    val latitude: Double?,
    val longitude: Double?,
) {
    val hasLocation: Boolean get() = latitude != null && longitude != null
}
