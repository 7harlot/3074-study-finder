package ca.georgebrown.studygroupfinder.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.georgebrown.studygroupfinder.data.repository.SessionRepository
import ca.georgebrown.studygroupfinder.ui.components.AppTextField
import ca.georgebrown.studygroupfinder.ui.components.AppTopBar
import ca.georgebrown.studygroupfinder.ui.components.PrimaryButton
import ca.georgebrown.studygroupfinder.ui.components.SessionCard
import ca.georgebrown.studygroupfinder.ui.theme.Dimens

/**
 * Screen 3: Session Feed (Home). Prototype 1: lists sample sessions with a simple text filter.
 * Munir replaces the filter and data source with the Firestore-backed ViewModel (FR-05, FR-06, FR-15).
 */
@Composable
fun FeedScreen(
    sessionRepository: SessionRepository,
    onOpenSession: (String) -> Unit,
    onCreateSession: () -> Unit,
) {
    val sessions by sessionRepository.sessions.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val visible = sessions
        .filter { query.isBlank() || it.courseCode.contains(query, true) || it.topic.contains(query, true) }
        .sortedBy { it.startAtMillis }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "Study Sessions")
        Column(
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
        ) {
            AppTextField(
                value = query,
                onValueChange = { query = it },
                label = "Search course code or topic",
                leadingIcon = Icons.Outlined.Search,
            )
            AppTextField(
                value = "",
                onValueChange = {},
                label = "Date filter (optional)",
                leadingIcon = Icons.Outlined.CalendarMonth,
                readOnly = true,
                supportingText = "Date filtering arrives in a later sprint",
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            if (visible.isEmpty()) {
                item {
                    Text(
                        "No sessions match \"$query\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(visible, key = { it.sessionId }) { session ->
                SessionCard(session = session, onView = { onOpenSession(session.sessionId) })
            }
        }
        PrimaryButton(
            text = "Create",
            icon = Icons.Filled.Add,
            onClick = onCreateSession,
            modifier = Modifier.padding(Dimens.ScreenPadding),
        )
    }
}
