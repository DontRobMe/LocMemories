package com.ynov.helloworld.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

// region Permissions

/** `true` si l'utilisateur a accordé la localisation précise ou approximative. */
fun hasLocationPermission(context: Context): Boolean =
    listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

// endregion

// region Localisation

/**
 * Récupère la position actuelle de l'appareil.
 *
 * Utilise directement [LocationManager] (aucune dépendance à Google Play Services) :
 * 1. demande une position fraîche au GPS, ou au réseau si le GPS est coupé (10 s max) ;
 * 2. à défaut, renvoie la dernière position connue la plus récente.
 *
 * @return la position, ou `null` si la permission est refusée ou aucune source n'est disponible.
 */
@SuppressLint("MissingPermission")
suspend fun fetchCurrentLocation(context: Context): Location? {
    if (!hasLocationPermission(context)) return null
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val provider = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .firstOrNull { manager.isProviderEnabled(it) }
        ?: return null

    val current = withTimeoutOrNull(10_000) { requestSingleLocation(manager, provider) }
    return current
        ?: manager.getProviders(true).mapNotNull { manager.getLastKnownLocation(it) }
            .maxByOrNull { it.time }
}

/**
 * Demande une mise à jour unique de position à [provider], sous forme de fonction suspendue.
 *
 * Android 11+ dispose de `getCurrentLocation` ; les versions antérieures utilisent
 * `requestSingleUpdate`. L'annulation de la coroutine annule aussi la requête système.
 */
@SuppressLint("MissingPermission")
private suspend fun requestSingleLocation(manager: LocationManager, provider: String): Location? =
    suspendCancellableCoroutine { cont ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val signal = android.os.CancellationSignal()
            cont.invokeOnCancellation { signal.cancel() }
            manager.getCurrentLocation(provider, signal, Runnable::run) { location ->
                if (cont.isActive) cont.resume(location)
            }
        } else {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    manager.removeUpdates(this)
                    if (cont.isActive) cont.resume(location)
                }
            }
            cont.invokeOnCancellation { manager.removeUpdates(listener) }
            @Suppress("DEPRECATION")
            manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        }
    }

// endregion
