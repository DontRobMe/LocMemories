package com.ynov.helloworld

import android.app.Application
import android.content.Context
import com.ynov.helloworld.data.AppPreferences
import com.ynov.helloworld.data.NoteRepository
import com.ynov.helloworld.ui.configureOsmdroid

/**
 * Application : porte les dépendances partagées par les ViewModels
 * (injection manuelle, sans framework).
 */
class App : Application() {

    /** Notes de l'utilisateur, communes à tous les écrans. */
    val repository by lazy { NoteRepository(this) }

    /** Préférences (introduction déjà vue…). */
    val preferences by lazy { AppPreferences(this) }

    override fun onCreate() {
        super.onCreate()
        configureOsmdroid()
    }
}

/** Accès à l'[App] depuis n'importe quel contexte. */
val Context.app: App get() = applicationContext as App
