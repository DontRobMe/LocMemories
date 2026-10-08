package com.ynov.helloworld.ui.detail

import androidx.lifecycle.SavedStateHandle
import com.ynov.helloworld.MainDispatcherRule
import com.ynov.helloworld.TestEnvironment
import com.ynov.helloworld.app
import com.ynov.helloworld.testNotes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests du [NoteDetailViewModel] : note affichée, note introuvable et suppression. */
@RunWith(RobolectricTestRunner::class)
class NoteDetailViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository get() = TestEnvironment.context.app.repository

    @Before
    fun setUp() = TestEnvironment.reset(testNotes)

    /** ViewModel ouvert sur la note [id], comme depuis l'intent de [NoteDetailActivity.start]. */
    private fun viewModel(id: Long) =
        NoteDetailViewModel(repository, SavedStateHandle(mapOf(NoteDetailViewModel.EXTRA_ID to id)))

    private fun NoteDetailViewModel.awaitState(condition: (NoteDetailState) -> Boolean): NoteDetailState =
        runBlocking { withTimeout(5_000) { state.first(condition) } }

    @Test
    fun `la note demandée est affichée`() {
        val state = viewModel(2).awaitState { it.note != null }

        assertEquals("Idée de projet", state.note?.title)
    }

    @Test
    fun `une note introuvable ferme l'écran une fois le chargement terminé`() {
        val state = viewModel(42).awaitState { it.closed }

        assertEquals(null, state.note)
    }

    @Test
    fun `supprimer la note la retire du repository et ferme l'écran`() {
        val viewModel = viewModel(2)
        viewModel.awaitState { it.note != null }

        viewModel.delete()

        assertTrue(viewModel.awaitState { it.closed }.closed)
        assertTrue(repository.notes.value.none { it.id == 2L })
    }
}
