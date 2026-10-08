package com.ynov.helloworld

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ynov.helloworld.ui.AddNoteScreen
import com.ynov.helloworld.ui.MapScreen
import com.ynov.helloworld.ui.NoteDetailScreen
import com.ynov.helloworld.ui.NoteListScreen
import com.ynov.helloworld.ui.OnboardingScreen
import com.ynov.helloworld.ui.theme.HelloWorldTheme

// region Activité

/**
 * Point d'entrée unique de l'application (architecture « single activity »).
 *
 * Active l'affichage bord à bord puis délègue tout l'affichage à [NotesApp].
 */
class MainActivity : ComponentActivity() {

    private val viewModel: NotesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HelloWorldTheme {
                NotesApp(viewModel)
            }
        }
    }
}

// endregion

// region Navigation

/** Routes de navigation de l'application. */
private object Routes {
    const val ONBOARDING = "onboarding"
    const val LIST = "list"
    const val ADD = "add"
    const val DETAIL = "detail/{id}"
    const val MAP = "map"

    fun detail(id: Long) = "detail/$id"
}

/**
 * Graphe de navigation : introduction (premier lancement) → liste → ajout / détail / carte.
 *
 * Les transitions suivent le motif Material « shared axis » horizontal
 * (glissement d'un quart d'écran combiné à un fondu).
 */
@Composable
fun NotesApp(viewModel: NotesViewModel) {
    val navController = rememberNavController()
    val notes by viewModel.notes.collectAsState()
    val loaded by viewModel.loaded.collectAsState()
    val startDestination = remember {
        if (viewModel.preferences.onboardingDone) Routes.LIST else Routes.ONBOARDING
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { slideInHorizontally(tween(300)) { it / 4 } + fadeIn(tween(300)) },
        exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(150)) },
        popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
        popExitTransition = { slideOutHorizontally(tween(300)) { it / 4 } + fadeOut(tween(150)) },
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinish = {
                    viewModel.preferences.onboardingDone = true
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Routes.LIST) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                },
            )
        }
        composable(Routes.LIST) {
            NoteListScreen(
                notes = notes,
                loading = !loaded,
                onAddClick = { navController.navigate(Routes.ADD) },
                onNoteClick = { navController.navigate(Routes.detail(it.id)) },
                onMapClick = { navController.navigate(Routes.MAP) },
                onHelpClick = { navController.navigate(Routes.ONBOARDING) },
            )
        }
        composable(Routes.ADD) {
            AddNoteScreen(
                repository = viewModel.repository,
                onSave = { title, content, photo, lat, lon ->
                    viewModel.addNote(title, content, photo, lat, lon)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            Routes.DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            if (!loaded) return@composable
            val id = entry.arguments?.getLong("id")
            val note = notes.find { it.id == id }
            if (note == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                NoteDetailScreen(
                    note = note,
                    onBack = { navController.popBackStack() },
                    onDelete = {
                        navController.popBackStack()
                        viewModel.deleteNote(note.id)
                    },
                )
            }
        }
        composable(Routes.MAP) {
            MapScreen(
                notes = notes,
                onNoteClick = { navController.navigate(Routes.detail(it.id)) },
                onBack = { navController.popBackStack() },
            )
        }
    }
}

// endregion
