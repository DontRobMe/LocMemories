package com.ynov.helloworld.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ynov.helloworld.data.Note
import com.ynov.helloworld.ui.theme.HelloWorldTheme
import java.io.File

// region Écran

/**
 * Détail d'une note.
 *
 * - photo pleine largeur passant sous la barre d'état, avec un dégradé
 *   qui garde les icônes système lisibles ;
 * - boutons tonals (retour, partager, supprimer) qui restent contrastés sur la photo ;
 * - mini-carte non interactive et ouverture du lieu dans une application de cartographie ;
 * - suppression protégée par une boîte de confirmation.
 *
 * @param note note à afficher.
 * @param onBack retour à l'écran précédent.
 * @param onDelete suppression confirmée de la note.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteDetailScreen(
    note: Note,
    onBack: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val hasPhoto = note.photoPath != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack, modifier = Modifier.padding(start = 4.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour à la liste")
                    }
                },
                actions = {
                    FilledTonalIconButton(onClick = { shareNote(context, note) }) {
                        Icon(Icons.Outlined.Share, contentDescription = "Partager la note")
                    }
                    FilledTonalIconButton(onClick = { confirmDelete = true }, modifier = Modifier.padding(end = 4.dp)) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Supprimer la note")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (hasPhoto) Color.Transparent else MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = if (hasPhoto) 0.dp else padding.calculateTopPadding()),
        ) {
            if (note.photoPath != null) {
                Box {
                    AsyncImage(
                        model = File(note.photoPath),
                        contentDescription = "Photo de la note « ${note.title} »",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent)))
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    note.title,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    MetaLabel(Icons.Outlined.Schedule, formatDate(note.date))
                    if (note.hasLocation) {
                        MetaLabel(
                            Icons.Outlined.Place,
                            formatCoordinates(note.latitude!!, note.longitude!!),
                            modifier = Modifier.semantics(mergeDescendants = true) {
                                contentDescription = spokenCoordinates(note.latitude, note.longitude)
                            },
                        )
                    }
                }
                if (note.content.isNotBlank()) {
                    Text(note.content, style = MaterialTheme.typography.bodyLarge)
                }

                SectionTitle("Lieu", Modifier.padding(top = 8.dp))
                if (note.hasLocation) {
                    LocationCard(note, onOpenInMaps = { openInMaps(context, note) })
                } else {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.LocationOff, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                            Text(
                                "Aucune position n'a été enregistrée pour cette note.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            title = { Text("Supprimer cette note ?") },
            text = { Text("« ${note.title} » et sa photo seront définitivement supprimées.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Annuler") }
            },
        )
    }
}

// endregion

// region Composants privés

/** Carte « Lieu » : mini-carte figée et lien vers une application de cartographie. */
@Composable
private fun LocationCard(note: Note, onOpenInMaps: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        NotesMap(
            notes = listOf(note),
            interactive = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
        )
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Écrite ici",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onOpenInMaps) {
                Text("Ouvrir dans Maps")
                Icon(
                    Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(18.dp),
                )
            }
        }
    }
}

// endregion

// region Actions externes

/**
 * Ouvre le lieu de la note dans l'application de cartographie de l'utilisateur
 * (URI `geo:`), ou affiche un message si aucune n'est installée.
 */
private fun openInMaps(context: Context, note: Note) {
    val lat = note.latitude ?: return
    val lon = note.longitude ?: return
    val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(${Uri.encode(note.title)})")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Aucune application de carte installée", Toast.LENGTH_SHORT).show()
    }
}

/** Partage la note en texte brut (titre, contenu et lien OpenStreetMap si localisée). */
private fun shareNote(context: Context, note: Note) {
    val text = buildString {
        appendLine(note.title)
        if (note.content.isNotBlank()) {
            appendLine()
            appendLine(note.content)
        }
        if (note.hasLocation) {
            appendLine()
            append("https://www.openstreetmap.org/?mlat=${note.latitude}&mlon=${note.longitude}#map=17/${note.latitude}/${note.longitude}")
        }
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, note.title)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "Partager la note"))
}

// endregion

// region Previews

@ThemePreviews
@Composable
private fun NoteDetailScreenPreview() {
    HelloWorldTheme {
        NoteDetailScreen(note = previewNotes.first(), onBack = {}, onDelete = {})
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Sans position")
@Composable
private fun NoteDetailScreenNoLocationPreview() {
    HelloWorldTheme {
        NoteDetailScreen(note = previewNotes[1], onBack = {}, onDelete = {})
    }
}

// endregion
