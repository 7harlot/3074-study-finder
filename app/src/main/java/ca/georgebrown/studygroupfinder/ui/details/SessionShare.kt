package ca.georgebrown.studygroupfinder.ui.details

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import ca.georgebrown.studygroupfinder.data.model.LocationType
import ca.georgebrown.studygroupfinder.data.model.StudySession
import ca.georgebrown.studygroupfinder.ui.components.seatsText
import ca.georgebrown.studygroupfinder.ui.map.directionsLabel
import ca.georgebrown.studygroupfinder.ui.map.geoPoint
import java.util.Locale

/** Subject + body for sharing a session. Plain text so it reads well in email, SMS or chat apps. */
data class ShareContent(val subject: String, val body: String)

/**
 * Builds the share message for a session (spec item 4, FR-17).
 * [whenText] is passed in already formatted, so this stays a pure function we can unit test.
 */
fun buildSessionShare(session: StudySession, whenText: String): ShareContent {
    val subject = "Study session: ${session.courseCode} - ${session.topic}"
    val where = when (session.locationType) {
        LocationType.ONLINE -> buildString {
            append("Online")
            if (session.meetingUrl.isNotBlank()) append(" - ${session.meetingUrl}")
        }
        LocationType.ON_CAMPUS -> session.directionsLabel.ifBlank { "On campus (room to be confirmed)" }
    }
    val body = buildString {
        appendLine("Join me for a study session!")
        appendLine()
        appendLine("Course: ${session.courseCode}")
        appendLine("Topic: ${session.topic}")
        appendLine("When: $whenText")
        appendLine("Where: $where")
        // A tappable map link for campus sessions that have a pin.
        session.geoPoint?.let {
            appendLine("Map: https://www.google.com/maps/search/?api=1&query=${String.format(Locale.US, "%.6f,%.6f", it.latitude, it.longitude)}")
        }
        appendLine("Organizer: ${session.organizerName}")
        appendLine("Seats: ${seatsText(session)}")
        if (session.description.isNotBlank()) {
            appendLine()
            appendLine(session.description.trim())
        }
        appendLine()
        append("Shared from Campus Study Group Finder")
    }
    return ShareContent(subject, body)
}

/**
 * Opens the system share sheet. Email apps use the subject; SMS/chat apps use the body.
 * ACTION_SEND (not mailto:) so Gmail, Messages, Facebook and X all show up, which also covers the spec's bonus options.
 */
fun Context.shareSession(content: ShareContent) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, content.subject)
        putExtra(Intent.EXTRA_TEXT, content.body)
    }
    try {
        startActivity(Intent.createChooser(send, "Share session"))
    } catch (error: ActivityNotFoundException) {
        Toast.makeText(this, "No app available to share with.", Toast.LENGTH_SHORT).show()
    }
}
