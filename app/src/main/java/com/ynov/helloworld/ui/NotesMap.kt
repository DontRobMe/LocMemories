package com.ynov.helloworld.ui

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ynov.helloworld.R
import com.ynov.helloworld.data.Note
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.snapshotter.MapSnapshot
import org.maplibre.android.snapshotter.MapSnapshotter
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

// region Carte

/**
 * Carte vectorielle MapLibre affichant un marqueur et un libellé par note géolocalisée.
 *
 * Fonds de carte OpenFreeMap (gratuits, sans clé API) : « Positron » en thème clair,
 * « Dark » en thème sombre. Les marqueurs sont dessinés par le moteur de rendu
 * (source GeoJSON + couche de symboles) : ils restent fluides quel que soit leur nombre.
 *
 * Comportement :
 * - cadrage automatique sur l'ensemble des notes au premier affichage (France par défaut) ;
 * - marqueur agrandi et caméra animée sur la note [focused] ;
 * - dans l'aperçu Android Studio, un simple placeholder remplace la carte native.
 *
 * Accessibilité : la vue native est masquée à TalkBack et remplacée par une
 * description qui résume la carte (nombre de notes ou titre de la note).
 *
 * @param notes notes à afficher ; celles sans position sont ignorées.
 * @param focused note sur laquelle centrer la carte (avec animation) quand elle change.
 * @param ornamentPadding zones recouvertes par l'interface (barres, carrousel) : la boussole,
 *   l'attribution et le cadrage des notes en tiennent compte.
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
    val labelColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val haloColor = MaterialTheme.colorScheme.surface.toArgb()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val padding = with(density) {
        Insets(
            left = ornamentPadding.calculateLeftPadding(layoutDirection).roundToPx(),
            top = ornamentPadding.calculateTopPadding().roundToPx(),
            right = ornamentPadding.calculateRightPadding(layoutDirection).roundToPx(),
            bottom = ornamentPadding.calculateBottomPadding().roundToPx(),
            margin = 12.dp.roundToPx(),
            fit = 64.dp.roundToPx(),
        )
    }

    val currentNotes by rememberUpdatedState(located)
    val currentOnNoteClick by rememberUpdatedState(onNoteClick)

    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        }
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }
    var cameraFitted by remember { mutableStateOf(false) }

    // region Cycle de vie de la MapView

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            mapView.onDestroy()
        }
    }

    // endregion

    // region Initialisation, style et données

    LaunchedEffect(mapView) {
        mapView.getMapAsync { m ->
            m.cameraPosition = CameraPosition.Builder().target(FRANCE_CENTER).zoom(4.5).build()
            m.uiSettings.apply {
                isLogoEnabled = false
            }
            m.addOnMapClickListener { point ->
                val screen = m.projection.toScreenLocation(point)
                val id = m.queryRenderedFeatures(screen, LAYER_ID)
                    .firstOrNull()?.getStringProperty(PROP_ID)?.toLongOrNull()
                val note = currentNotes.find { it.id == id }
                val onClick = currentOnNoteClick
                if (note != null && onClick != null) {
                    onClick(note)
                    true
                } else {
                    false
                }
            }
            map = m
        }
    }

    LaunchedEffect(map, padding) {
        map?.uiSettings?.apply {
            setCompassMargins(0, padding.top + padding.margin, padding.right + padding.margin, 0)
            setAttributionMargins(
                padding.left + padding.margin, 0, 0, padding.bottom + padding.margin,
            )
        }
    }

    LaunchedEffect(map, darkTheme) {
        val m = map ?: return@LaunchedEffect
        style = null
        m.setStyle(Style.Builder().fromUri(if (darkTheme) STYLE_DARK else STYLE_LIGHT)) { loaded ->
            loaded.addImage(PIN_IMAGE, pinBitmap(context))
            loaded.addSource(GeoJsonSource(SOURCE_ID))
            loaded.addLayer(notesLayer(labelColor, haloColor))
            style = loaded
        }
    }

    LaunchedEffect(style, located) {
        style?.getSourceAs<GeoJsonSource>(SOURCE_ID)?.setGeoJson(located.toFeatureCollection())
    }

    // endregion

    // region Caméra

    LaunchedEffect(style) {
        val m = map ?: return@LaunchedEffect
        if (style == null || cameraFitted) return@LaunchedEffect
        cameraFitted = true
        mapView.post { fitCamera(m, located, padding) }
    }

    LaunchedEffect(style, focused?.id) {
        val s = style ?: return@LaunchedEffect
        s.getLayerAs<SymbolLayer>(LAYER_ID)?.setProperties(
            PropertyFactory.iconSize(
                Expression.switchCase(
                    Expression.eq(Expression.get(PROP_ID), Expression.literal(focused?.id?.toString().orEmpty())),
                    Expression.literal(1.35f),
                    Expression.literal(1f),
                )
            )
        )
        val target = focused?.takeIf { it.hasLocation } ?: return@LaunchedEffect
        map?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(target.latitude!!, target.longitude!!), 15.0),
            600,
        )
    }

    // endregion

    AndroidView(
        factory = { mapView },
        modifier = modifier.semantics { contentDescription = description },
    )
}

// endregion

// region Aperçu statique

/**
 * Aperçu non interactif du lieu d'une note, pour les écrans qui défilent.
 *
 * Plutôt que d'instancier un moteur de carte complet (contexte OpenGL, gestes, animations),
 * l'image est générée une seule fois par [MapSnapshotter] puis affichée comme une simple
 * image Compose : défilement fluide, coins arrondis et transitions sans artefacts.
 * Le marqueur est dessiné par Compose au centre de l'image.
 *
 * @param note note géolocalisée à situer.
 */
