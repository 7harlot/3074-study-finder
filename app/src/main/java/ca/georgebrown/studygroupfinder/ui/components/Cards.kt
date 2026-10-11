package ca.georgebrown.studygroupfinder.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ca.georgebrown.studygroupfinder.data.model.LocationType
import ca.georgebrown.studygroupfinder.data.model.StudySession
import ca.georgebrown.studygroupfinder.ui.theme.Dimens
import ca.georgebrown.studygroupfinder.ui.util.formatSessionTime

/** Outlined dark card used to group content on every screen. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier.padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
            content = content,
        )
    }
}

/** Icon + text on one line, e.g. the date or seat count on a session card. */
@Composable
fun InfoRow(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * The session summary card shared by Feed and My Sessions, so both lists always look identical.
 * [showSeats] is off in My Sessions, matching the mockup.
 */
@Composable
fun SessionCard(
    session: StudySession,
    onView: () -> Unit,
    modifier: Modifier = Modifier,
    showSeats: Boolean = true,
) {
    SectionCard(modifier) {
        Text(
            "${session.courseCode} - ${session.topic}",
            style = MaterialTheme.typography.titleMedium,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            InfoRow(Icons.Outlined.CalendarMonth, formatSessionTime(session.startAtMillis))
            Text("  |  ", color = MaterialTheme.colorScheme.outline)
            InfoRow(
                if (session.locationType == LocationType.ONLINE) Icons.Outlined.Laptop else Icons.Outlined.Place,
                session.whereLabel,
            )
        }
        if (showSeats) {
            InfoRow(Icons.Filled.Groups, seatsText(session))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            SecondaryButton(text = "View", onClick = onView)
        }
    }
}

fun seatsText(session: StudySession): String = when {
    session.isFull -> "Full"
    session.seatsAvailable == 1 -> "1 seat available"
    else -> "${session.seatsAvailable} seats available"
}
