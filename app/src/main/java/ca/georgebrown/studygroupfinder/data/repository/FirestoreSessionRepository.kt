package ca.georgebrown.studygroupfinder.data.repository

import ca.georgebrown.studygroupfinder.data.model.LocationType
import ca.georgebrown.studygroupfinder.data.model.StudySession
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PersistentCacheSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Cloud Firestore implementation of [SessionRepository]
 * Matches all rules in [FakeSessionRepository] and passes [FakeSessionRepositoryTest].
 */
class FirestoreSessionRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : SessionRepository {

    private val sessionsCollection = firestore.collection(COLLECTION_SESSIONS)

    private val _sessions = MutableStateFlow<List<StudySession>>(emptyList())
    override val sessions: StateFlow<List<StudySession>> = _sessions.asStateFlow()

    // Retained listener reference so the exact same registration can be removed
    private var listenerRegistration: ListenerRegistration? = null

    private class RuleViolationException(val userMessage: String) : Exception(userMessage)

    init {
        enableOfflinePersistence()
        startListening()
    }

    private fun enableOfflinePersistence() {
        try {
            val settings = FirebaseFirestoreSettings.Builder(firestore.firestoreSettings)
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
            firestore.firestoreSettings = settings
        } catch (_: Exception) {
            // Firestore settings are already applied if the instance was previously initialized.
        }
    }

