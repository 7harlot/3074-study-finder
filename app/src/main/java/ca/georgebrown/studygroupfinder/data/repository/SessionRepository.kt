package ca.georgebrown.studygroupfinder.data.repository

import ca.georgebrown.studygroupfinder.data.model.StudySession
import kotlinx.coroutines.flow.StateFlow

/**
 * Read/write access to study sessions. Prototype 1 uses [FakeSessionRepository] (in-memory sample data);
 * the Firestore version (Munir's area) implements this same interface for the Functional Prototype.
 */
interface SessionRepository {
    val sessions: StateFlow<List<StudySession>>

    fun getSession(sessionId: String): StudySession?
}
