package com.ynov.helloworld.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import com.ynov.helloworld.R
import com.ynov.helloworld.data.Note
import org.osmdroid.config.Configuration as OsmConfiguration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import java.io.File

// region Contrôleur de carte

/**
 * Pilote une [MapView] osmdroid (OpenStreetMap) déclarée dans un layout XML :
 * un marqueur par note géolocalisée.
 *
 * - cadrage automatique sur les notes au premier affichage (France par défaut) ;
 * - marqueur agrandi et caméra animée sur la note mise en avant ([focus]) ;
 * - zoom au pincement uniquement, tuiles inversées en thème sombre ;
 * - mention obligatoire « © OpenStreetMap contributors ».
 *
 * L'activité hôte relaie son cycle de vie : `map.onResume()` / `map.onPause()`,
 * puis `map.onDetach()` dans `onDestroy`.
 *
 * Accessibilité : les tuiles ne sont pas lisibles par TalkBack, la carte porte donc
 * une description qui la résume (nombre de notes ou titre de la note).
 *
 * @param interactive `false` pour une carte figée, intégrée à un écran qui défile.
 * @param onNoteClick action au toucher d'un marqueur ; `null` pour des marqueurs inertes.
 */
class NotesMap(
    private val map: MapView,
    interactive: Boolean = true,
    private val onNoteClick: ((Note) -> Unit)? = null,
) {
    private val context = map.context
    private val pins = Pins.from(context)
    private val copyright = CopyrightOverlay(context)
    private var notes: List<Note> = emptyList()
    private var focusedId: Long? = null
    private var fitPadding = 0
    private var framed = false

    init {
        map.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(interactive)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            minZoomLevel = 3.0
            controller.setZoom(5.0)
            controller.setCenter(FRANCE_CENTER)
            if (!interactive) disableGestures()
            val dark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
            if (dark) overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
            copyright.setTextColor(if (dark) 0xFFE0E0E0.toInt() else 0xFF424242.toInt())
            overlays.add(copyright)
        }
    }

    /** Affiche [notes] ; celles sans position sont ignorées. Cadre la carte au premier appel. */
    fun setNotes(notes: List<Note>) {
        this.notes = notes.filter { it.hasLocation }
        map.contentDescription = describe(this.notes)
        refreshMarkers()
        if (!framed && this.notes.isNotEmpty()) {
            framed = true
            if (map.isLayoutOccurred) frame() else map.addOnFirstLayoutListener { _, _, _, _, _ -> frame() }
        }
    }

    /** Centre la carte (avec animation) sur [note] et agrandit son marqueur. */
    fun focus(note: Note) {
        focusedId = note.id
        refreshMarkers()
        if (note.hasLocation) {
            map.controller.animateTo(GeoPoint(note.latitude!!, note.longitude!!), 16.0, 600L)
        }
    }

    /**
     * Zones de la carte recouvertes par l'interface (barres, carrousel), en pixels :
     * l'attribution et le cadrage des notes en tiennent compte.
     */
    fun setCoveredInsets(left: Int, top: Int, bottom: Int) {
        val margin = (8 * context.resources.displayMetrics.density).toInt()
        copyright.setOffset(left + margin, bottom + margin)
        fitPadding = maxOf(top, bottom) + 6 * margin
        map.invalidate()
    }

    private fun refreshMarkers() {
        map.overlays.removeAll { it is Marker }
        notes.forEach { note ->
            map.overlays.add(
                Marker(map).apply {
                    position = GeoPoint(note.latitude!!, note.longitude!!)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = if (note.id == focusedId) pins.focused else pins.normal
                    title = note.title
                    setInfoWindow(null)
                    setOnMarkerClickListener { _, _ ->
                        onNoteClick?.invoke(note)
                        onNoteClick != null
                    }
                }
            )
        }
        map.invalidate()
    }

    /** Zoom rue pour une seule note, boîte englobante pour plusieurs. */
    private fun frame() {
        val points = notes.map { GeoPoint(it.latitude!!, it.longitude!!) }
        when (points.size) {
            0 -> Unit
            1 -> {
                map.controller.setZoom(16.0)
                map.controller.setCenter(points.first())
            }
            else -> map.zoomToBoundingBox(BoundingBox.fromGeoPointsSafe(points), false, fitPadding)
        }
    }

    private fun describe(notes: List<Note>): String = when (notes.size) {
        0 -> context.getString(R.string.map_description_none)
        1 -> context.getString(R.string.map_description_one, notes.first().title)
        else -> context.getString(R.string.map_description_many, notes.size)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun MapView.disableGestures() = setOnTouchListener { _, _ -> true }
}

// endregion

// region Configuration

/** Centre de la France métropolitaine, utilisé tant qu'aucune note n'est géolocalisée. */
private val FRANCE_CENTER = GeoPoint(46.6, 2.4)

/**
 * Configure osmdroid. À appeler avant l'inflation de toute [MapView] (voir `App.onCreate`) :
 * la vue lit cette configuration dès sa création.
 *
 * - user-agent obligatoire pour télécharger les tuiles OpenStreetMap ;
 * - cache des tuiles dans le stockage privé de l'application : le dossier externe par défaut
 *   n'est plus accessible en écriture depuis Android 10, et les tuiles étaient alors
 *   retéléchargées à chaque affichage.
 */
fun Context.configureOsmdroid() {
    OsmConfiguration.getInstance().apply {
        userAgentValue = packageName
        osmdroidBasePath = File(cacheDir, "osmdroid")
        osmdroidTileCache = File(osmdroidBasePath, "tiles")
    }
}

/** Marqueurs : normal et agrandi (note mise en avant). */
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

// endregion
