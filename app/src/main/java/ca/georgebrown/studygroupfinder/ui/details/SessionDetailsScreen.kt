package ca.georgebrown.studygroupfinder.ui.details

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import ca.georgebrown.studygroupfinder.data.model.LocationType
import ca.georgebrown.studygroupfinder.data.model.StudySession
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

/**
 * Screen 4: Session Details. Prototype 1 shows real sample data; Join/Leave only toggles local UI state
 * and Delete only shows the confirm dialog. Firestore transactions come in the Session Details sprint (FR-08, FR-09).
 */
@Composable
fun SessionDetailsScreen(
    session: StudySession?,
    currentUserId: String?,
    onBack: () -> Unit,
    onViewMap: () -> Unit,
    onEdit: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "Session Details", onBack = onBack)
        if (session == null) {
            MissingContent("This session no longer exists.")
            return@Column
        }
        val isCreator = session.creatorId == currentUserId
        val joinedInitially = currentUserId in session.participantIds
        var joined by rememberSaveable(session.sessionId) { mutableStateOf(joinedInitially) }
        // Local-only seat math so Join/Leave visibly updates the count in the prototype.
        val shownSession = when {
            joined == joinedInitially -> session
            joined -> session.copy(participantIds = session.participantIds + currentUserId.orEmpty())
            else -> session.copy(participantIds = session.participantIds - currentUserId.orEmpty())
        }
        var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

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
                InfoRow(Icons.Filled.Groups, seatsText(shownSession))
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
                SecondaryButton("View Map", onClick = onViewMap, icon = Icons.Outlined.Map, modifier = Modifier.fillMaxWidth())
            } else {
                SecondaryButton("Open Meeting", onClick = {}, icon = Icons.Outlined.Videocam, modifier = Modifier.fillMaxWidth())
            }

            PrimaryButton(
                text = when {
                    joined -> "Leave session"
                    shownSession.isFull -> "Session full"
                    else -> "Join session"
                },
                enabled = joined || !shownSession.isFull, // FR-09: can't join a full session
                onClick = { joined = !joined },
            )

            if (isCreator) {
                Text("Creator actions", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing)) {
                    SecondaryButton("Edit", onClick = onEdit, icon = Icons.Outlined.Edit, modifier = Modifier.weight(1f))
                    SecondaryButton("Delete", onClick = { showDeleteDialog = true }, icon = Icons.Outlined.Delete, modifier = Modifier.weight(1f))
                }
            }
            SecondaryButton("Share", onClick = {}, icon = Icons.Outlined.Share, modifier = Modifier.fillMaxWidth())
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete session?") },
                text = { Text("Everyone who joined will lose this session. This can't be undone.") },
                confirmButton = { TextButton(onClick = { showDeleteDialog = false; onBack() }) { Text("Delete") } },
                dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } },
            )
        }
    }
}

@Composable
private fun LabeledLine(label: String, value: String) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append("$label: ") }
            append(value)
        },
        style = MaterialTheme.typography.bodyMedium,
    )
}
