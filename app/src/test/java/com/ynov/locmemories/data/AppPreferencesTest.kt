package com.ynov.locmemories.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests des préférences de l'application. */
@RunWith(RobolectricTestRunner::class)
class AppPreferencesTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun `l'introduction n'est pas encore vue au premier lancement`() {
        assertFalse(AppPreferences(context).onboardingDone)
    }

    @Test
    fun `l'introduction vue est mémorisée d'un lancement à l'autre`() {
        AppPreferences(context).onboardingDone = true

        assertTrue(AppPreferences(context).onboardingDone)
    }
}