@Composable
fun StaticNoteMap(note: Note, modifier: Modifier = Modifier) {
    val description = "Carte montrant l'emplacement de « ${note.title} »"
    val background = MaterialTheme.colorScheme.surfaceContainerHighest
    BoxWithConstraints(
        modifier = modifier
            .background(background)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        val inPreview = LocalInspectionMode.current
        if (!inPreview && note.hasLocation) {
            val context = LocalContext.current
            val darkTheme = isSystemInDarkTheme()
            val widthDp = maxWidth.value.toInt()
            val heightDp = maxHeight.value.toInt()
            var image by remember(note.id, darkTheme) { mutableStateOf<Bitmap?>(null) }

            DisposableEffect(note.id, darkTheme, widthDp, heightDp) {
                val snapshotter = MapSnapshotter(
                    context,
                    MapSnapshotter.Options(widthDp, heightDp)
                        .withStyleBuilder(Style.Builder().fromUri(if (darkTheme) STYLE_DARK else STYLE_LIGHT))
                        .withCameraPosition(
                            CameraPosition.Builder()
                                .target(LatLng(note.latitude!!, note.longitude!!))
                                .zoom(15.0)
                                .build()
                        )
                        .withLogo(false),
                )
                snapshotter.start(
                    object : MapSnapshotter.SnapshotReadyCallback {
                        override fun onSnapshotReady(snapshot: MapSnapshot) {
                            image = snapshot.bitmap
                        }
                    }
                )
                onDispose { snapshotter.cancel() }
            }

            Crossfade(targetState = image, label = "static-map") { bitmap ->
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(Modifier.fillMaxSize())
                }
            }
        } else {
            Icon(Icons.Outlined.Map, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
        Image(
            painter = painterResource(R.drawable.ic_map_pin),
            contentDescription = null,
            modifier = Modifier.offset(y = (-20).dp),
        )
    }
}

// endregion

// region Configuration de la carte

private const val STYLE_LIGHT = "https://tiles.openfreemap.org/styles/positron"
private const val STYLE_DARK = "https://tiles.openfreemap.org/styles/dark"
private const val SOURCE_ID = "notes-source"
private const val LAYER_ID = "notes-layer"
private const val PIN_IMAGE = "note-pin"
private const val PROP_ID = "id"
private const val PROP_TITLE = "title"

/** Centre de la France métropolitaine, utilisé tant qu'aucune note n'est géolocalisée. */
private val FRANCE_CENTER = LatLng(46.6, 2.4)

/**
 * Marges en pixels autour de la carte.
 *
 * @property margin espace entre les ornements (boussole, attribution) et le bord utile.
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

/** Couche de symboles : marqueur teal et titre de la note en dessous. */
private fun notesLayer(labelColor: Int, haloColor: Int) =
    SymbolLayer(LAYER_ID, SOURCE_ID).withProperties(
        PropertyFactory.iconImage(PIN_IMAGE),
        PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
        PropertyFactory.iconAllowOverlap(true),
        PropertyFactory.iconIgnorePlacement(true),
        PropertyFactory.textField(Expression.get(PROP_TITLE)),
        PropertyFactory.textFont(arrayOf("Noto Sans Regular")),
        PropertyFactory.textSize(12f),
        PropertyFactory.textAnchor(Property.TEXT_ANCHOR_TOP),
        PropertyFactory.textOffset(arrayOf(0f, 0.3f)),
        PropertyFactory.textMaxWidth(8f),
        PropertyFactory.textOptional(true),
        PropertyFactory.textColor(labelColor),
        PropertyFactory.textHaloColor(haloColor),
        PropertyFactory.textHaloWidth(1.5f),
    )

/** Image du marqueur, rendue depuis le vecteur `ic_map_pin`. */
private fun pinBitmap(context: Context) =
    ContextCompat.getDrawable(context, R.drawable.ic_map_pin)!!.toBitmap()

/** Convertit les notes en entités GeoJSON portant leur identifiant et leur titre. */
private fun List<Note>.toFeatureCollection(): FeatureCollection =
    FeatureCollection.fromFeatures(
        map { note ->
            Feature.fromGeometry(Point.fromLngLat(note.longitude!!, note.latitude!!)).apply {
                addStringProperty(PROP_ID, note.id.toString())
                addStringProperty(PROP_TITLE, note.title)
            }
        }
    )

// endregion

// region Cadrage

/**
 * Cadre la carte sur [notes] : zoom rue pour une seule note,
 * boîte englobante avec marges pour plusieurs, rien si la liste est vide.
 */
private fun fitCamera(map: MapLibreMap, notes: List<Note>, padding: Insets) {
    val points = notes.map { LatLng(it.latitude!!, it.longitude!!) }
    when {
        points.isEmpty() -> Unit
        points.size == 1 -> map.moveCamera(CameraUpdateFactory.newLatLngZoom(points.first(), 15.0))
        else -> map.moveCamera(
            CameraUpdateFactory.newLatLngBounds(
                LatLngBounds.Builder().includes(points).build(),
                padding.left + padding.fit,
                padding.top + padding.fit,
                padding.right + padding.fit,
                padding.bottom + padding.fit,
            )
        )
    }
}

// endregion
