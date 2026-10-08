package com.ynov.helloworld.ui

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ynov.helloworld.R
import com.ynov.helloworld.data.Note
import com.ynov.helloworld.ui.icons.Map
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import java.io.File

// region Carte

/**
 * Carte OpenStreetMap (osmdroid) affichant un marqueur par note géolocalisée.
 *
 * Comportement :
 * - cadrage automatique sur l'ensemble des notes au premier affichage (France par défaut) ;
 * - marqueur agrandi et caméra animée sur la note [focused] ;
 * - zoom au pincement uniquement, les boutons +/- d'osmdroid étant masqués ;
 * - tuiles inversées en thème sombre ;
 * - dans l'aperçu Android Studio, osmdroid ne pouvant pas s'exécuter,
 *   un simple placeholder est dessiné à la place.
 *
 * Accessibilité : la vue native est masquée à TalkBack et remplacée par une
 * description qui résume la carte (nombre de notes ou titre de la note).
 *
 * @param notes notes à afficher ; celles sans position sont ignorées.
 * @param focused note sur laquelle centrer la carte (avec animation) quand elle change.
 * @param ornamentPadding zones recouvertes par l'interface (barres, carrousel) : l'attribution
 *   OpenStreetMap et le cadrage des notes en tiennent compte.
 * @param onNoteClick action au toucher d'un marqueur ; `null` pour des marqueurs inertes.
 */
@Composable
fun NotesMap(
    notes: List<Note>,
    modifier: Modifier = Modifier,
    focused: Note? = null,
    ornamentPadding: PaddingValues = PaddingValues(0.dp),
    onNoteClick: ((Note) -> Unit)? = null,
) {
    OsmMap(
        notes = notes,
        modifier = modifier,
        focused = focused,
        interactive = true,
        ornamentPadding = ornamentPadding,
        onNoteClick = onNoteClick,
    )
}

/**
 * Aperçu figé du lieu d'une note, pour les écrans qui défilent.
 *
 * Les gestes sont désactivés : le défilement de l'écran n'est jamais capturé par la carte.
 *
 * @param note note géolocalisée à situer.
 */
@Composable
fun StaticNoteMap(note: Note, modifier: Modifier = Modifier) {
    OsmMap(notes = listOf(note), modifier = modifier, interactive = false)
}

/** Implémentation commune de [NotesMap] et [StaticNoteMap]. */
@Composable
private fun OsmMap(
    notes: List<Note>,
    modifier: Modifier,
    focused: Note? = null,
    interactive: Boolean,
    ornamentPadding: PaddingValues = PaddingValues(0.dp),
    onNoteClick: ((Note) -> Unit)? = null,
) {
    val located = notes.filter { it.hasLocation }
    val description = when (located.size) {
        0 -> "Carte, aucune note géolocalisée"
        1 -> "Carte montrant l'emplacement de « ${located.first().title} »"
        else -> "Carte montrant ${located.size} notes"
    }

    if (LocalInspectionMode.current) {
        Box(
            modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Map, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
        return
    }

    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val padding = with(density) {
        Insets(
            left = ornamentPadding.calculateLeftPadding(layoutDirection).roundToPx(),
            top = ornamentPadding.calculateTopPadding().roundToPx(),
            right = ornamentPadding.calculateRightPadding(layoutDirection).roundToPx(),
            bottom = ornamentPadding.calculateBottomPadding().roundToPx(),
            margin = 8.dp.roundToPx(),
            fit = 48.dp.roundToPx(),
        )
    }
    val currentOnNoteClick by rememberUpdatedState(onNoteClick)

    val pins = remember { Pins.from(context) }
    val copyright = remember { CopyrightOverlay(context) }
    val mapView = remember {
        context.configureOsmdroid()
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(interactive)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            minZoomLevel = 3.0
            controller.setZoom(5.0)
            controller.setCenter(FRANCE_CENTER)
            if (!interactive) setOnTouchListener { _, _ -> true }
            overlays.add(copyright)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        }
    }

    // region Cycle de vie de la MapView

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    // endregion

    LaunchedEffect(focused?.id) {
        val note = focused?.takeIf { it.hasLocation } ?: return@LaunchedEffect
        mapView.controller.animateTo(GeoPoint(note.latitude!!, note.longitude!!), 16.0, 600L)
    }

    AndroidView(
        factory = {
            mapView.apply {
                addOnFirstLayoutListener { _, _, _, _, _ -> centerOn(this, located, padding) }
            }
        },
        update = { map ->
            map.overlayManager.tilesOverlay.setColorFilter(if (darkTheme) TilesOverlay.INVERT_COLORS else null)
            copyright.setTextColor(if (darkTheme) 0xFFE0E0E0.toInt() else 0xFF424242.toInt())
            copyright.setOffset(padding.left + padding.margin, padding.bottom + padding.margin)
            map.overlays.removeAll { it is Marker }
            located.forEach { note ->
                map.overlays.add(
                    Marker(map).apply {
                        position = GeoPoint(note.latitude!!, note.longitude!!)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = if (note.id == focused?.id) pins.focused else pins.normal
                        title = note.title
                        setInfoWindow(null)
                        setOnMarkerClickListener { _, _ ->
                            val onClick = currentOnNoteClick
                            onClick?.invoke(note)
                            onClick != null
                        }
                    }
                )
            }
            map.invalidate()
        },
        modifier = modifier.semantics { contentDescription = description },
    )
}

