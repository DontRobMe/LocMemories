package com.ynov.helloworld.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ynov.helloworld.data.Note
import com.ynov.helloworld.ui.theme.HelloWorldTheme
import java.io.File

// region Écran

/**
 * Écran d'accueil : liste des notes, de la plus récente à la plus ancienne.
 *
 * - grand titre qui se replie au défilement ;
 * - recherche instantanée sur le titre et le contenu ;
 * - bouton flottant « Nouvelle note » qui se réduit à son icône pendant le défilement ;
 * - état vide illustré lorsque le carnet ne contient aucune note.
 *
 * @param notes notes à afficher.
 * @param onAddClick ouverture de l'écran de création.
 * @param onNoteClick ouverture du détail d'une note.
 * @param onMapClick ouverture de la carte des notes.
 * @param onHelpClick réaffichage de l'introduction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    notes: List<Note>,
    onAddClick: () -> Unit,
    onNoteClick: (Note) -> Unit,
    onMapClick: () -> Unit,
    onHelpClick: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(notes, query) {
        if (query.isBlank()) notes
        else notes.filter { it.title.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true) }
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Mes notes") },
                actions = {
                    IconButton(onClick = onHelpClick) {
                        Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = "Revoir l'introduction")
                    }
                    IconButton(onClick = onMapClick) {
                        Icon(Icons.Outlined.Map, contentDescription = "Voir les notes sur la carte")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (notes.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onAddClick,
                    expanded = fabExpanded,
                    icon = { Icon(Icons.Outlined.EditNote, contentDescription = null) },
                    text = { Text("Nouvelle note") },
                )
            }
        },
    ) { padding ->
        if (notes.isEmpty()) {
            EmptyState(onAddClick, Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "search") {
                SearchField(query = query, onQueryChange = { query = it })
            }
            item(key = "count") {
                Text(
                    text = when {
                        query.isNotBlank() -> "${filtered.size} résultat${if (filtered.size > 1) "s" else ""}"
                        else -> "${notes.size} note${if (notes.size > 1) "s" else ""}"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 4.dp, top = 4.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            if (filtered.isEmpty()) {
                item(key = "no-result") { NoResult(query) }
            }
            items(filtered, key = { it.id }) { note ->
                NoteCard(note, onClick = { onNoteClick(note) }, modifier = Modifier.animateItem())
            }
        }
    }
}

// endregion

// region Composants privés

/** Champ de recherche arrondi ; la touche « Rechercher » du clavier le referme. */
@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Rechercher dans mes notes") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Effacer la recherche")
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

/**
 * Carte d'une note dans la liste : photo en 16:9, titre, extrait et métadonnées.
 *
 * `Card(onClick)` fusionne la sémantique de ses enfants : TalkBack lit la note
 * d'un seul bloc et l'annonce comme un bouton. La photo est décorative.
 */
@Composable
private fun NoteCard(note: Note, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        if (note.photoPath != null) {
            AsyncImage(
                model = File(note.photoPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            )
        }
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                note.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (note.content.isNotBlank()) {
                Text(
                    note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                MetaLabel(Icons.Outlined.Schedule, formatShortDate(note.date))
                if (note.hasLocation) {
                    MetaLabel(Icons.Outlined.Place, "Localisée", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/** État affiché lorsque le carnet est vide, avec un appel à l'action. */
@Composable
private fun EmptyState(onAddClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(120.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.EditNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(56.dp),
                )
            }
        }
        Text(
            "Votre carnet est vide",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            "Capturez une idée, une photo et le lieu où vous êtes.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(
            onClick = onAddClick,
            modifier = Modifier.padding(top = 24.dp),
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        ) {
            Icon(Icons.Outlined.EditNote, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Text("Écrire ma première note", Modifier.padding(start = ButtonDefaults.IconSpacing))
        }
    }
}

/** Message affiché lorsque la recherche ne trouve aucune note. */
@Composable
private fun NoResult(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(48.dp),
        )
        Text(
            "Aucune note ne correspond à « $query »",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

// endregion

// region Previews

@ThemePreviews
@Composable
private fun NoteListScreenPreview() {
    HelloWorldTheme {
        NoteListScreen(notes = previewNotes, onAddClick = {}, onNoteClick = {}, onMapClick = {}, onHelpClick = {})
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Liste vide")
@Composable
private fun NoteListScreenEmptyPreview() {
    HelloWorldTheme {
        NoteListScreen(notes = emptyList(), onAddClick = {}, onNoteClick = {}, onMapClick = {}, onHelpClick = {})
    }
}

@Preview(showBackground = true, name = "Carte de note — texte agrandi", fontScale = 1.5f)
@Composable
private fun NoteCardLargeFontPreview() {
    HelloWorldTheme {
        NoteCard(note = previewNotes.first(), onClick = {}, modifier = Modifier.padding(16.dp))
    }
}

// endregion
