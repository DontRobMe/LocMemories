package com.ynov.locmemories

import com.ynov.locmemories.data.Note

/** Notes de test : deux localisées, une sans position, aucune photo. */
val testNotes = listOf(
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
