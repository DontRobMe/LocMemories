package com.ynov.helloworld.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.ynov.helloworld.data.Note
import com.ynov.helloworld.ui.theme.HelloWorldTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests de l'écran de liste : états vide / chargement, recherche et navigation. */
@RunWith(RobolectricTestRunner::class)
class NoteListScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private var clickedNote: Note? = null
    private var addClicks = 0
    private var mapClicks = 0
    private var helpClicks = 0

    private fun setContent(notes: List<Note>, loading: Boolean = false) {
        compose.setContent {
            HelloWorldTheme {
                NoteListScreen(
                    notes = notes,
                    onAddClick = { addClicks++ },
                    onNoteClick = { clickedNote = it },
                    onMapClick = { mapClicks++ },
                    onHelpClick = { helpClicks++ },
                    loading = loading,
                )
            }
        }
    }

    // region États

    @Test
    fun `un carnet vide invite à écrire la première note`() {
        setContent(emptyList())

        compose.onNodeWithText("Votre carnet est vide").assertIsDisplayed()
        compose.onNodeWithText("Écrire ma première note").performClick()
        assertEquals(1, addClicks)
    }

    @Test
    fun `rien n'est affiché pendant le chargement`() {
        setContent(emptyList(), loading = true)

        compose.onNodeWithText("Votre carnet est vide").assertDoesNotExist()
        compose.onNodeWithText("Nouvelle note").assertDoesNotExist()
    }

    @Test
    fun `la liste affiche le nombre de notes et la plus récente`() {
        setContent(previewNotes)

        compose.onNodeWithText("3 notes").assertIsDisplayed()
        compose.onNodeWithText("Balade au port").assertIsDisplayed()
    }

    // endregion

    // region Recherche

    @Test
    fun `la recherche filtre sur le titre et le contenu`() {
        setContent(previewNotes)

        compose.onNode(hasSetTextAction()).performTextInput("wifi")

        compose.onNodeWithText("1 résultat").assertIsDisplayed()
        compose.onNodeWithText("Café sympa").assertIsDisplayed()
        compose.onNodeWithText("Balade au port").assertDoesNotExist()
    }

    @Test
    fun `la recherche ignore la casse`() {
        setContent(previewNotes)

        compose.onNode(hasSetTextAction()).performTextInput("BALADE")

        compose.onNodeWithText("1 résultat").assertIsDisplayed()
    }

    @Test
    fun `une recherche sans résultat l'indique clairement`() {
        setContent(previewNotes)

        compose.onNode(hasSetTextAction()).performTextInput("zzz")

        compose.onNodeWithText("Aucune note ne correspond à « zzz »").assertIsDisplayed()
    }

    @Test
    fun `effacer la recherche réaffiche toutes les notes`() {
        setContent(previewNotes)
        compose.onNode(hasSetTextAction()).performTextInput("zzz")

        compose.onNodeWithContentDescription("Effacer la recherche").performClick()

        compose.onNodeWithText("3 notes").assertIsDisplayed()
    }

    // endregion

    // region Navigation

    @Test
    fun `toucher une note l'ouvre`() {
        setContent(previewNotes)

        compose.onNodeWithText("Balade au port").performClick()

        assertEquals(previewNotes.first().id, clickedNote?.id)
    }

    @Test
    fun `les actions de la barre sont accessibles par leur description`() {
        setContent(previewNotes)

        compose.onNodeWithContentDescription("Voir les notes sur la carte").performClick()
        compose.onNodeWithContentDescription("Revoir l'introduction").performClick()

        assertEquals(1, mapClicks)
        assertEquals(1, helpClicks)
    }

    // endregion
}