// endregion

// region Configuration de la carte

/** Centre de la France métropolitaine, utilisé tant qu'aucune note n'est géolocalisée. */
private val FRANCE_CENTER = GeoPoint(46.6, 2.4)

/**
 * Configure osmdroid avant la création d'une carte.
 *
 * - user-agent obligatoire pour télécharger les tuiles OpenStreetMap ;
 * - cache des tuiles dans le stockage privé de l'application : le dossier externe par défaut
 *   n'est plus accessible en écriture depuis Android 10, et les tuiles étaient alors
 *   retéléchargées à chaque affichage.
 */
private fun Context.configureOsmdroid() {
    Configuration.getInstance().apply {
        userAgentValue = packageName
        osmdroidBasePath = File(cacheDir, "osmdroid")
        osmdroidTileCache = File(osmdroidBasePath, "tiles")
    }
}

/** Marqueurs : normal et agrandi (note sélectionnée dans le carrousel). */
private class Pins(val normal: Drawable, val focused: Drawable) {
    companion object {
        fun from(context: Context): Pins {
            val bitmap = ContextCompat.getDrawable(context, R.drawable.ic_map_pin)!!.toBitmap()
            val large = bitmap.scale((bitmap.width * 1.35f).toInt(), (bitmap.height * 1.35f).toInt())
            return Pins(
                normal = BitmapDrawable(context.resources, bitmap),
                focused = BitmapDrawable(context.resources, large),
            )
        }
    }
}

/**
 * Marges en pixels autour de la carte.
 *
 * @property margin espace entre l'attribution et le bord utile.
 * @property fit marge supplémentaire laissée autour des notes lors du cadrage.
 */
private data class Insets(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val margin: Int,
    val fit: Int,
)

// endregion

// region Cadrage

/**
 * Cadre la carte sur [notes] : zoom rue pour une seule note,
 * boîte englobante (hors zones recouvertes par l'interface) pour plusieurs.
 */
private fun centerOn(map: MapView, notes: List<Note>, padding: Insets) {
    val points = notes.map { GeoPoint(it.latitude!!, it.longitude!!) }
    when {
        points.isEmpty() -> Unit
        points.size == 1 -> {
            map.controller.setZoom(16.0)
            map.controller.setCenter(points.first())
        }
        else -> {
            val border = maxOf(padding.top, padding.bottom, padding.left, padding.right) + padding.fit
            map.zoomToBoundingBox(BoundingBox.fromGeoPointsSafe(points), false, border)
        }
    }
}

// endregion
