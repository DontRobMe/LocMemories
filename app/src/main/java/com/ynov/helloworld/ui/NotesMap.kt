package com.ynov.helloworld.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ynov.helloworld.R
import com.ynov.helloworld.data.Note
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay

// region Carte

/**
 * Carte OpenStreetMap (osmdroid) affichant un marqueur par note géolocalisée.
 *
 * Comportement :
 * - cadrage automatique sur l'ensemble des notes au premier affichage (France par défaut) ;
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
 * @param interactive `false` pour une carte figée, intégrée à un écran qui défile.
 * @param onNoteClick action au toucher d'un marqueur ; `null` pour des marqueurs inertes.
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun NotesMap(
    notes: List<Note>,
    modifier: Modifier = Modifier,
    focused: Note? = null,
    interactive: Boolean = true,
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
    val pin = remember { ContextCompat.getDrawable(context, R.drawable.ic_map_pin) }
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(interactive)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            minZoomLevel = 3.0
            controller.setZoom(5.0)
            controller.setCenter(FRANCE_CENTER)
            if (!interactive) setOnTouchListener { _, _ -> true }
            importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
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
        val note = focused ?: return@LaunchedEffect
        if (note.hasLocation) {
            mapView.controller.animateTo(GeoPoint(note.latitude!!, note.longitude!!), 16.0, 600L)
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                addOnFirstLayoutListener { _, _, _, _, _ -> centerOn(this, located) }
            }
        },
        update = { map ->
            map.overlayManager.tilesOverlay.setColorFilter(if (darkTheme) TilesOverlay.INVERT_COLORS else null)
            map.overlays.removeAll { it is Marker }
            located.forEach { note ->
                val marker = Marker(map).apply {
                    position = GeoPoint(note.latitude!!, note.longitude!!)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = pin
                    title = note.title
                    setInfoWindow(null)
                    setOnMarkerClickListener { _, _ -> onNoteClick?.invoke(note); onNoteClick != null }
                }
                map.overlays.add(marker)
            }
            map.invalidate()
        },
        modifier = modifier.semantics { contentDescription = description },
    )
}

// endregion

// region Cadrage

/** Centre de la France métropolitaine, utilisé tant qu'aucune note n'est géolocalisée. */
private val FRANCE_CENTER = GeoPoint(46.6, 2.4)

/**
 * Cadre la carte sur [notes] : zoom rue pour une seule note,
 * boîte englobante avec marge pour plusieurs, rien si la liste est vide.
 */
private fun centerOn(map: MapView, notes: List<Note>) {
    val points = notes.map { GeoPoint(it.latitude!!, it.longitude!!) }
    when {
        points.isEmpty() -> Unit
        points.size == 1 -> {
            map.controller.setZoom(16.0)
            map.controller.setCenter(points.first())
        }
        else -> {
            val box = BoundingBox.fromGeoPointsSafe(points)
            map.zoomToBoundingBox(box.increaseByScale(1.4f), false, 96)
        }
    }
}

// endregion
