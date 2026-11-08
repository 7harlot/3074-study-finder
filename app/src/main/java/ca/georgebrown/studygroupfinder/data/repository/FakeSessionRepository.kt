package ca.georgebrown.studygroupfinder.data.repository

import ca.georgebrown.studygroupfinder.data.model.LocationType
import ca.georgebrown.studygroupfinder.data.model.StudySession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Calendar
import java.util.UUID

/**
 * In-memory sessions for the prototype and for unit tests. Starts with sample data matching the mockups
 * (pass an empty list in tests). The rules here are the reference the Firestore version must match.
 */
class FakeSessionRepository(initial: List<StudySession>? = null) : SessionRepository {

    private val _sessions = MutableStateFlow(initial ?: sampleSessions())
    override val sessions: StateFlow<List<StudySession>> = _sessions.asStateFlow()

    override fun getSession(sessionId: String): StudySession? =
        _sessions.value.firstOrNull { it.sessionId == sessionId }

    override suspend fun createSession(session: StudySession): SessionResult {
        val id = UUID.randomUUID().toString()
        // The creator is always the first participant, so it shows under both Joined and Created.
        val saved = session.copy(sessionId = id, participantIds = (listOf(session.creatorId) + session.participantIds).distinct())
        _sessions.update { it + saved }
        return SessionResult.Success(id)
    }

    override suspend fun updateSession(session: StudySession, requesterId: String): SessionResult =
        mutate(session.sessionId) { existing ->
            if (existing.creatorId != requesterId) return@mutate Edit.Fail(SessionErrors.NOT_CREATOR)
            // Edits can't change who created it or who has joined; join/leave own those fields.
            Edit.Replace(session.copy(creatorId = existing.creatorId, participantIds = existing.participantIds))
        }

    override suspend fun deleteSession(sessionId: String, requesterId: String): SessionResult =
        mutate(sessionId) { existing ->
            if (existing.creatorId != requesterId) Edit.Fail(SessionErrors.NOT_CREATOR) else Edit.Remove
        }

    override suspend fun joinSession(sessionId: String, userId: String): SessionResult =
        mutate(sessionId) { existing ->
            when {
                userId in existing.participantIds -> Edit.Fail(SessionErrors.ALREADY_JOINED)
                existing.isFull -> Edit.Fail(SessionErrors.FULL)
                else -> Edit.Replace(existing.copy(participantIds = existing.participantIds + userId))
            }
        }

    override suspend fun leaveSession(sessionId: String, userId: String): SessionResult =
        mutate(sessionId) { existing ->
            when {
                existing.creatorId == userId -> Edit.Fail(SessionErrors.CREATOR_CANNOT_LEAVE)
                userId !in existing.participantIds -> Edit.Fail(SessionErrors.NOT_JOINED)
                else -> Edit.Replace(existing.copy(participantIds = existing.participantIds - userId))
            }
        }

    /** What a write decided to do with one session. */
    private sealed interface Edit {
        data class Replace(val session: StudySession) : Edit
        data object Remove : Edit
        data class Fail(val message: String) : Edit
    }

    /**
     * Looks up a session, lets [decide] check the rules, and applies the result in one step.
     * This is the in-memory version of a Firestore transaction: read, check, write, nothing in between.
     */
    private fun mutate(sessionId: String, decide: (StudySession) -> Edit): SessionResult {
        var result: SessionResult = SessionResult.Error(SessionErrors.NOT_FOUND)
        _sessions.update { all ->
            val existing = all.firstOrNull { it.sessionId == sessionId } ?: return@update all.also {
                result = SessionResult.Error(SessionErrors.NOT_FOUND)
            }
            when (val edit = decide(existing)) {
                is Edit.Fail -> all.also { result = SessionResult.Error(edit.message) }
                is Edit.Remove -> all.filterNot { it.sessionId == sessionId }.also { result = SessionResult.Success(sessionId) }
                is Edit.Replace -> all.map { if (it.sessionId == sessionId) edit.session else it }
                    .also { result = SessionResult.Success(sessionId) }
            }
        }
        return result
    }

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
