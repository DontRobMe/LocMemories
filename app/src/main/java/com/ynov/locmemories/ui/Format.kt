package com.ynov.locmemories.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * Formateurs coûteux à construire : un par thread, car SimpleDateFormat n'est pas thread-safe.
 * ThreadLocal.withInitial n'existe qu'à partir de l'API 26, d'où la sous-classe.
 */
private val fullDateFormat = dateFormat("d MMM yyyy 'à' HH:mm")
private val shortDateFormat = dateFormat("d MMM yyyy")

private fun dateFormat(pattern: String) = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue() = SimpleDateFormat(pattern, Locale.FRENCH)
}

/** « 7 oct. 2026 à 14:32 » */
fun formatDate(millis: Long): String = fullDateFormat.get().format(Date(millis))

/** « 7 oct. 2026 » */
fun formatShortDate(millis: Long): String = shortDateFormat.get().format(Date(millis))

/** « 43.29512, 5.37432 » : 5 décimales, soit une précision d'environ 1 m. */
fun formatCoordinates(latitude: Double, longitude: Double): String =
    String.format(Locale.US, "%.5f, %.5f", latitude, longitude)

/** Pour TalkBack, qui lirait sinon « 43 point 29512 virgule 5 point 37432 ». */
fun spokenCoordinates(latitude: Double, longitude: Double): String =
    String.format(Locale.FRENCH, "latitude %.4f, longitude %.4f", latitude, longitude)
