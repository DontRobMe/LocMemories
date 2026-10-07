package com.ynov.helloworld.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ynov.helloworld.data.Note
import com.ynov.helloworld.ui.icons.Schedule
import com.ynov.helloworld.ui.theme.HelloWorldTheme
import java.io.File

// region Écran

/**
 * Carte plein écran de toutes les notes géolocalisées.
 *
 * Le carrousel du bas liste ces notes : toucher une carte centre la vue dessus,
 * la flèche ouvre la note. Il sert aussi d'alternative accessible aux marqueurs,
 * que TalkBack ne peut pas parcourir.
 *
 * @param notes toutes les notes ; seules celles qui ont une position sont affichées.
 * @param onNoteClick ouverture du détail d'une note.
 * @param onBack retour à la liste.
 */
@Composable
fun MapScreen(
    notes: List<Note>,
    onNoteClick: (Note) -> Unit,
    onBack: () -> Unit,
) {
    val located = remember(notes) { notes.filter { it.hasLocation } }
    var focusedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val focused = located.find { it.id == focusedId }

    Box(Modifier.fillMaxSize()) {
        val bars = WindowInsets.systemBars.asPaddingValues()
        NotesMap(
            notes = located,
            focused = focused,
            onNoteClick = onNoteClick,
            ornamentPadding = PaddingValues(
                top = bars.calculateTopPadding() + 64.dp,
                bottom = bars.calculateBottomPadding() + if (located.isEmpty()) 0.dp else 112.dp,
            ),
            modifier = Modifier.fillMaxSize(),
        )

        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour à la liste")
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 2.dp,
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Text(
                    when (located.size) {
                        0 -> "Aucune note géolocalisée"
                        1 -> "1 note sur la carte"
                        else -> "${located.size} notes sur la carte"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .semantics { heading() },
                )
            }
        }

        if (located.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(located, key = { it.id }) { note ->
                    MapNoteCard(
                        note = note,
                        selected = note.id == focusedId,
                        onFocus = { focusedId = note.id },
                        onOpen = { onNoteClick(note) },
                    )
                }
            }
        }
    }
}

// endregion

// region Composants privés

/**
 * Carte compacte d'une note dans le carrousel.
 *
 * @param selected `true` si la carte est actuellement centrée sur cette note.
 * @param onFocus centre la carte sur la note.
 * @param onOpen ouvre le détail de la note.
 */
@Composable
private fun MapNoteCard(note: Note, selected: Boolean, onFocus: () -> Unit, onOpen: () -> Unit) {
    Card(
        onClick = onFocus,
        modifier = Modifier
            .width(300.dp)
            .semantics { if (selected) stateDescription = "Affichée sur la carte" },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (note.photoPath != null) {
                AsyncImage(
                    model = File(note.photoPath),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(note.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                MetaLabel(Icons.Outlined.Schedule, formatShortDate(note.date))
            }
            IconButton(onClick = onOpen) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = "Ouvrir « ${note.title} »",
                )
            }
        }
    }
}

// endregion

// region Previews

@ThemePreviews
@Composable
private fun MapScreenPreview() {
    HelloWorldTheme {
        MapScreen(notes = previewNotes, onNoteClick = {}, onBack = {})
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Aucune note géolocalisée")
@Composable
private fun MapScreenEmptyPreview() {
    HelloWorldTheme {
        MapScreen(notes = emptyList(), onNoteClick = {}, onBack = {})
    }
}

// endregion
