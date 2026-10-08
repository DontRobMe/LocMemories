package com.ynov.locmemories.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests du modèle [Note]. */
class NoteTest {

    private fun note(latitude: Double?, longitude: Double?) = Note(
        id = 1,
        title = "Titre",
        content = "Contenu",
        photoPath = null,
        date = 0,
        latitude = latitude,
        longitude = longitude,
    )

    @Test
    fun `une note avec latitude et longitude est localisée`() {
        assertTrue(note(43.3, 5.4).hasLocation)
    }

    @Test
    fun `une note sans coordonnées n'est pas localisée`() {
        assertFalse(note(null, null).hasLocation)
    }

    @Test
    fun `une note avec une seule coordonnée n'est pas localisée`() {
        assertFalse(note(43.3, null).hasLocation)
        assertFalse(note(null, 5.4).hasLocation)
    }
}
