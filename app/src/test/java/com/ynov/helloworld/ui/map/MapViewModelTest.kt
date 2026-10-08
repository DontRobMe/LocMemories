package com.ynov.helloworld.ui.map

import androidx.lifecycle.SavedStateHandle
import com.ynov.helloworld.MainDispatcherRule
import com.ynov.helloworld.TestEnvironment
import com.ynov.helloworld.app
import com.ynov.helloworld.testNotes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests du [MapViewModel] : notes géolocalisées et note sélectionnée. */
@RunWith(RobolectricTestRunner::class)
class MapViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @Before
    fun setUp() = TestEnvironment.reset(testNotes)

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        MapViewModel(TestEnvironment.context.app.repository, handle)

    private fun MapViewModel.awaitState(condition: (MapState) -> Boolean): MapState =
        runBlocking { withTimeout(5_000) { state.first(condition) } }

    @Test
    fun `seules les notes géolocalisées sont sur la carte`() {
        val state = viewModel().awaitState { it.notes.isNotEmpty() }

        assertEquals(listOf("Balade au port", "Café sympa"), state.notes.map { it.title })
        assertNull(state.selected)
    }

    @Test
    fun `sélectionner une note la met en avant et la mémorise`() {
        val handle = SavedStateHandle()
        val viewModel = viewModel(handle)
        val note = viewModel.awaitState { it.notes.isNotEmpty() }.notes.last()

        viewModel.select(note)

        assertEquals(note.id, viewModel.awaitState { it.selected != null }.selected?.id)
        assertEquals(note.id, handle.get<Long>("selected"))
    }

    @Test
    fun `la sélection est restaurée après une rotation`() {
        val state = viewModel(SavedStateHandle(mapOf("selected" to 3L))).awaitState { it.selected != null }

        assertEquals("Café sympa", state.selected?.title)
    }
}
