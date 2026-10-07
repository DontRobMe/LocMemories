package com.ynov.helloworld.ui

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview
import com.ynov.helloworld.data.Note

// region Données factices

/** Jeu de notes factices réservé aux `@Preview` (avec et sans position, sans photo). */
internal val previewNotes = listOf(
    Note(
        id = 1,
        title = "Balade au port",
        content = "Superbe coucher de soleil sur le Vieux-Port, penser à revenir avec un trépied.",
        photoPath = null,
        date = 1_759_840_000_000,
        latitude = 43.29512,
        longitude = 5.37432,
    ),
    Note(
        id = 2,
        title = "Idée de projet",
        content = "Une appli qui rappelle les notes quand on repasse au même endroit.",
        photoPath = null,
        date = 1_759_750_000_000,
        latitude = null,
        longitude = null,
    ),
    Note(
        id = 3,
        title = "Café sympa",
        content = "Bon wifi, prises partout, calme le matin.",
        photoPath = null,
        date = 1_759_600_000_000,
        latitude = 43.29960,
        longitude = 5.38420,
    ),
)

// endregion

// region Annotations de preview

/**
 * Multi-preview : rend l'écran annoté en thème clair et en thème sombre,
 * dans un cadre de téléphone complet.
 */
@Preview(name = "Clair", showBackground = true, showSystemUi = true)
@Preview(
    name = "Sombre",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
annotation class ThemePreviews

// endregion
