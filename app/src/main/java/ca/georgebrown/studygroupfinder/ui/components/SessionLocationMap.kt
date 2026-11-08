package ca.georgebrown.studygroupfinder.ui.components

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import ca.georgebrown.studygroupfinder.BuildConfig
import ca.georgebrown.studygroupfinder.ui.map.GeoPoint
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

/** Street level: close enough to see the building, far enough to see nearby roads. */
private const val SESSION_ZOOM = 16f

/**
 * Live Google Map with a pin on the session (FR-11).
 *
 * Falls back to the drawn [MapPlaceholder] when there are no coordinates (online sessions) or when
 * MAPS_API_KEY is missing from local.properties, because a missing key otherwise shows a blank grey map.
 *
 * @param showMyLocation draws the blue "you are here" dot. Only pass true once location permission is granted,
 *   or the Maps SDK throws a SecurityException.
 * @param interactive false for the small preview on Session Details (no panning inside a scrolling screen).
 */
@Composable
fun SessionLocationMap(
    destination: GeoPoint?,
    destinationLabel: String,
    modifier: Modifier = Modifier,
    showMyLocation: Boolean = false,
    interactive: Boolean = true,
) {
    if (destination == null || !BuildConfig.HAS_MAPS_KEY) {
        if (!BuildConfig.HAS_MAPS_KEY) Log.w("SessionLocationMap", "MAPS_API_KEY not set in local.properties; showing placeholder")
        MapPlaceholder(destinationLabel = destinationLabel, showCurrentLocation = showMyLocation, modifier = modifier)
        return
    }

    val target = remember(destination) { LatLng(destination.latitude, destination.longitude) }
    // Keyed on the coordinates so the camera re-centres if the session's location is edited.
    val cameraState = rememberCameraPositionState(key = target.toString()) {
        position = CameraPosition.fromLatLngZoom(target, SESSION_ZOOM)
    }
    val markerState = rememberUpdatedMarkerState(position = target)

    GoogleMap(
        modifier = modifier.clip(MaterialTheme.shapes.medium),
        cameraPositionState = cameraState,
        properties = MapProperties(isMyLocationEnabled = showMyLocation),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = interactive,
            zoomGesturesEnabled = interactive,
            scrollGesturesEnabled = interactive,
            rotationGesturesEnabled = interactive,
            tiltGesturesEnabled = interactive,
            myLocationButtonEnabled = interactive && showMyLocation,
            mapToolbarEnabled = false, // We have our own Get Directions button.
        ),
    ) {
        Marker(state = markerState, title = destinationLabel)
    }
}
