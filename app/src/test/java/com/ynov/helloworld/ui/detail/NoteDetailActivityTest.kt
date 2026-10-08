package com.ynov.helloworld.ui.detail

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.ynov.helloworld.R
import com.ynov.helloworld.TestEnvironment
import com.ynov.helloworld.TestEnvironment.awaitLoaded
import com.ynov.helloworld.app
import com.ynov.helloworld.testNotes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests de l'écran de détail (note sans position) : affichage et suppression confirmée. */
@RunWith(RobolectricTestRunner::class)
class NoteDetailActivityTest {

    private val note = testNotes.first { !it.hasLocation }

    @Before
    fun setUp() = TestEnvironment.reset(testNotes)

    private fun launch() = ActivityScenario.launch<NoteDetailActivity>(
        Intent(TestEnvironment.context, NoteDetailActivity::class.java).putExtra("note_id", note.id)
    )

    @Test
    fun `le titre, le contenu et l'absence de position sont affichés`() {
        launch().use {
            awaitLoaded()

            onView(withText(note.title)).check(matches(isDisplayed()))
            onView(withText(note.content)).check(matches(isDisplayed()))
            onView(withText(R.string.detail_no_location)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun `la suppression demande une confirmation puis supprime la note`() {
        launch().use { scenario ->
            awaitLoaded()

            onView(withContentDescription(R.string.detail_delete)).perform(click())
            TestEnvironment.idle()
            onView(withText(R.string.detail_delete_title)).inRoot(isDialog()).check(matches(isDisplayed()))
            assertEquals(3, TestEnvironment.context.app.repository.notes.value.size)

            onView(withText(R.string.detail_delete_confirm)).inRoot(isDialog()).perform(click())
            TestEnvironment.idle()

            assertTrue(TestEnvironment.context.app.repository.notes.value.none { it.id == note.id })
            scenario.onActivity { assertTrue(it.isFinishing) }
        }
    }

    @Test
    fun `annuler la suppression ne supprime rien`() {
        launch().use {
            awaitLoaded()

            onView(withContentDescription(R.string.detail_delete)).perform(click())
            TestEnvironment.idle()
            onView(withText(R.string.action_cancel)).inRoot(isDialog()).perform(click())
            TestEnvironment.idle()

            onView(withText(R.string.detail_delete_title)).check(doesNotExist())
            assertEquals(3, TestEnvironment.context.app.repository.notes.value.size)
        }
    }
}
