package ca.georgebrown.studygroupfinder

import ca.georgebrown.studygroupfinder.data.model.LocationType
import ca.georgebrown.studygroupfinder.data.model.StudySession
import ca.georgebrown.studygroupfinder.ui.details.buildSessionShare
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Share by email (spec item 4, FR-17): campus sessions, online sessions, and missing optional fields. */
class SessionShareTest {

    private val campus = StudySession(
        sessionId = "s1", creatorId = "u1", organizerName = "Aisha Patel", courseCode = "COMP3074", topic = "Exam Review",
        description = "  Bring practice questions.  ", startAtMillis = 0L, locationType = LocationType.ON_CAMPUS,
        campusName = "Casa Loma", locationLabel = "Library, Room 2", latitude = 43.6761, longitude = -79.4105,
        maxParticipants = 6, participantIds = listOf("u1", "u2"),
    )

    @Test fun `campus session includes place, map link and seats`() {
        val share = buildSessionShare(campus, "Thursday, Nov 12 · 3:00 PM")
        assertEquals("Study session: COMP3074 - Exam Review", share.subject)
        assertTrue(share.body.contains("When: Thursday, Nov 12 · 3:00 PM"))
        assertTrue(share.body.contains("Where: Casa Loma, Library, Room 2"))
        assertTrue(share.body.contains("Map: https://www.google.com/maps/search/?api=1&query=43.676100,-79.410500"))
        assertTrue(share.body.contains("Seats: 4 seats available"))
        assertTrue(share.body.contains("\nBring practice questions.\n")) // trimmed
    }

    @Test fun `online session shows the meeting link and no map`() {
        val online = campus.copy(
            locationType = LocationType.ONLINE, meetingUrl = "https://meet.example.com/abc",
            campusName = "", locationLabel = "", latitude = null, longitude = null,
        )
        val body = buildSessionShare(online, "Fri").body
        assertTrue(body.contains("Where: Online - https://meet.example.com/abc"))
        assertFalse(body.contains("Map:"))
    }

    @Test fun `missing optional fields still produce a clean message`() {
        val bare = campus.copy(campusName = "", locationLabel = "", latitude = null, longitude = null, description = " ",
            participantIds = List(6) { "u$it" })
        val body = buildSessionShare(bare, "Mon").body
        assertTrue(body.contains("Where: On campus (room to be confirmed)"))
        assertTrue(body.contains("Seats: Full"))
        assertFalse(body.contains("\n\n\n")) // no double blank line where the description would be
    }
}
