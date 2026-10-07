package com.ynov.helloworld.data

/**
 * Note géolocalisée du carnet.
 *
 * @property id identifiant unique (horodatage de création en millisecondes).
 * @property title titre de la note, obligatoire.
 * @property content texte libre de la note.
 * @property photoPath chemin absolu de la photo dans le stockage interne, ou `null`.
 * @property date date de création (epoch, en millisecondes).
 * @property latitude latitude du lieu d'écriture, ou `null` si inconnue.
 * @property longitude longitude du lieu d'écriture, ou `null` si inconnue.
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
    /** `true` si la note possède des coordonnées complètes. */
    val hasLocation: Boolean get() = latitude != null && longitude != null
}