    fun startListening() {
        if (listenerRegistration != null) return
        listenerRegistration = sessionsCollection.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val loaded = snapshot.documents
                .mapNotNull { doc -> doc.toStudySessionOrNull() }
                .sortedBy { it.startAtMillis }
            _sessions.value = loaded
        }
    }

    fun stopListening() {
        listenerRegistration?.let { sameRegistration ->
            sameRegistration.remove()
        }
        listenerRegistration = null
    }

    override fun getSession(sessionId: String): StudySession? =
        _sessions.value.firstOrNull { it.sessionId == sessionId }

    override suspend fun createSession(session: StudySession): SessionResult =
        withContext(Dispatchers.IO) {
            try {
                val docRef = sessionsCollection.document()
                val newId = docRef.id
                val coords = resolveCampusCoordinates(
                    locationType = session.locationType,
                    campusName = session.campusName,
                    existingLat = session.latitude,
                    existingLng = session.longitude,
                )
                // Creator is always the first participant per FakeSessionRepository rules
                val initialParticipants = (listOf(session.creatorId) + session.participantIds)
                    .filter { it.isNotBlank() }
                    .distinct()

                val saved = session.copy(
                    sessionId = newId,
                    courseCode = session.courseCode.trim().uppercase(),
                    latitude = coords?.first,
                    longitude = coords?.second,
                    participantIds = initialParticipants,
                )
                val now = Timestamp.now()
                docRef.set(saved.toFirestoreMap(createdAt = now, updatedAt = now)).await()
                _sessions.value = (_sessions.value.filterNot { it.sessionId == newId } + saved)
                    .sortedBy { it.startAtMillis }
                SessionResult.Success(newId)
            } catch (e: Exception) {
                SessionResult.Error(e.localizedMessage ?: SessionErrors.NETWORK)
            }
        }

    override suspend fun updateSession(
        session: StudySession,
        requesterId: String,
    ): SessionResult = withContext(Dispatchers.IO) {
        try {
            val docRef = sessionsCollection.document(session.sessionId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (!snapshot.exists()) {
                    throw RuleViolationException(SessionErrors.NOT_FOUND)
                }
                val existing = snapshot.toStudySessionOrNull()
                    ?: throw RuleViolationException(SessionErrors.NOT_FOUND)

                if (existing.creatorId != requesterId) {
                    throw RuleViolationException(SessionErrors.NOT_CREATOR)
                }
                if (session.maxParticipants < existing.participantIds.size) {
                    throw RuleViolationException(
                        "Maximum participants cannot be less than current attendees (${existing.participantIds.size})."
                    )
                }
                val createdAt = snapshot.getTimestamp(FIELD_CREATED_AT) ?: Timestamp.now()
                val coords = resolveCampusCoordinates(
                    locationType = session.locationType,
                    campusName = session.campusName,
                    existingLat = session.latitude,
                    existingLng = session.longitude,
                )
                val updated = session.copy(
                    sessionId = existing.sessionId,
                    creatorId = existing.creatorId,
                    courseCode = session.courseCode.trim().uppercase(),
                    latitude = coords?.first,
                    longitude = coords?.second,
                    participantIds = existing.participantIds,
                )
                transaction.set(
                    docRef,
                    updated.toFirestoreMap(createdAt = createdAt, updatedAt = Timestamp.now())
                )
            }.await()
            SessionResult.Success(session.sessionId)
        } catch (e: Exception) {
            SessionResult.Error(extractErrorMessage(e))
        }
    }

    override suspend fun deleteSession(
        sessionId: String,
        requesterId: String,
    ): SessionResult = withContext(Dispatchers.IO) {
        try {
            val docRef = sessionsCollection.document(sessionId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (!snapshot.exists()) {
                    throw RuleViolationException(SessionErrors.NOT_FOUND)
                }
                val creatorId = snapshot.getString(FIELD_CREATOR_ID).orEmpty()
                if (creatorId != requesterId) {
                    throw RuleViolationException(SessionErrors.NOT_CREATOR)
                }
                transaction.delete(docRef)
            }.await()
            SessionResult.Success(sessionId)
        } catch (e: Exception) {
            SessionResult.Error(extractErrorMessage(e))
        }
    }

    override suspend fun joinSession(
        sessionId: String,
        userId: String,
    ): SessionResult = withContext(Dispatchers.IO) {
        try {
            val docRef = sessionsCollection.document(sessionId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (!snapshot.exists()) {
                    throw RuleViolationException(SessionErrors.NOT_FOUND)
                }
                val existing = snapshot.toStudySessionOrNull()
                    ?: throw RuleViolationException(SessionErrors.NOT_FOUND)

                when {
                    userId in existing.participantIds ->
                        throw RuleViolationException(SessionErrors.ALREADY_JOINED)
                    existing.isFull ->
                        throw RuleViolationException(SessionErrors.FULL)
                    else -> {
                        val updatedParticipants = existing.participantIds + userId
                        transaction.update(
                            docRef,
                            mapOf(
                                FIELD_PARTICIPANT_IDS to updatedParticipants,
                                FIELD_UPDATED_AT to Timestamp.now(),
                            )
                        )
                    }
                }
            }.await()
            SessionResult.Success(sessionId)
        } catch (e: Exception) {
            SessionResult.Error(extractErrorMessage(e))
        }
    }

    override suspend fun leaveSession(
        sessionId: String,
        userId: String,
    ): SessionResult = withContext(Dispatchers.IO) {
        try {
            val docRef = sessionsCollection.document(sessionId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (!snapshot.exists()) {
                    throw RuleViolationException(SessionErrors.NOT_FOUND)
                }
                val existing = snapshot.toStudySessionOrNull()
                    ?: throw RuleViolationException(SessionErrors.NOT_FOUND)

                when {
                    existing.creatorId == userId ->
                        throw RuleViolationException(SessionErrors.CREATOR_CANNOT_LEAVE)
                    userId !in existing.participantIds ->
                        throw RuleViolationException(SessionErrors.NOT_JOINED)
                    else -> {
                        val updatedParticipants = existing.participantIds - userId
                        transaction.update(
                            docRef,
                            mapOf(
                                FIELD_PARTICIPANT_IDS to updatedParticipants,
                                FIELD_UPDATED_AT to Timestamp.now(),
                            )
                        )
                    }
                }
            }.await()
            SessionResult.Success(sessionId)
        } catch (e: Exception) {
            SessionResult.Error(extractErrorMessage(e))
        }
    }

    private fun extractErrorMessage(e: Exception): String {
        val cause = e.cause
        return when {
            e is RuleViolationException -> e.userMessage
            cause is RuleViolationException -> cause.userMessage
            else -> cause?.localizedMessage ?: e.localizedMessage ?: SessionErrors.NETWORK
        }
    }

    private fun DocumentSnapshot.toStudySessionOrNull(): StudySession? {
        return try {
            val startAtMillis = when (val rawStart = get(FIELD_START_AT)) {
                is Timestamp -> rawStart.toDate().time
                is Number -> rawStart.toLong()
                else -> getLong(FIELD_START_AT_MILLIS) ?: return null
            }
            val locTypeStr = getString(FIELD_LOCATION_TYPE).orEmpty()
            val locationType = if (locTypeStr.equals(LocationType.ONLINE.name, ignoreCase = true)) {
                LocationType.ONLINE
            } else {
                LocationType.ON_CAMPUS
            }
            @Suppress("UNCHECKED_CAST")
            val participants = (get(FIELD_PARTICIPANT_IDS) as? List<*>)
                ?.mapNotNull { it?.toString() }
                ?: emptyList()

            val campus = getString(FIELD_CAMPUS_NAME).orEmpty()
            val coords = resolveCampusCoordinates(
                locationType = locationType,
                campusName = campus,
                existingLat = getDouble(FIELD_LATITUDE),
                existingLng = getDouble(FIELD_LONGITUDE),
            )

            StudySession(
                sessionId = id,
                creatorId = getString(FIELD_CREATOR_ID).orEmpty(),
                organizerName = getString(FIELD_ORGANIZER_NAME).orEmpty(),
                courseCode = getString(FIELD_COURSE_CODE).orEmpty(),
                topic = getString(FIELD_TOPIC).orEmpty(),
                description = getString(FIELD_DESCRIPTION).orEmpty(),
                startAtMillis = startAtMillis,
                locationType = locationType,
                campusName = campus,
                locationLabel = getString(FIELD_LOCATION_LABEL).orEmpty(),
                meetingUrl = getString(FIELD_MEETING_URL).orEmpty(),
                latitude = coords?.first,
                longitude = coords?.second,
                maxParticipants = (getLong(FIELD_MAX_PARTICIPANTS) ?: 2L).toInt(),
                participantIds = participants,
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun StudySession.toFirestoreMap(
        createdAt: Timestamp,
        updatedAt: Timestamp,
    ): Map<String, Any?> = mapOf(
        FIELD_SESSION_ID to sessionId,
        FIELD_CREATOR_ID to creatorId,
        FIELD_ORGANIZER_NAME to organizerName,
        FIELD_COURSE_CODE to courseCode,
        FIELD_TOPIC to topic,
        FIELD_DESCRIPTION to description,
        FIELD_START_AT to Timestamp(Date(startAtMillis)),
        FIELD_START_AT_MILLIS to startAtMillis,
        FIELD_LOCATION_TYPE to locationType.name,
        FIELD_CAMPUS_NAME to campusName,
        FIELD_LOCATION_LABEL to locationLabel,
        FIELD_MEETING_URL to meetingUrl,
        FIELD_LATITUDE to latitude,
        FIELD_LONGITUDE to longitude,
        FIELD_MAX_PARTICIPANTS to maxParticipants,
        FIELD_PARTICIPANT_IDS to participantIds,
        FIELD_CREATED_AT to createdAt,
        FIELD_UPDATED_AT to updatedAt,
    )

    private fun resolveCampusCoordinates(
        locationType: LocationType,
        campusName: String,
        existingLat: Double?,
        existingLng: Double?,
    ): Pair<Double, Double>? {
        if (locationType == LocationType.ONLINE) return null
        if (existingLat != null && existingLng != null) return existingLat to existingLng
        val normalized = campusName.trim().lowercase()
        return when {
            "casa loma" in normalized -> CASA_LOMA_LAT to CASA_LOMA_LNG
            "st. james" in normalized || "st james" in normalized -> ST_JAMES_LAT to ST_JAMES_LNG
            "waterfront" in normalized -> WATERFRONT_LAT to WATERFRONT_LNG
            else -> CASA_LOMA_LAT to CASA_LOMA_LNG
        }
    }

    companion object {
        private const val COLLECTION_SESSIONS = "sessions"
        private const val FIELD_SESSION_ID = "sessionId"
        private const val FIELD_CREATOR_ID = "creatorId"
        private const val FIELD_ORGANIZER_NAME = "organizerName"
        private const val FIELD_COURSE_CODE = "courseCode"
        private const val FIELD_TOPIC = "topic"
        private const val FIELD_DESCRIPTION = "description"
        private const val FIELD_START_AT = "startAt"
        private const val FIELD_START_AT_MILLIS = "startAtMillis"
        private const val FIELD_LOCATION_TYPE = "locationType"
        private const val FIELD_CAMPUS_NAME = "campusName"
        private const val FIELD_LOCATION_LABEL = "locationLabel"
        private const val FIELD_MEETING_URL = "meetingUrl"
        private const val FIELD_LATITUDE = "latitude"
        private const val FIELD_LONGITUDE = "longitude"
        private const val FIELD_MAX_PARTICIPANTS = "maxParticipants"
        private const val FIELD_PARTICIPANT_IDS = "participantIds"
        private const val FIELD_CREATED_AT = "createdAt"
        private const val FIELD_UPDATED_AT = "updatedAt"

        private const val CASA_LOMA_LAT = 43.6761
        private const val CASA_LOMA_LNG = -79.4105
        private const val ST_JAMES_LAT = 43.6511
        private const val ST_JAMES_LNG = -79.3700
        private const val WATERFRONT_LAT = 43.6440
        private const val WATERFRONT_LNG = -79.3666
    }
}