package com.ynov.helloworld.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// region Dates

/** Date et heure complètes, ex. « 7 oct. 2026 à 14:32 ». */
fun formatDate(millis: Long): String =
    SimpleDateFormat("d MMM yyyy 'à' HH:mm", Locale.FRENCH).format(Date(millis))

/** Date seule, ex. « 7 oct. 2026 ». */
fun formatShortDate(millis: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.FRENCH).format(Date(millis))

// endregion

// region Coordonnées

/** Coordonnées compactes à 5 décimales (précision ≈ 1 m), ex. « 43.29512, 5.37432 ». */
fun formatCoordinates(latitude: Double, longitude: Double): String =
    String.format(Locale.US, "%.5f, %.5f", latitude, longitude)

/**
 * Coordonnées formulées pour la synthèse vocale.
 *
 * Évite que TalkBack lise « 43 point 29512 virgule 5 point 37432 ».
 */
fun spokenCoordinates(latitude: Double, longitude: Double): String =
    String.format(Locale.FRENCH, "latitude %.4f, longitude %.4f", latitude, longitude)

// endregion
