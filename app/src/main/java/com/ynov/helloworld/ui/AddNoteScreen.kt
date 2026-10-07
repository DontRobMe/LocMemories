package com.ynov.helloworld.ui

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.ynov.helloworld.data.NoteRepository
import com.ynov.helloworld.location.fetchCurrentLocation
import com.ynov.helloworld.location.hasLocationPermission
import com.ynov.helloworld.ui.theme.HelloWorldTheme
import kotlinx.coroutines.launch
import java.io.File

// region Écran (logique)

/**
 * Écran de création d'une note : porte l'état et les interactions système.
 *
 * - demande la permission puis capture la position dès l'ouverture ;
 * - prend une photo via l'appareil photo (`FileProvider`) ou la galerie ;
 * - supprime la photo temporaire si l'utilisateur quitte sans enregistrer.
 *
 * L'affichage est délégué à [AddNoteContent], sans état, donc prévisualisable.
 *
 * @param repository stockage utilisé pour créer et importer les photos.
 * @param onSave enregistrement de la note (titre, contenu, photo, latitude, longitude).
 * @param onBack fermeture de l'écran sans enregistrer.
 */
@Composable
fun AddNoteScreen(
    repository: NoteRepository,
    onSave: (title: String, content: String, photoPath: String?, lat: Double?, lon: Double?) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var photoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var latitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var longitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var locating by rememberSaveable { mutableStateOf(false) }
    var locationError by rememberSaveable { mutableStateOf<String?>(null) }

    fun locate() {
        locating = true
        locationError = null
        scope.launch {
            val location = fetchCurrentLocation(context)
            locating = false
            if (location != null) {
                latitude = location.latitude
                longitude = location.longitude
            } else {
                locationError = "Position indisponible. Le GPS est-il activé ?"
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) locate() else locationError = "Autorisez la localisation pour situer la note."
    }

    fun requestLocation() {
        if (hasLocationPermission(context)) {
            locate()
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    LaunchedEffect(Unit) { if (latitude == null) requestLocation() }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val pending = pendingPhotoPath
        if (success && pending != null) {
            photoPath?.let { File(it).delete() }
            photoPath = pending
        } else {
            pending?.let { File(it).delete() }
        }
        pendingPhotoPath = null
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            photoPath?.let { File(it).delete() }
            photoPath = repository.importPhoto(uri).absolutePath
        }
    }

    fun takePicture() {
        val file = repository.newPhotoFile()
        pendingPhotoPath = file.absolutePath
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        takePictureLauncher.launch(uri)
    }

    fun discardAndBack() {
        photoPath?.let { File(it).delete() }
        onBack()
    }

    BackHandler(onBack = ::discardAndBack)

    AddNoteContent(
        title = title,
        onTitleChange = { title = it },
        content = content,
        onContentChange = { content = it },
        photoPath = photoPath,
        latitude = latitude,
        longitude = longitude,
        locating = locating,
        locationError = locationError,
        onTakePicture = ::takePicture,
        onPickImage = {
            pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onRemovePhoto = {
            photoPath?.let { File(it).delete() }
            photoPath = null
        },
        onRefreshLocation = ::requestLocation,
        onSave = { onSave(title.trim(), content.trim(), photoPath, latitude, longitude) },
        onBack = ::discardAndBack,
    )
}

// endregion

// region Écran (affichage)

/**
 * Affichage de l'écran de création, sans état ni launcher.
 *
 * Le bouton « Enregistrer » n'est jamais désactivé : un bouton grisé n'explique pas
 * pourquoi l'action est impossible. Si le titre manque, le champ passe en erreur
 * avec un message explicite et reçoit le focus.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteContent(
    title: String,
    onTitleChange: (String) -> Unit,
    content: String,
    onContentChange: (String) -> Unit,
    photoPath: String?,
    latitude: Double?,
    longitude: Double?,
    locating: Boolean,
    locationError: String?,
    onTakePicture: () -> Unit,
    onPickImage: () -> Unit,
    onRemovePhoto: () -> Unit,
    onRefreshLocation: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    var showTitleError by rememberSaveable { mutableStateOf(false) }
    val titleFocus = remember { FocusRequester() }

    fun trySave() {
        if (title.isBlank()) {
            showTitleError = true
            titleFocus.requestFocus()
        } else {
            onSave()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nouvelle note") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.Close, contentDescription = "Annuler et fermer")
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Button(
                    onClick = ::trySave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Text("Enregistrer la note", Modifier.padding(start = ButtonDefaults.IconSpacing))
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val titleInError = showTitleError && title.isBlank()
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Titre") },
                supportingText = { Text(if (titleInError) "Donnez un titre à votre note" else "Obligatoire") },
                isError = titleInError,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocus),
            )
            OutlinedTextField(
                value = content,
                onValueChange = onContentChange,
                label = { Text("Contenu") },
                placeholder = { Text("Qu'avez-vous en tête ?") },
                minLines = 6,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle("Photo")
            PhotoSection(photoPath, onTakePicture, onPickImage, onRemovePhoto)

            SectionTitle("Lieu")
            LocationSection(latitude, longitude, locating, locationError, onRefreshLocation)
        }
    }
}

// endregion

// region Composants privés

/**
 * Section photo : zone d'ajout (appareil photo / galerie) ou aperçu de la photo choisie.
 *
 * Les boutons sont placés dans un `FlowRow` pour passer à la ligne
 * lorsque la taille du texte est agrandie.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhotoSection(
    photoPath: String?,
    onTakePicture: () -> Unit,
    onPickImage: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    if (photoPath == null) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    Icons.Outlined.AddAPhoto,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp),
                )
                Text(
                    "Ajoutez une photo pour vous souvenir du moment",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(onClick = onTakePicture) {
                        ButtonContent(Icons.Outlined.PhotoCamera, "Appareil photo")
                    }
                    OutlinedButton(onClick = onPickImage) {
                        ButtonContent(Icons.Outlined.PhotoLibrary, "Galerie")
                    }
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box {
                AsyncImage(
                    model = File(photoPath),
                    contentDescription = "Photo jointe à la note",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(MaterialTheme.shapes.large),
                )
                FilledTonalIconButton(
                    onClick = onRemovePhoto,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Retirer la photo")
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onTakePicture) { ButtonContent(Icons.Outlined.PhotoCamera, "Reprendre") }
                TextButton(onClick = onPickImage) { ButtonContent(Icons.Outlined.PhotoLibrary, "Choisir une autre") }
            }
        }
    }
}

/** Contenu standard d'un bouton Material : icône décorative suivie d'un libellé. */
@Composable
private fun ButtonContent(icon: ImageVector, text: String) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
    Text(text, Modifier.padding(start = ButtonDefaults.IconSpacing))
}

/**
 * Section position : état de la localisation (en cours, trouvée, erreur) et bouton d'actualisation.
 *
 * Le texte d'état est une « live region » : TalkBack annonce le résultat dès qu'il arrive.
 */
@Composable
private fun LocationSection(
    latitude: Double?,
    longitude: Double?,
    locating: Boolean,
    locationError: String?,
    onRefreshLocation: () -> Unit,
) {
    val located = latitude != null && longitude != null
    val failed = !locating && !located
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = if (failed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                contentColor = if (failed) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    when {
                        locating -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp)
                        located -> Icon(Icons.Outlined.MyLocation, contentDescription = null)
                        else -> Icon(Icons.Outlined.LocationOff, contentDescription = null)
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            ) {
                Text("Position", style = MaterialTheme.typography.labelLarge)
                when {
                    locating -> Text(
                        "Localisation en cours…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    located -> Text(
                        formatCoordinates(latitude!!, longitude!!),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics {
                            contentDescription = spokenCoordinates(latitude, longitude)
                        },
                    )
                    else -> Text(
                        locationError ?: "Aucune position",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            IconButton(onClick = onRefreshLocation, enabled = !locating) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Actualiser la position")
            }
        }
    }
}

// endregion

// region Previews

@ThemePreviews
@Composable
private fun AddNoteContentPreview() {
    HelloWorldTheme {
        AddNoteContent(
            title = "Balade au port",
            onTitleChange = {},
            content = "Superbe coucher de soleil sur le Vieux-Port.",
            onContentChange = {},
            photoPath = null,
            latitude = 43.29512,
            longitude = 5.37432,
            locating = false,
            locationError = null,
            onTakePicture = {},
            onPickImage = {},
            onRemovePhoto = {},
            onRefreshLocation = {},
            onSave = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Localisation refusée")
@Composable
private fun AddNoteContentErrorPreview() {
    HelloWorldTheme {
        AddNoteContent(
            title = "",
            onTitleChange = {},
            content = "",
            onContentChange = {},
            photoPath = null,
            latitude = null,
            longitude = null,
            locating = false,
            locationError = "Autorisez la localisation pour situer la note.",
            onTakePicture = {},
            onPickImage = {},
            onRemovePhoto = {},
            onRefreshLocation = {},
            onSave = {},
            onBack = {},
        )
    }
}

// endregion
