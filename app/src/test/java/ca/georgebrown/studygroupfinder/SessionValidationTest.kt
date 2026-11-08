package ca.georgebrown.studygroupfinder

import ca.georgebrown.studygroupfinder.ui.editsession.validateSessionInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionValidationTest {

    @Test
    fun validCampusSessionWith2To20Seats_passesValidation() {
        val now = 1_700_000_000_000L
        val tomorrow = now + 86_400_000L
        val error = validateSessionInput(
            courseCode = "COMP3074",
            topic = "Final Exam Prep",
            hasSelectedDateTime = true,
            startAtMillis = tomorrow,
            nowMillis = now,
            isOnline = false,
            campus = "Casa Loma",
            room = "Room C240",
            meetingLink = "",
            maxParticipantsStr = "6",
            currentParticipantCount = 1,
        )
        assertNull(error)
    }

    @Test
    fun seatsOutside2To20OrBelowCurrentAttendees_areRejected() {
        val now = 1_700_000_000_000L
        val tomorrow = now + 86_400_000L

        val tooFew = validateSessionInput(
            courseCode = "COMP3074",
            topic = "Review",
            hasSelectedDateTime = true,
            startAtMillis = tomorrow,
            nowMillis = now,
            isOnline = false,
            campus = "Casa Loma",
            room = "Library",
            meetingLink = "",
            maxParticipantsStr = "1",
        )
        assertEquals("Maximum participants must be between 2 and 20.", tooFew)

        val tooMany = validateSessionInput(
            courseCode = "COMP3074",
            topic = "Review",
            hasSelectedDateTime = true,
            startAtMillis = tomorrow,
            nowMillis = now,
            isOnline = false,
            campus = "Casa Loma",
            room = "Library",
            meetingLink = "",
            maxParticipantsStr = "25",
        )
        assertEquals("Maximum participants must be between 2 and 20.", tooMany)

        val belowJoined = validateSessionInput(
            courseCode = "COMP3074",
            topic = "Review",
            hasSelectedDateTime = true,
            startAtMillis = tomorrow,
            nowMillis = now,
            isOnline = false,
            campus = "Casa Loma",
            room = "Library",
            meetingLink = "",
            maxParticipantsStr = "3",
            currentParticipantCount = 4,
        )
        assertEquals("Maximum participants cannot be less than the people already joined (4).", belowJoined)
    }
}