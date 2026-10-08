package com.ynov.locmemories.ui.onboarding

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.ynov.locmemories.R
import com.ynov.locmemories.TestEnvironment
import com.ynov.locmemories.app
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests du carrousel d'introduction, sur un petit écran (320 × 480 dp). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w320dp-h480dp")
class OnboardingActivityTest {

    @Before
    fun setUp() = TestEnvironment.reset(onboardingDone = false)

    @Test
    fun `la première page est affichée avec son indicateur`() {
        ActivityScenario.launch(OnboardingActivity::class.java).use {
            TestEnvironment.idle()
            onView(withText(R.string.onboarding_title_1)).check(matches(isDisplayed()))
            onView(withContentDescription("Page 1 sur 4")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun `suivant passe à la page suivante`() {
        ActivityScenario.launch(OnboardingActivity::class.java).use {
            TestEnvironment.idle()
            onView(withId(R.id.next)).perform(click())
            TestEnvironment.idle()

            onView(withText(R.string.onboarding_title_2)).check(matches(isDisplayed()))
            onView(withContentDescription("Page 2 sur 4")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun `passer termine l'introduction et la mémorise`() {
        ActivityScenario.launch(OnboardingActivity::class.java).use { scenario ->
            TestEnvironment.idle()
            onView(withText(R.string.onboarding_skip)).perform(click())

            assertTrue(TestEnvironment.context.app.preferences.onboardingDone)
            scenario.onActivity { assertTrue(it.isFinishing) }
        }
    }

    @Test
    fun `la dernière page propose de commencer`() {
        ActivityScenario.launch(OnboardingActivity::class.java).use { scenario ->
            TestEnvironment.idle()
            repeat(3) {
                onView(withId(R.id.next)).perform(click())
                TestEnvironment.idle()
            }

            onView(withText(R.string.onboarding_title_4)).check(matches(isDisplayed()))
            onView(withText(R.string.onboarding_start)).perform(click())

            assertTrue(TestEnvironment.context.app.preferences.onboardingDone)
            scenario.onActivity { assertTrue(it.isFinishing) }
        }
    }
}
