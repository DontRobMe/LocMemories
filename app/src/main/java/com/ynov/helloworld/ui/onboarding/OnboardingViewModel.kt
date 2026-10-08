package com.ynov.helloworld.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ynov.helloworld.app
import com.ynov.helloworld.data.AppPreferences

class OnboardingViewModel(private val preferences: AppPreferences) : ViewModel() {

    fun complete() {
        preferences.onboardingDone = true
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { OnboardingViewModel(this[APPLICATION_KEY]!!.app.preferences) }
        }
    }
}
