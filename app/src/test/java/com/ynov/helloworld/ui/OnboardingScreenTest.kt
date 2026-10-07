package com.ynov.helloworld.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ynov.helloworld.ui.theme.HelloWorldTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Tests du carrousel d'introduction, sur un petit écran (320 × 480 dp). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w320dp-h480dp")
class OnboardingScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private var finished = 0

    private fun setContent() {
        compose.setContent {
            HelloWorldTheme { OnboardingScreen(onFinish = { finished++ }) }
        }
    }

    @Test
    fun `la première page est affichée avec son indicateur`() {
        setContent()
        compose.onNodeWithText("Bienvenue dans votre carnet").assertIsDisplayed()
        compose.onNodeWithContentDescription("Page 1 sur 4").assertExists()
    }

    @Test
    fun `suivant passe à la page suivante`() {
        setContent()
        compose.onNodeWithText("Suivant").performClick()

        compose.onNodeWithText("Une photo pour chaque souvenir").assertIsDisplayed()
        compose.onNodeWithContentDescription("Page 2 sur 4").assertExists()
    }

    @Test
    fun `passer termine l'introduction immédiatement`() {
        setContent()
        compose.onNodeWithText("Passer").performClick()

        assertEquals(1, finished)
    }

    @Test
    fun `le titre reste lisible sur petit écran avec un texte agrandi`() {
        RuntimeEnvironment.setFontScale(1.5f)
        setContent()

        compose.onNodeWithText("Bienvenue dans votre carnet").assertIsDisplayed()
        compose.onNodeWithText("Suivant").assertIsDisplayed()
    }

    @Test
    fun `la dernière page propose de commencer`() {
        setContent()
        repeat(3) { compose.onNodeWithText("Suivant").performClick() }

        compose.onNodeWithText("Retrouvez tout sur la carte").assertIsDisplayed()
        compose.onNodeWithText("Commencer").performClick()

        assertEquals(1, finished)
    }
}
