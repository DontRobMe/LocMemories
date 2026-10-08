package com.ynov.locmemories

import android.Manifest
import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.ynov.locmemories.ui.list.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests de bout en bout sur appareil ou émulateur : l'application complète est lancée
 * (activités, stockage réel).
 *
 * Lancement : `./gradlew connectedDebugAndroidTest`, avec un appareil connecté.
 */
@RunWith(AndroidJUnit4::class)
class NotesAppTest {

    @get:Rule
    val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

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
            onView(withText("Bienvenue dans votre carnet")).check(matches(isDisplayed()))
            onView(withText("Passer")).perform(click())

            onView(withText("Votre carnet est vide")).check(matches(isDisplayed()))
            onView(withText("Écrire ma première note")).perform(click())

            onView(withId(R.id.title)).perform(typeText("Note de test"), closeSoftKeyboard())
            onView(withId(R.id.save)).perform(click())

            onView(withText("Note de test")).check(matches(isDisplayed()))
            onView(withText("1 note")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun introductionNonReaffichee_apresUnPremierPassage() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withText("Passer")).perform(click())
        }

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withText("Votre carnet est vide")).check(matches(isDisplayed()))
            onView(withText("Passer")).check(doesNotExist())
        }
    }
}
