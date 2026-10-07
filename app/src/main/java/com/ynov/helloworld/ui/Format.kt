package com.ynov.helloworld.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// region Dates

/*
 * Les formateurs sont coûteux à construire (chargement des données de locale) :
 * ils sont créés une seule fois par thread, SimpleDateFormat n'étant pas thread-safe.
 */
private val fullDateFormat = dateFormat("d MMM yyyy 'à' HH:mm")
private val shortDateFormat = dateFormat("d MMM yyyy")

/** Formateur français mis en cache par thread (compatible API 24, contrairement à `withInitial`). */
private fun dateFormat(pattern: String) = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue() = SimpleDateFormat(pattern, Locale.FRENCH)
}

/** Date et heure complètes, ex. « 7 oct. 2026 à 14:32 ». */
fun formatDate(millis: Long): String = fullDateFormat.get()!!.format(Date(millis))

/** Date seule, ex. « 7 oct. 2026 ». */
fun formatShortDate(millis: Long): String = shortDateFormat.get()!!.format(Date(millis))

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
