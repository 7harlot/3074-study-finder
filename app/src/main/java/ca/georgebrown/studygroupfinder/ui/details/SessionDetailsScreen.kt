package ca.georgebrown.studygroupfinder.ui.details

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.georgebrown.studygroupfinder.data.model.LocationType
import ca.georgebrown.studygroupfinder.data.model.StudySession
import ca.georgebrown.studygroupfinder.data.repository.SessionRepository
import ca.georgebrown.studygroupfinder.data.repository.SessionResult
import ca.georgebrown.studygroupfinder.ui.components.AppTopBar
import ca.georgebrown.studygroupfinder.ui.components.InfoRow
import ca.georgebrown.studygroupfinder.ui.components.MapPlaceholder
import ca.georgebrown.studygroupfinder.ui.components.MissingContent
import ca.georgebrown.studygroupfinder.ui.components.PrimaryButton
import ca.georgebrown.studygroupfinder.ui.components.SecondaryButton
import ca.georgebrown.studygroupfinder.ui.components.SectionCard
import ca.georgebrown.studygroupfinder.ui.components.seatsText
import ca.georgebrown.studygroupfinder.ui.theme.Dimens
import ca.georgebrown.studygroupfinder.ui.util.formatSessionDateLong
import kotlinx.coroutines.launch


/**
 * Screen 4: Session Details. Prototype 1 shows real sample data; Join/Leave only toggles local UI state
 * and Delete only shows the confirm dialog. Firestore transactions come in the Session Details sprint (FR-08, FR-09).
 */
@Composable
fun SessionDetailsScreen(
    sessionId: String,
    sessionRepository: SessionRepository,
    currentUserId: String?,
    onBack: () -> Unit,
    onViewMap: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sessions by sessionRepository.sessions.collectAsStateWithLifecycle()
    val session = sessions.firstOrNull { it.sessionId == sessionId } ?: sessionRepository.getSession(sessionId)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var isProcessing by rememberSaveable { mutableStateOf(false) }
    var actionError by rememberSaveable { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = "Session Details", onBack = onBack)
        if (session == null) {
            MissingContent("This session no longer exists.")
            return@Column
        }
        val isCreator = currentUserId != null && session.creatorId == currentUserId
        val joined = currentUserId != null && currentUserId in session.participantIds

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SmallSpacing),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            SectionCard {
                Text("${session.courseCode} - ${session.topic}", style = MaterialTheme.typography.titleMedium)
                InfoRow(Icons.Outlined.CalendarMonth, formatSessionDateLong(session.startAtMillis))
                InfoRow(
                    if (session.locationType == LocationType.ONLINE) Icons.Outlined.Laptop else Icons.Outlined.Place,
                    session.whereLabel,
                )
                InfoRow(Icons.Filled.Groups, seatsText(session))
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                LabeledLine("Topic", session.topic)
                LabeledLine("Organizer", session.organizerName)
                if (session.locationType == LocationType.ON_CAMPUS) LabeledLine("Campus", session.campusName)
                LabeledLine("Description", session.description)
            }

            if (session.locationType == LocationType.ON_CAMPUS) {
                MapPlaceholder(
                    destinationLabel = session.locationLabel,
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                )
                SecondaryButton(
                    text = "View Map",
                    onClick = onViewMap,
                    icon = Icons.Outlined.Map,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                SecondaryButton(
                    text = "Open Meeting",
                    onClick = {
                        if (session.meetingUrl.isNotBlank()) {
                            val webPage = Uri.parse(session.meetingUrl.trim())
                            startIfAvailable(context, Intent(Intent.ACTION_VIEW, webPage))
                        } else {
                            Toast.makeText(context, "No meeting link provided", Toast.LENGTH_SHORT).show()
                        }
                    },
                    icon = Icons.Outlined.Videocam,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (actionError != null) {
                Text(
                    text = actionError.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (!isCreator) {
                PrimaryButton(
                    text = when {
                        isProcessing -> "Updating..."
                        joined -> "Leave session"
                        session.isFull -> "Session full"
                        else -> "Join session"
                    },
                    enabled = !isProcessing && (joined || !session.isFull),
                    onClick = {
                        val uid = currentUserId
                        if (uid.isNullOrBlank()) {
                            actionError = "Please sign in to join or leave a session."
                            return@PrimaryButton
                        }
                        isProcessing = true
                        actionError = null
                        scope.launch {
                            val wasJoining = !joined
                            val result = if (joined) {
                                sessionRepository.leaveSession(session.sessionId, uid)
                            } else {
                                sessionRepository.joinSession(session.sessionId, uid)
                            }
                            isProcessing = false
                            when (result) {
                                is SessionResult.Success -> {
                                    val msg = if (wasJoining) "Joined study session" else "Left study session"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                                is SessionResult.Error -> {
                                    actionError = result.message
                                }
                            }
                        }
                    },
                )
            }

            if (isCreator) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                ) {
                    SecondaryButton(
                        text = "Edit",
                        onClick = onEdit,
                        icon = Icons.Outlined.Edit,
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton(
                        text = "Delete",
                        onClick = { showDeleteDialog = true },
                        icon = Icons.Outlined.Delete,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            SecondaryButton(
                text = "Share",
                onClick = { shareSessionByEmail(context, session) },
                icon = Icons.Outlined.Share,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete session?") },
                text = { Text("Are you sure you want to permanently delete this study session?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val uid = currentUserId.orEmpty()
                            showDeleteDialog = false
                            scope.launch {
                                when (val res = sessionRepository.deleteSession(session.sessionId, uid)) {
                                    is SessionResult.Success -> {
                                        Toast.makeText(context, "Session deleted", Toast.LENGTH_LONG).show()
                                        onBack()
                                    }
                                    is SessionResult.Error -> actionError = res.message
                                }
                            }
                        }
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                },
            )
        }
    }
}

private fun startIfAvailable(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app available for this action", Toast.LENGTH_SHORT).show()
    }
}

private fun shareSessionByEmail(context: Context, session: StudySession) {
    val locationDetails = if (session.locationType == LocationType.ONLINE) {
        "Online (${session.meetingUrl})"
    } else {
        "${session.campusName} - ${session.locationLabel}"
    }
    val subject = "Study Session: ${session.courseCode} - ${session.topic}"
    val body = buildString {
        appendLine("Join our George Brown College study session!")
        appendLine()
        appendLine("Course: ${session.courseCode}")
        appendLine("Topic: ${session.topic}")
        appendLine("When: ${formatSessionDateLong(session.startAtMillis)}")
        appendLine("Where: $locationDetails")
        appendLine("Organizer: ${session.organizerName}")
        appendLine("Seats Available: ${session.seatsAvailable} of ${session.maxParticipants}")
        if (session.description.isNotBlank()) {
            appendLine()
            appendLine("Details: ${session.description}")
        }
    }
    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try {
        context.startActivity(emailIntent)
    } catch (_: ActivityNotFoundException) {
        val fallbackShare = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        startIfAvailable(context, Intent.createChooser(fallbackShare, "Share study session"))
    }
}

@Composable
private fun LabeledLine(label: String, value: String) {
    val text = buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append("$label: ") }
        append(value)
    }
    Text(text, style = MaterialTheme.typography.bodyMedium)
}
