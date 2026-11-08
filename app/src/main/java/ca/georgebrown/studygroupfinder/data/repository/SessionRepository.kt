package ca.georgebrown.studygroupfinder.data.repository

import ca.georgebrown.studygroupfinder.data.model.StudySession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * Read/write access to study sessions: the contract between the screens and the data layer.
 *
 * [FakeSessionRepository] implements it in memory today and doubles as the reference for the rules
 * (creator-only edits, no overbooking). FirestoreSessionRepository (Munir) implements the same interface,
 * so swapping it in only changes AppContainer.
 *
 * Write methods never throw for expected problems: they return [SessionResult.Error] with a message
 * that's safe to show the user ("This session is full.").
 */
interface SessionRepository {
    /** Every upcoming session, live. The Feed filters this. Firestore: a snapshot listener. */
    val sessions: StateFlow<List<StudySession>>

    fun getSession(sessionId: String): StudySession?

    /** Sessions [userId] has joined, soonest first (My Sessions > Joined, FR-10). */
    fun sessionsJoinedBy(userId: String): Flow<List<StudySession>> =
        sessions.map { all -> all.filter { userId in it.participantIds }.sortedBy { it.startAtMillis } }

    /** Sessions [userId] created, soonest first (My Sessions > Created, FR-10). */
    fun sessionsCreatedBy(userId: String): Flow<List<StudySession>> =
        sessions.map { all -> all.filter { it.creatorId == userId }.sortedBy { it.startAtMillis } }

    /** Saves a new session. Ignores [StudySession.sessionId] and returns the generated one (FR-03). */
    suspend fun createSession(session: StudySession): SessionResult

    /** Saves edits. Only the creator may edit, enforced here and in firestore.rules (FR-13). */
    suspend fun updateSession(session: StudySession, requesterId: String): SessionResult

    /** Deletes a session. Only the creator may delete (FR-13). */
    suspend fun deleteSession(sessionId: String, requesterId: String): SessionResult

    /** Adds [userId] to the session if there's a free seat. Firestore must do this in a transaction (FR-08, FR-09). */
    suspend fun joinSession(sessionId: String, userId: String): SessionResult

    /** Removes [userId] from the session. The creator can't leave their own session (FR-08). */
    suspend fun leaveSession(sessionId: String, userId: String): SessionResult
}

/** Result of a session write. [Success.sessionId] is the affected session (handy after create). */
sealed interface SessionResult {
    data class Success(val sessionId: String) : SessionResult
    data class Error(val message: String) : SessionResult
}

/** Shared user-facing messages, so the fake and Firestore versions say the same thing. */
object SessionErrors {
    const val NOT_FOUND = "This session no longer exists."
    const val NOT_CREATOR = "Only the person who created this session can change it."
    const val FULL = "This session is full."
    const val ALREADY_JOINED = "You've already joined this session."
    const val NOT_JOINED = "You haven't joined this session."
    const val CREATOR_CANNOT_LEAVE = "You created this session. Delete it instead of leaving."
    const val NETWORK = "Couldn't reach the server. Check your connection and try again."
}
