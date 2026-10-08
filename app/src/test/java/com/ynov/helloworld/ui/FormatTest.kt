package com.ynov.helloworld.ui

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.TimeZone

/** Tests des fonctions de formatage (dates et coordonnées). */
class FormatTest {

    private lateinit var defaultTimeZone: TimeZone

    @Before
    fun setUp() {
        defaultTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Paris"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(defaultTimeZone)
    }

    @Test
    fun `formatDate affiche le jour, le mois abrégé, l'année et l'heure`() {
        // 7 octobre 2026, 14 h 32 à Paris (UTC+2).
        assertEquals("7 oct. 2026 à 14:32", formatDate(1_791_376_320_000))
    }

    @Test
    fun `formatShortDate n'affiche que la date`() {
        assertEquals("7 oct. 2026", formatShortDate(1_791_376_320_000))
    }

    @Test
    fun `les formateurs restent corrects sur plusieurs threads`() {
        val results = (1..8).map { index ->
            val millis = 1_791_376_320_000 + index * 86_400_000L
            Thread { assertEquals(formatShortDate(millis), formatShortDate(millis)) }
        }
        results.forEach { it.start() }
        results.forEach { it.join() }
    }

    @Test
    fun `formatCoordinates garde 5 décimales avec un point`() {
        assertEquals("43.29512, 5.37432", formatCoordinates(43.295123, 5.374321))
    }

    @Test
    fun `formatCoordinates gère les coordonnées négatives`() {
        assertEquals("-33.86882, 151.20930", formatCoordinates(-33.868820, 151.209296))
    }

    @Test
    fun `spokenCoordinates est formulé pour la synthèse vocale`() {
        assertEquals("latitude 43,2951, longitude 5,3743", spokenCoordinates(43.295123, 5.374321))
    }
}
