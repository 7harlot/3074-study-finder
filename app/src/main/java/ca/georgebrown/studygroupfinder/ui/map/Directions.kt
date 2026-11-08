package ca.georgebrown.studygroupfinder.ui.map

import ca.georgebrown.studygroupfinder.data.model.StudySession
import java.net.URLEncoder
import java.util.Locale

/** A plain latitude/longitude pair. Kept free of Android/Maps types so it can be unit tested on the JVM. */
data class GeoPoint(val latitude: Double, val longitude: Double)

/** The session's pin, or null for online sessions or sessions saved without coordinates. */
val StudySession.geoPoint: GeoPoint?
    get() {
        val lat = latitude ?: return null
        val lng = longitude ?: return null
        return GeoPoint(lat, lng)
    }

/** The text Google Maps searches for when a session has no coordinates, e.g. "St. James Campus, Room C412". */
val StudySession.directionsLabel: String
    get() = listOf(campusName, locationLabel).filter { it.isNotBlank() }.joinToString(", ")

/**
 * Builds a Google Maps "directions" link (Maps URLs API: https://developers.google.com/maps/documentation/urls/get-started).
 *
 * - [destination] wins when present; otherwise we fall back to searching [destinationLabel].
 * - [origin] is the device GPS position. When it's null (permission denied, GPS off) we leave it out,
 *   and Google Maps starts from wherever it thinks the user is, so the button still works.
 *
 * Returns null when there's nothing to navigate to.
 */
fun buildDirectionsUrl(destination: GeoPoint?, destinationLabel: String, origin: GeoPoint?): String? {
    val destinationParam = when {
        destination != null -> destination.toUrlParam()
        destinationLabel.isNotBlank() -> URLEncoder.encode(destinationLabel.trim(), "UTF-8")
        else -> return null
    }
    val originPart = origin?.let { "&origin=${it.toUrlParam()}" }.orEmpty()
    return "https://www.google.com/maps/dir/?api=1$originPart&destination=$destinationParam&travelmode=walking"
}

// Locale.US so a French/German phone doesn't write "43,67" (a comma would split the coordinates).
private fun GeoPoint.toUrlParam(): String = String.format(Locale.US, "%.6f,%.6f", latitude, longitude)
