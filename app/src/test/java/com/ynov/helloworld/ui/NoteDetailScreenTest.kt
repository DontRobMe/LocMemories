package com.ynov.helloworld.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ynov.helloworld.ui.theme.HelloWorldTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests de l'écran de détail.
 *
 * La note utilisée n'a pas de position : l'aperçu cartographique (moteur natif MapLibre)
 * n'est pas disponible sous Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
class NoteDetailScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val note = previewNotes.first { !it.hasLocation }
    private var deletes = 0
    private var backs = 0

    @Before
    fun setUp() {
        compose.setContent {
            HelloWorldTheme {
                NoteDetailScreen(note = note, onBack = { backs++ }, onDelete = { deletes++ })
            }
        }
    }

    @Test
    fun `le titre, le contenu et l'absence de position sont affichés`() {
        compose.onNodeWithText(note.title).assertIsDisplayed()
        compose.onNodeWithText(note.content).assertIsDisplayed()
        compose.onNodeWithText("Aucune position n'a été enregistrée pour cette note.").assertExists()
    }

    @Test
    fun `la suppression demande une confirmation`() {
        compose.onNodeWithContentDescription("Supprimer la note").performClick()

        compose.onNodeWithText("Supprimer cette note ?").assertIsDisplayed()
        assertEquals(0, deletes)

        compose.onNodeWithText("Supprimer").performClick()
        assertEquals(1, deletes)
    }

    @Test
    fun `annuler la suppression ferme la boîte de dialogue sans supprimer`() {
        compose.onNodeWithContentDescription("Supprimer la note").performClick()

        compose.onNodeWithText("Annuler").performClick()

        compose.onNodeWithText("Supprimer cette note ?").assertDoesNotExist()
        assertEquals(0, deletes)
    }

    @Test
    fun `le retour est accessible par sa description`() {
        compose.onNodeWithContentDescription("Retour à la liste").performClick()

        assertEquals(1, backs)
    }
}
