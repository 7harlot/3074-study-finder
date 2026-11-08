package ca.georgebrown.studygroupfinder.ui.mysessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.georgebrown.studygroupfinder.ui.components.AppTopBar
import ca.georgebrown.studygroupfinder.ui.components.MissingContent
import ca.georgebrown.studygroupfinder.ui.components.SegmentedToggle
import ca.georgebrown.studygroupfinder.ui.components.SessionCard
import ca.georgebrown.studygroupfinder.ui.theme.Dimens

/** Screen 7: My Sessions with Joined / Created tabs and an empty state (FR-10). Data comes from [MySessionsViewModel]. */
@Composable
fun MySessionsScreen(
    viewModel: MySessionsViewModel,
    onOpenSession: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val showingCreated = state.tab == MySessionsTab.CREATED

    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "My Sessions")
        SegmentedToggle(
            options = listOf("Joined (${state.joined.size})", "Created (${state.created.size})"),
            selectedIndex = state.tab.ordinal,
            onSelect = { viewModel.selectTab(MySessionsTab.entries[it]) },
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
        )
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.errorMessage != null -> Text(
                state.errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(Dimens.ScreenPadding),
            )
            state.visible.isEmpty() -> MissingContent(
                if (showingCreated) "You haven't created any sessions yet. Tap Create on Home to start one."
                else "You haven't joined any sessions yet. Browse Home to find one.",
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(Dimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            ) {
                items(state.visible, key = { it.sessionId }) { session ->
                    SessionCard(session = session, onView = { onOpenSession(session.sessionId) }, showSeats = false)
                }
            }
        }
    }
}
