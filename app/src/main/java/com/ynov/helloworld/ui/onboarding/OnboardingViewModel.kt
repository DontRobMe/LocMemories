package com.ynov.helloworld.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ynov.helloworld.app
import com.ynov.helloworld.data.AppPreferences

/** ViewModel de l'introduction : mémorise qu'elle a été vue. */
class OnboardingViewModel(private val preferences: AppPreferences) : ViewModel() {

    /** Introduction terminée ou passée : elle ne s'affichera plus au lancement. */
    fun complete() {
        preferences.onboardingDone = true
    }

    companion object {
        /** Fabrique : fournit les préférences portées par l'[com.ynov.helloworld.App]. */
        val Factory = viewModelFactory {
            initializer { OnboardingViewModel(this[APPLICATION_KEY]!!.app.preferences) }
        }
    }
}
