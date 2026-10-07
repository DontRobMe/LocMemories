package com.ynov.helloworld.data

import android.content.Context
import androidx.core.content.edit

/**
 * Préférences simples de l'application, stockées dans des `SharedPreferences`.
 *
 * @param context contexte applicatif.
 */
class AppPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

    // region Introduction

    /** `true` une fois l'introduction terminée ou passée : elle n'est plus affichée au lancement. */
    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit { putBoolean(KEY_ONBOARDING_DONE, value) }

    // endregion

    private companion object {
        const val KEY_ONBOARDING_DONE = "onboarding_done"
    }
}
