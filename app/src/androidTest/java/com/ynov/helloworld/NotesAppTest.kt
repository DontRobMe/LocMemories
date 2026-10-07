package com.ynov.helloworld

import android.Manifest
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests de bout en bout sur appareil ou émulateur : l'application complète est lancée
 * (navigation, ViewModel, stockage réel).
 *
 * Lancement : `./gradlew connectedDebugAndroidTest`, avec un appareil connecté.
 */
@RunWith(AndroidJUnit4::class)
class NotesAppTest {

    @get:Rule(order = 0)
    val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    /** Repart d'une installation vierge : aucune note, introduction jamais vue. */
    @Before
    fun clearAppData() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.filesDir.deleteRecursively()
        context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun premierLancement_passerLIntroduction_puisCreerUneNote() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText("Bienvenue dans votre carnet").assertIsDisplayed()
            compose.onNodeWithText("Passer").performClick()

            compose.onNodeWithText("Votre carnet est vide").assertIsDisplayed()
            compose.onNodeWithText("Écrire ma première note").performClick()

            compose.onNodeWithText("Titre").performTextInput("Note de test")
            compose.onNodeWithText("Enregistrer la note").performClick()

            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText("1 note").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Note de test").assertIsDisplayed()
        }
    }

    @Test
    fun introductionNonReaffichee_apresUnPremierPassage() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText("Passer").performClick()
        }

        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText("Votre carnet est vide").assertIsDisplayed()
            compose.onNodeWithText("Passer").assertDoesNotExist()
        }
    }

    @Test
    fun noteCreee_estConserveeApresRedemarrage() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText("Passer").performClick()
            compose.onNodeWithText("Écrire ma première note").performClick()
            compose.onNodeWithText("Titre").performTextInput("Note persistante")
            compose.onNodeWithText("Enregistrer la note").performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText("Note persistante").fetchSemanticsNodes().isNotEmpty()
            }
        }

        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText("Note persistante").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Note persistante").assertIsDisplayed()
        }
    }
}
