package tokyo.isseikuzumaki.nolotracker.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Service for capturing device location using Google Play Services.
 * 
 * Implements FR 2.1 (immediate location capture), FR 2.2 (FusedLocationProviderClient),
 * and FR 2.3 (5-second timeout with graceful handling).
 */
class LocationServiceImpl(private val context: Context) {
    
    private val tag = "LocationService"
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    /**
     * Captures current location with high priority and 5-second timeout.
     * 
     * FR 2.1: Triggered immediately upon notification receipt
     * FR 2.2: Uses FusedLocationProviderClient
     * FR 2.3: Returns null if location unavailable within 5 seconds
     * NFR 4.1.1: Minimizes battery consumption with one-time request
     * 
     * @return LocationData if available within timeout, null otherwise
     */
    suspend fun getCurrentLocation(): LocationData? {
        // FR 2.4: Check for location permission
        if (!hasLocationPermission()) {
            Log.w(tag, "Location permission not granted")
            return null
        }
        
        return try {
            // Create cancellation token for timeout control
            val cancellationTokenSource = CancellationTokenSource()
            
            suspendCancellableCoroutine { continuation ->
                // Configure high-accuracy request
                val currentLocationRequest = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setDurationMillis(5000) // FR 2.3: 5-second timeout
                    .setMaxUpdateAgeMillis(0) // Always get fresh location
                    .build()
                
                try {
                    fusedLocationClient.getCurrentLocation(
                        currentLocationRequest,
                        cancellationTokenSource.token
                    ).addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            Log.d(tag, "Location captured: ${location.latitude}, ${location.longitude}")
                            continuation.resume(
                                LocationData(
                                    latitude = location.latitude,
                                    longitude = location.longitude
                                )
                            )
                        } else {
                            Log.w(tag, "Location is null")
                            continuation.resume(null)
                        }
                    }.addOnFailureListener { e ->
                        Log.e(tag, "Failed to get location", e)
                        continuation.resume(null)
                    }
                    
                    // Handle coroutine cancellation
                    continuation.invokeOnCancellation {
                        cancellationTokenSource.cancel()
                        Log.d(tag, "Location request cancelled")
                    }
                    
                } catch (e: SecurityException) {
                    Log.e(tag, "Security exception getting location", e)
                    continuation.resume(null)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error getting current location", e)
            null
        }
    }
    
    /**
     * Checks if the app has location permission.
     * 
     * FR 2.4: Requires ACCESS_FINE_LOCATION permission
     * 
     * @return true if permission granted, false otherwise
     */
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    companion object {
        /**
         * Static method for easy access from NotificationListenerService.
         * 
         * @param context Application context
         * @return LocationData or null if unavailable
         */
        suspend fun getCurrentLocation(context: Context): LocationData? {
            return LocationServiceImpl(context).getCurrentLocation()
        }
    }
}
