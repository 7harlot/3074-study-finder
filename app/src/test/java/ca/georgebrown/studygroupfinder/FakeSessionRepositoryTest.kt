package ca.georgebrown.studygroupfinder

import ca.georgebrown.studygroupfinder.data.model.LocationType
import ca.georgebrown.studygroupfinder.data.model.StudySession
import ca.georgebrown.studygroupfinder.data.repository.FakeSessionRepository
import ca.georgebrown.studygroupfinder.data.repository.SessionErrors
import ca.georgebrown.studygroupfinder.data.repository.SessionResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The session rules every SessionRepository must follow (FR-03, FR-08, FR-09, FR-10, FR-13).
 * FirestoreSessionRepository should pass the same scenarios.
 */
class FakeSessionRepositoryTest {

    private fun session(id: String, creator: String, max: Int = 3, participants: List<String> = listOf(creator), start: Long = 1000L) =
        StudySession(
            sessionId = id, creatorId = creator, organizerName = creator, courseCode = "COMP3074", topic = "Review",
            description = "", startAtMillis = start, locationType = LocationType.ONLINE,
            maxParticipants = max, participantIds = participants,
        )

    @Test fun `create assigns a new id and adds the creator as a participant`() = runTest {
        val repo = FakeSessionRepository(emptyList())
        val result = repo.createSession(session(id = "ignored", creator = "ben", participants = emptyList()))
        val id = (result as SessionResult.Success).sessionId
        assertTrue(id != "ignored")
        assertEquals(listOf("ben"), repo.getSession(id)?.participantIds)
    }

    @Test fun `join adds a user until the session is full`() = runTest {
        val repo = FakeSessionRepository(listOf(session("s1", "ben", max = 2)))
        assertEquals(SessionResult.Success("s1"), repo.joinSession("s1", "munir"))
        assertEquals(SessionResult.Error(SessionErrors.FULL), repo.joinSession("s1", "aisha")) // FR-09: no overbooking
        assertEquals(listOf("ben", "munir"), repo.getSession("s1")?.participantIds)
    }

    @Test fun `joining twice or a missing session is refused`() = runTest {
        val repo = FakeSessionRepository(listOf(session("s1", "ben")))
        assertEquals(SessionResult.Error(SessionErrors.ALREADY_JOINED), repo.joinSession("s1", "ben"))
        assertEquals(SessionResult.Error(SessionErrors.NOT_FOUND), repo.joinSession("nope", "ben"))
    }

    @Test fun `leave frees a seat but the creator cannot leave`() = runTest {
        val repo = FakeSessionRepository(listOf(session("s1", "ben", participants = listOf("ben", "munir"))))
        assertEquals(SessionResult.Success("s1"), repo.leaveSession("s1", "munir"))
        assertEquals(SessionResult.Error(SessionErrors.NOT_JOINED), repo.leaveSession("s1", "munir"))
        assertEquals(SessionResult.Error(SessionErrors.CREATOR_CANNOT_LEAVE), repo.leaveSession("s1", "ben"))
    }

    @Test fun `only the creator can edit or delete`() = runTest {
        val repo = FakeSessionRepository(listOf(session("s1", "ben", participants = listOf("ben", "munir"))))
        val edited = repo.getSession("s1")!!.copy(topic = "Hacked")
        assertEquals(SessionResult.Error(SessionErrors.NOT_CREATOR), repo.updateSession(edited, requesterId = "munir"))
        assertEquals(SessionResult.Error(SessionErrors.NOT_CREATOR), repo.deleteSession("s1", requesterId = "munir"))
        assertEquals("Review", repo.getSession("s1")?.topic)

        assertEquals(SessionResult.Success("s1"), repo.deleteSession("s1", requesterId = "ben"))
        assertNull(repo.getSession("s1"))
    }

    @Test fun `editing cannot change the creator or the participant list`() = runTest {
        val repo = FakeSessionRepository(listOf(session("s1", "ben", participants = listOf("ben", "munir"))))
        val sneaky = repo.getSession("s1")!!.copy(topic = "Finals", creatorId = "aisha", participantIds = emptyList())
        repo.updateSession(sneaky, requesterId = "ben")
        val saved = repo.getSession("s1")!!
        assertEquals("Finals", saved.topic)
        assertEquals("ben", saved.creatorId)
        assertEquals(listOf("ben", "munir"), saved.participantIds)
    }

    @Test fun `joined and created lists are per user and soonest first`() = runTest {
        val repo = FakeSessionRepository(
            listOf(
                session("late", "ben", start = 3000L),
                session("early", "ben", start = 1000L),
                session("other", "munir", participants = listOf("munir", "ben"), start = 2000L),
            ),
        )
        assertEquals(listOf("early", "late"), repo.sessionsCreatedBy("ben").first().map { it.sessionId })
        assertEquals(listOf("early", "other", "late"), repo.sessionsJoinedBy("ben").first().map { it.sessionId })
        assertEquals(emptyList<StudySession>(), repo.sessionsCreatedBy("aisha").first())
    }
}
