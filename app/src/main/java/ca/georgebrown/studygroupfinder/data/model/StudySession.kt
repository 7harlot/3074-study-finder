package ca.georgebrown.studygroupfinder.data.model

/** Where a session happens. Stored as a string ("ON_CAMPUS" / "ONLINE") in Firestore. */
enum class LocationType { ON_CAMPUS, ONLINE }

/**
 * One study session. Mirrors the "StudySession" entity in the design proposal (Section 7),
 * including the latitude/longitude the map screens need.
 */
data class StudySession(
    val sessionId: String,
    val creatorId: String,
    val organizerName: String,
    val courseCode: String,
    val topic: String,
    val description: String,
    val startAtMillis: Long,
    val locationType: LocationType,
    val campusName: String = "",
    val locationLabel: String = "",
    val meetingUrl: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val maxParticipants: Int,
    val participantIds: List<String> = emptyList(),
) {
    /** Seats left, never negative even if data is inconsistent. */
    val seatsAvailable: Int get() = (maxParticipants - participantIds.size).coerceAtLeast(0)

    val isFull: Boolean get() = seatsAvailable == 0

    /** Short "where" label used on cards: the room for campus sessions, "Online" otherwise. */
    val whereLabel: String
        get() = if (locationType == LocationType.ONLINE) "Online" else locationLabel.ifBlank { campusName }
}
