package com.ynov.locmemories.ui.add

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasFocus
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.ynov.locmemories.R
import com.ynov.locmemories.TestEnvironment
import com.ynov.locmemories.TestEnvironment.awaitLoaded
import com.ynov.locmemories.app
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests de l'écran de création : validation du titre et enregistrement. */
@RunWith(RobolectricTestRunner::class)
class AddNoteActivityTest {

    @Before
    fun setUp() = TestEnvironment.reset()

    @Test
    fun `enregistrer sans titre affiche une erreur explicite et n'enregistre rien`() {
        ActivityScenario.launch(AddNoteActivity::class.java).use {
            awaitLoaded()

            onView(withId(R.id.save)).perform(click())

            onView(withText(R.string.add_field_title_error)).check(matches(isDisplayed()))
            onView(withId(R.id.title)).check(matches(hasFocus()))
            assertTrue(TestEnvironment.context.app.repository.notes.value.isEmpty())
        }
    }

    @Test
    fun `enregistrer avec un titre ajoute la note et ferme l'écran`() {
        ActivityScenario.launch(AddNoteActivity::class.java).use { scenario ->
            awaitLoaded()

            onView(withId(R.id.title)).perform(typeText("Ma note"), closeSoftKeyboard())
            onView(withId(R.id.save)).perform(click())
            TestEnvironment.idle()

            assertEquals("Ma note", TestEnvironment.context.app.repository.notes.value.single().title)
            scenario.onActivity { assertTrue(it.isFinishing) }
        }
    }
}
