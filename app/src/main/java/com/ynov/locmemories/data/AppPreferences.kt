package com.ynov.locmemories.data

import android.content.Context
import androidx.core.content.edit

/** Préférences de l'application, stockées dans des `SharedPreferences`. */
class AppPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

    /** `true` une fois l'introduction terminée ou passée : elle n'est plus proposée au lancement. */
    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit { putBoolean(KEY_ONBOARDING_DONE, value) }

    private companion object {
        const val KEY_ONBOARDING_DONE = "onboarding_done"
    }
}
