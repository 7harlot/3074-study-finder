package ca.georgebrown.studygroupfinder

import ca.georgebrown.studygroupfinder.data.model.LocationType
import ca.georgebrown.studygroupfinder.data.model.StudySession
import ca.georgebrown.studygroupfinder.ui.map.GeoPoint
import ca.georgebrown.studygroupfinder.ui.map.buildDirectionsUrl
import ca.georgebrown.studygroupfinder.ui.map.directionsLabel
import ca.georgebrown.studygroupfinder.ui.map.geoPoint
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

/** Get Directions link (FR-12): with GPS, without GPS, without coordinates, and on non-English phones. */
class DirectionsUrlTest {
    private val casaLoma = GeoPoint(43.6761, -79.4105)
    private val phone = GeoPoint(43.6532, -79.3832)
    private val originalLocale: Locale = Locale.getDefault()

    @After
    fun restoreLocale() = Locale.setDefault(originalLocale)

    @Test
    fun withGps_includesOriginAndDestination() {
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&origin=43.653200,-79.383200" +
                "&destination=43.676100,-79.410500&travelmode=walking",
            buildDirectionsUrl(casaLoma, "Room C412", phone),
        )
    }

    @Test
    fun permissionDenied_leavesOriginOut() {
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&destination=43.676100,-79.410500&travelmode=walking",
            buildDirectionsUrl(casaLoma, "Room C412", origin = null),
        )
    }

    @Test
    fun noCoordinates_searchesForTheEncodedLabel() {
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&destination=Casa+Loma+Campus%2C+Room+C412+%26+C413&travelmode=walking",
            buildDirectionsUrl(null, "  Casa Loma Campus, Room C412 & C413 ", origin = null),
        )
    }

    @Test
    fun nothingToNavigateTo_returnsNull() {
        assertNull(buildDirectionsUrl(null, "", phone))
        assertNull(buildDirectionsUrl(null, "   ", null))
    }

    @Test
    fun germanLocale_stillUsesDotDecimals() {
        Locale.setDefault(Locale.GERMANY) // German formats 43.6761 as "43,6761", which would break the URL.
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&destination=43.676100,-79.410500&travelmode=walking",
            buildDirectionsUrl(casaLoma, "", null),
        )
    }

    @Test
    fun sessionHelpers_handleMissingFields() {
        val online = session(latitude = null, longitude = 1.0, campus = "", room = "")
        assertNull(online.geoPoint) // Half a coordinate is no coordinate.
        assertEquals("", online.directionsLabel)

        val onCampus = session(latitude = 43.6761, longitude = -79.4105, campus = "Casa Loma", room = "C412")
        assertEquals(casaLoma, onCampus.geoPoint)
        assertEquals("Casa Loma, C412", onCampus.directionsLabel)
        assertEquals("Casa Loma", onCampus.copy(locationLabel = "").directionsLabel)
    }

    private fun session(latitude: Double?, longitude: Double?, campus: String, room: String) = StudySession(
        sessionId = "s1", creatorId = "u1", organizerName = "Test", courseCode = "COMP3074", topic = "Maps",
        description = "", startAtMillis = 0L, locationType = LocationType.ON_CAMPUS,
        campusName = campus, locationLabel = room, latitude = latitude, longitude = longitude, maxParticipants = 4,
    )
}
