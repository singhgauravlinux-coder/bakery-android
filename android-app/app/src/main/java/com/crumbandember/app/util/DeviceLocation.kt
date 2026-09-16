package com.crumbandember.app.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Reads the device's own last-known location (no backend call) and reverse
 * geocodes it to a short "City, State" label for the Home header. Returns
 * null if the person hasn't granted location access (see
 * LocationConsentScreen) or no provider has a fix yet — callers should show
 * a neutral fallback rather than a hardcoded city in that case.
 */
object DeviceLocation {

    @SuppressLint("MissingPermission") // Guarded by hasLocationPermission(context) below —
    // lint can't trace that check across the mapNotNull/runCatching lambdas into
    // getLastKnownLocation(), so it flags a call that's already permission-safe.
    suspend fun currentCityState(context: Context): String? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission(context)) return@withContext null

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return@withContext null

        val candidateProviders = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        val bestFix = candidateProviders
            .mapNotNull { provider ->
                runCatching {
                    if (locationManager.isProviderEnabled(provider)) locationManager.getLastKnownLocation(provider) else null
                }.getOrNull()
            }
            .maxByOrNull { it.time }
            ?: return@withContext null

        runCatching {
            @Suppress("DEPRECATION") // Sync API still works; async overload only exists on API 33+.
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val results = geocoder.getFromLocation(bestFix.latitude, bestFix.longitude, 1)
            val address = results?.firstOrNull() ?: return@runCatching null
            listOfNotNull(
                address.locality ?: address.subAdminArea,
                address.adminArea
            ).joinToString(", ").ifBlank { null }
        }.getOrNull()
    }

    private fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }
}
