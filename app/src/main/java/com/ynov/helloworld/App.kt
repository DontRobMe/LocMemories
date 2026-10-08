package com.ynov.helloworld

import android.app.Application
import android.content.Context
import com.ynov.helloworld.data.AppPreferences
import com.ynov.helloworld.data.NoteRepository
import com.ynov.helloworld.ui.configureOsmdroid

/** Porte les dépendances partagées par les ViewModels (injection manuelle, sans framework). */
class App : Application() {

    val repository by lazy { NoteRepository(this) }

    val preferences by lazy { AppPreferences(this) }

    override fun onCreate() {
        super.onCreate()
        configureOsmdroid() // avant l'inflation de toute MapView
    }
}

val Context.app: App get() = applicationContext as App
