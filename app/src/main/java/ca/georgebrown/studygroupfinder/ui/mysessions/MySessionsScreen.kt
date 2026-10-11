package ca.georgebrown.studygroupfinder.ui.mysessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.georgebrown.studygroupfinder.data.repository.SessionRepository
import ca.georgebrown.studygroupfinder.ui.components.AppTopBar
import ca.georgebrown.studygroupfinder.ui.components.MissingContent
import ca.georgebrown.studygroupfinder.ui.components.SegmentedToggle
import ca.georgebrown.studygroupfinder.ui.components.SessionCard
import ca.georgebrown.studygroupfinder.ui.theme.Dimens

/** Screen 7: My Sessions with Joined / Created tabs and an empty state (FR-10). */
@Composable
fun MySessionsScreen(
    sessionRepository: SessionRepository,
    currentUserId: String?,
    onOpenSession: (String) -> Unit,
) {
    val sessions by sessionRepository.sessions.collectAsStateWithLifecycle()
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    val showingCreated = tabIndex == 1
    val visible = sessions
        .filter { if (showingCreated) it.creatorId == currentUserId else currentUserId in it.participantIds }
        .sortedBy { it.startAtMillis }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "My Sessions")
        SegmentedToggle(
            options = listOf("Joined", "Created"),
            selectedIndex = tabIndex,
            onSelect = { tabIndex = it },
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
        )
        if (visible.isEmpty()) {
            MissingContent(
                if (showingCreated) "You haven't created any sessions yet. Tap Create on Home to start one."
                else "You haven't joined any sessions yet. Browse Home to find one.",
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(Dimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            ) {
                items(visible, key = { it.sessionId }) { session ->
                    SessionCard(session = session, onView = { onOpenSession(session.sessionId) }, showSeats = false)
                }
            }
        }
    }
}
