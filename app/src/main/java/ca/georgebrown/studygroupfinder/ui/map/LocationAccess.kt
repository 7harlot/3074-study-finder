package ca.georgebrown.studygroupfinder.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val TAG = "LocationAccess"

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

/** True if the user allowed either precise or approximate location. Approximate is enough for walking directions. */
fun Context.hasLocationPermission(): Boolean = LOCATION_PERMISSIONS.any {
    ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
}

/**
 * Runtime location permission for a screen.
 * [isGranted] updates as soon as the user answers the system dialog; [request] shows that dialog.
 */
@Stable
class LocationPermissionState internal constructor(initiallyGranted: Boolean) {
    var isGranted by mutableStateOf(initiallyGranted)
        internal set

    /** Becomes true after the user has answered the dialog once, so we don't nag on every recomposition. */
    var hasAsked by mutableStateOf(false)
        internal set

    internal var launch: () -> Unit = {}

    fun request() = launch()
}

@Composable
fun rememberLocationPermissionState(): LocationPermissionState {
    val context = LocalContext.current
    val state = remember { LocationPermissionState(context.hasLocationPermission()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        state.isGranted = results.values.any { it }
        state.hasAsked = true
        Log.d(TAG, "Location permission result: $results")
    }
    state.launch = { launcher.launch(LOCATION_PERMISSIONS) }
    return state
}

/**
 * One-shot current GPS position from FusedLocationProviderClient, or null if we can't get one
 * (permission missing, location turned off, no fix yet). Never throws: callers just fall back.
 */
@SuppressLint("MissingPermission") // Checked on the first line.
suspend fun Context.currentLocationOrNull(): GeoPoint? {
    if (!hasLocationPermission()) return null
    val client = LocationServices.getFusedLocationProviderClient(this)
    val cancellation = CancellationTokenSource()
    return try {
        suspendCancellableCoroutine { continuation ->
            // Balanced accuracy (~100 m) is plenty for "start directions from here" and is easy on the battery.
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token)
                .addOnSuccessListener { location ->
                    continuation.resume(location?.let { GeoPoint(it.latitude, it.longitude) })
                }
                .addOnFailureListener { error ->
                    Log.w(TAG, "Couldn't get current location", error)
                    continuation.resume(null)
                }
            continuation.invokeOnCancellation { cancellation.cancel() }
        }
    } catch (error: SecurityException) {
        // The user can revoke permission in Settings between our check and the call.
        Log.w(TAG, "Location permission revoked mid-request", error)
        null
    }
}
