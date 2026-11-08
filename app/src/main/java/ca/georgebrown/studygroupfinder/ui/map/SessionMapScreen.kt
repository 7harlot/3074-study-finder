package ca.georgebrown.studygroupfinder.ui.map

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import ca.georgebrown.studygroupfinder.data.model.LocationType
import ca.georgebrown.studygroupfinder.data.model.StudySession
import ca.georgebrown.studygroupfinder.ui.components.AppTopBar
import ca.georgebrown.studygroupfinder.ui.components.InfoRow
import ca.georgebrown.studygroupfinder.ui.components.MissingContent
import ca.georgebrown.studygroupfinder.ui.components.SecondaryButton
import ca.georgebrown.studygroupfinder.ui.components.SectionCard
import ca.georgebrown.studygroupfinder.ui.components.SessionLocationMap
import ca.georgebrown.studygroupfinder.ui.theme.Dimens

private const val TAG = "SessionMapScreen"

/**
 * Screen 6: Full-Screen Map (FR-11, FR-12).
 * Shows the session pin, asks for location once, and opens Google Maps walking directions
 * from the user's GPS position. If location is denied, directions still open without a start point.
 */
@Composable
fun SessionMapScreen(
    session: StudySession?,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "Session Location", onBack = onBack)
        if (session == null) {
            MissingContent("This session no longer exists.")
            return@Column
        }
        if (session.locationType == LocationType.ONLINE) {
            MissingContent("This session is online, so there's no place to show on a map.")
            return@Column
        }

        val context = LocalContext.current
        val permission = rememberLocationPermissionState()
        var myLocation by remember { mutableStateOf<GeoPoint?>(null) }

        // Ask once when the screen opens; the system dialog handles "don't ask again" itself.
        LaunchedEffect(Unit) {
            if (!permission.isGranted) permission.request()
        }
        // Fetch a GPS fix whenever permission becomes available.
        LaunchedEffect(permission.isGranted) {
            if (permission.isGranted) {
                myLocation = context.currentLocationOrNull()
                Log.d(TAG, "Current location: $myLocation")
            }
        }

        SessionLocationMap(
            destination = session.geoPoint,
            destinationLabel = session.locationLabel.ifBlank { session.campusName },
            showMyLocation = permission.isGranted,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = Dimens.ScreenPadding),
        )
        Column(
            modifier = Modifier.padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            SectionCard { InfoRow(Icons.Outlined.Place, session.directionsLabel.ifBlank { "Location not set" }) }
            if (!permission.isGranted && permission.hasAsked) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InfoRow(
                        Icons.Outlined.LocationOff,
                        "Location is off. Directions will start from wherever Google Maps thinks you are.",
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = permission::request) { Text("Allow") }
                }
            }
            SecondaryButton(
                "Get Directions",
                onClick = {
                    val url = buildDirectionsUrl(session.geoPoint, session.directionsLabel, myLocation)
                    if (url == null) {
                        Toast.makeText(context, "This session has no location yet.", Toast.LENGTH_SHORT).show()
                    } else {
                        context.openUrl(url)
                    }
                },
                icon = Icons.Outlined.Directions,
                modifier = Modifier.fillMaxWidth(),
            )
            if (session.geoPoint == null) {
                Text(
                    "No map pin saved for this session, so directions search for the room name instead.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Opens Google Maps if installed, otherwise the browser. Shows a message instead of crashing if neither exists. */
private fun Context.openUrl(url: String) {
    Log.d(TAG, "Opening directions: $url")
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (error: ActivityNotFoundException) {
        Log.w(TAG, "No app can open $url", error)
        Toast.makeText(this, "No maps app or browser found.", Toast.LENGTH_SHORT).show()
    }
}
