package ca.georgebrown.studygroupfinder.data.repository

import ca.georgebrown.studygroupfinder.data.model.LocationType
import ca.georgebrown.studygroupfinder.data.model.StudySession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

/** Sample sessions matching the mockups, so every screen has something real-looking to show. */
class FakeSessionRepository : SessionRepository {

    private val _sessions = MutableStateFlow(sampleSessions())
    override val sessions: StateFlow<List<StudySession>> = _sessions.asStateFlow()

    override fun getSession(sessionId: String): StudySession? =
        _sessions.value.firstOrNull { it.sessionId == sessionId }

    private fun sampleSessions(): List<StudySession> = listOf(
        StudySession(
            sessionId = "s1",
            creatorId = "demo-user",
            organizerName = "Aisha Patel",
            courseCode = "COMP3074",
            topic = "Exam Review",
            description = "Review activities and practice questions for the midterm.",
            startAtMillis = nextWeekday(Calendar.THURSDAY, hour = 15),
            locationType = LocationType.ON_CAMPUS,
            campusName = "Casa Loma",
            locationLabel = "Campus library, Study room 2",
            latitude = 43.6761,
            longitude = -79.4105,
            maxParticipants = 6,
            participantIds = listOf("demo-user", "u2", "u3"),
        ),
        StudySession(
            sessionId = "s2",
            creatorId = "u2",
            organizerName = "Marcus Lee",
            courseCode = "COMP3074",
            topic = "Assignment Help",
            description = "Bring your Compose layouts. We will debug navigation together.",
            startAtMillis = nextWeekday(Calendar.FRIDAY, hour = 17),
            locationType = LocationType.ONLINE,
            meetingUrl = "https://meet.example.com/comp3074-help",
            maxParticipants = 5,
            participantIds = listOf("u2", "u4", "u5"),
        ),
        StudySession(
            sessionId = "s3",
            creatorId = "u4",
            organizerName = "Priya Singh",
            courseCode = "COMP3095",
            topic = "Spring Boot Lab Prep",
            description = "Walk through the lab requirements and set up projects.",
            startAtMillis = nextWeekday(Calendar.MONDAY, hour = 13),
            locationType = LocationType.ON_CAMPUS,
            campusName = "St. James",
            locationLabel = "Building C, Room 412",
            latitude = 43.6511,
            longitude = -79.3700,
            maxParticipants = 4,
            participantIds = listOf("u4", "u6", "u7", "u8"),
        ),
    )

    /** Next occurrence of [dayOfWeek] at [hour]:00, so sample data always looks upcoming. */
    private fun nextWeekday(dayOfWeek: Int, hour: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        while (cal.get(Calendar.DAY_OF_WEEK) != dayOfWeek || cal.timeInMillis < System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }
}
