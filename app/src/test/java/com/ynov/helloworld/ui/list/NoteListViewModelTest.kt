package com.ynov.helloworld.ui.list

import com.ynov.helloworld.MainDispatcherRule
import com.ynov.helloworld.TestEnvironment
import com.ynov.helloworld.app
import com.ynov.helloworld.testNotes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests du [NoteListViewModel] : état de la liste et recherche. */
@RunWith(RobolectricTestRunner::class)
class NoteListViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private fun viewModel() = TestEnvironment.context.app.let { NoteListViewModel(it.repository, it.preferences) }

    private fun NoteListViewModel.awaitState(condition: (NoteListState) -> Boolean): NoteListState =
        runBlocking { withTimeout(5_000) { state.first(condition) } }

    @Test
    fun `l'état indique un carnet vide une fois le chargement terminé`() {
        TestEnvironment.reset()

        val state = viewModel().awaitState { !it.loading }

        assertTrue(state.empty)
    }

    @Test
    fun `toutes les notes sont listées sans recherche`() {
        TestEnvironment.reset(testNotes)

        val state = viewModel().awaitState { !it.loading }

        assertFalse(state.empty)
        assertEquals(3, state.total)
        assertEquals(3, state.notes.size)
    }

    @Test
    fun `la recherche filtre sur le contenu sans tenir compte de la casse`() {
        TestEnvironment.reset(testNotes)
        val viewModel = viewModel()

        viewModel.search("WIFI")

        val state = viewModel.awaitState { !it.loading && it.query == "WIFI" }
        assertEquals(listOf("Café sympa"), state.notes.map { it.title })
        assertEquals(3, state.total)
    }

    @Test
    fun `l'introduction est demandée tant qu'elle n'a pas été vue`() {
        TestEnvironment.reset(onboardingDone = false)
        assertTrue(viewModel().onboardingNeeded)

        TestEnvironment.reset(onboardingDone = true)
        assertFalse(viewModel().onboardingNeeded)
    }
}
