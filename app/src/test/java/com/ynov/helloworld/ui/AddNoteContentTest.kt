package com.ynov.helloworld.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.ynov.helloworld.ui.theme.HelloWorldTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests de l'écran de création : validation du titre et états de la photo / position. */
@RunWith(RobolectricTestRunner::class)
class AddNoteContentTest {

    @get:Rule
    val compose = createComposeRule()

    private var saves = 0
    private var backs = 0

    private fun setContent(
        latitude: Double? = null,
        longitude: Double? = null,
        locating: Boolean = false,
        locationError: String? = null,
        photoProcessing: Boolean = false,
    ) {
        compose.setContent {
            var title by remember { mutableStateOf("") }
            HelloWorldTheme {
                AddNoteContent(
                    title = title,
                    onTitleChange = { title = it },
                    content = "",
                    onContentChange = {},
                    photoPath = null,
                    photoProcessing = photoProcessing,
                    latitude = latitude,
                    longitude = longitude,
                    locating = locating,
                    locationError = locationError,
                    onTakePicture = {},
                    onPickImage = {},
                    onRemovePhoto = {},
                    onRefreshLocation = {},
                    onSave = { saves++ },
                    onBack = { backs++ },
                )
            }
        }
    }

    // region Validation

    @Test
    fun `enregistrer sans titre affiche une erreur explicite et n'enregistre pas`() {
        setContent()

        compose.onNodeWithText("Enregistrer la note").performClick()

        compose.onNodeWithText("Donnez un titre à votre note").assertIsDisplayed()
        compose.onNodeWithText("Titre").assertIsFocused()
        assertEquals(0, saves)
    }

    @Test
    fun `enregistrer avec un titre déclenche la sauvegarde`() {
        setContent()

        compose.onNodeWithText("Titre").performTextInput("Ma note")
        compose.onNodeWithText("Enregistrer la note").performClick()

        assertEquals(1, saves)
    }

    @Test
    fun `fermer l'écran est accessible par sa description`() {
        setContent()

        compose.onNodeWithContentDescription("Annuler et fermer").performClick()

        assertEquals(1, backs)
    }

    // endregion

    // region Position et photo

    @Test
    fun `la localisation en cours est annoncée`() {
        setContent(locating = true)

        compose.onNodeWithText("Localisation en cours…").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `les coordonnées trouvées sont affichées`() {
        setContent(latitude = 43.29512, longitude = 5.37432)

        compose.onNodeWithText("43.29512, 5.37432").assertExists()
    }

    @Test
    fun `une erreur de localisation est affichée`() {
        setContent(locationError = "Autorisez la localisation pour situer la note.")

        compose.onNodeWithText("Autorisez la localisation pour situer la note.").assertExists()
    }

    @Test
    fun `la préparation de la photo est signalée`() {
        setContent(photoProcessing = true)

        compose.onNodeWithContentDescription("Préparation de la photo").assertExists()
    }

    // endregion
}
