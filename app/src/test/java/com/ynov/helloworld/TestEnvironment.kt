package com.ynov.helloworld

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.ynov.helloworld.data.Note
import com.ynov.helloworld.data.NoteStorage
import kotlinx.coroutines.runBlocking
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/** Outils communs aux tests d'écrans Robolectric. */
object TestEnvironment {

    val context: Context get() = ApplicationProvider.getApplicationContext()

    /** Repart d'une installation vierge, avec éventuellement des notes déjà enregistrées. */
    fun reset(notes: List<Note> = emptyList(), onboardingDone: Boolean = true) {
        context.filesDir.deleteRecursively()
        context.filesDir.mkdirs()
        if (notes.isNotEmpty()) runBlocking { NoteStorage(context).save(notes) }
        context.app.preferences.onboardingDone = onboardingDone
    }

    /** Attend le chargement des notes, puis exécute les tâches en attente sur le thread principal. */
    fun awaitLoaded() {
        val deadline = System.currentTimeMillis() + 5_000
        while (!context.app.repository.loaded.value) {
            check(System.currentTimeMillis() < deadline) { "Notes non chargées après 5 s" }
            idle()
            Thread.sleep(10)
        }
        idle()
    }

    /** Attend que [condition] devienne vraie (tâches en arrière-plan : sauvegarde, suppression…). */
    fun eventually(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!(runCatching(condition).getOrNull() ?: false)) {
            check(System.currentTimeMillis() < deadline) { "Condition non remplie après 5 s" }
            Thread.sleep(20)
        }
    }

    /** Avance l'horloge du thread principal (animations, défilements) et vide sa file. */
    fun idle(duration: Duration = Duration.ofSeconds(1)) {
        shadowOf(Looper.getMainLooper()).idleFor(duration)
    }
}
