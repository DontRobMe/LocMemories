package com.ynov.helloworld.ui.list

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.ynov.helloworld.R
import com.ynov.helloworld.TestEnvironment
import com.ynov.helloworld.TestEnvironment.awaitLoaded
import com.ynov.helloworld.testNotes
import com.ynov.helloworld.ui.detail.NoteDetailActivity
import com.ynov.helloworld.ui.onboarding.OnboardingActivity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** Tests de l'écran de liste : état vide, recherche, navigation et premier lancement. */
@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

    @Test
    fun `un carnet vide invite à écrire la première note`() {
        TestEnvironment.reset()
        ActivityScenario.launch(MainActivity::class.java).use {
            awaitLoaded()

            onView(withText(R.string.list_empty_title)).check(matches(isDisplayed()))
            onView(withText(R.string.list_empty_action)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun `la liste affiche le nombre de notes et la plus récente`() {
        TestEnvironment.reset(testNotes)
        ActivityScenario.launch(MainActivity::class.java).use {
            awaitLoaded()

            onView(withText("3 notes")).check(matches(isDisplayed()))
            onView(withText("Balade au port")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun `la recherche filtre sur le titre et le contenu, sans tenir compte de la casse`() {
        TestEnvironment.reset(testNotes)
        ActivityScenario.launch(MainActivity::class.java).use {
            awaitLoaded()

            onView(withId(R.id.search_input)).perform(typeText("WIFI"))
            TestEnvironment.idle()

            onView(withText("1 résultat")).check(matches(isDisplayed()))
            onView(withText("Café sympa")).check(matches(isDisplayed()))
            onView(withText("Balade au port")).check(doesNotExist())
        }
    }

    @Test
    fun `une recherche sans résultat l'indique clairement`() {
        TestEnvironment.reset(testNotes)
        ActivityScenario.launch(MainActivity::class.java).use {
            awaitLoaded()

            onView(withId(R.id.search_input)).perform(typeText("zzz"))
            TestEnvironment.idle()

            onView(withText("Aucune note ne correspond à « zzz »")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun `toucher une note ouvre son détail`() {
        TestEnvironment.reset(testNotes)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            awaitLoaded()

            onView(withText("Balade au port")).perform(click())

            scenario.onActivity { activity ->
                val next = shadowOf(activity).nextStartedActivity
                assertEquals(NoteDetailActivity::class.java.name, next.component?.className)
                assertEquals(testNotes.first().id, next.getLongExtra("note_id", -1))
            }
        }
    }

    @Test
    fun `le premier lancement ouvre l'introduction`() {
        TestEnvironment.reset(onboardingDone = false)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val next = shadowOf(activity).nextStartedActivity
                assertEquals(OnboardingActivity::class.java.name, next.component?.className)
            }
        }
    }
}
