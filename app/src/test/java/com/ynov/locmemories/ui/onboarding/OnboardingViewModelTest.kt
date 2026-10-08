package com.ynov.locmemories.ui.onboarding

import com.ynov.locmemories.TestEnvironment
import com.ynov.locmemories.app
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests de l'[OnboardingViewModel]. */
@RunWith(RobolectricTestRunner::class)
class OnboardingViewModelTest {

    @Test
    fun `terminer l'introduction la marque comme vue`() {
        TestEnvironment.reset(onboardingDone = false)
        val preferences = TestEnvironment.context.app.preferences
        assertFalse(preferences.onboardingDone)

        OnboardingViewModel(preferences).complete()

        assertTrue(preferences.onboardingDone)
    }
}
