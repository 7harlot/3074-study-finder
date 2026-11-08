package ca.georgebrown.studygroupfinder.ui.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.georgebrown.studygroupfinder.data.repository.SessionRepository
import ca.georgebrown.studygroupfinder.ui.components.AppTextField
import ca.georgebrown.studygroupfinder.ui.components.AppTopBar
import ca.georgebrown.studygroupfinder.ui.components.PrimaryButton
import ca.georgebrown.studygroupfinder.ui.components.SessionCard
import ca.georgebrown.studygroupfinder.ui.theme.Dimens
import ca.georgebrown.studygroupfinder.ui.util.formatDateOnly
import java.util.Calendar
import java.util.TimeZone


/**
 * Screen 3: Session Feed (Home). Prototype 1: lists sample sessions with a simple text filter.
 * Munir replaces the filter and data source with the Firestore-backed ViewModel (FR-05, FR-06, FR-15).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    sessionRepository: SessionRepository,
    onOpenSession: (String) -> Unit,
    onCreateSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sessions by sessionRepository.sessions.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var selectedDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val now = System.currentTimeMillis() - (60 * 60 * 1000L)
    val visible = sessions
        .filter { it.startAtMillis >= now }
        .filter {
            query.isBlank() ||
                    it.courseCode.contains(query.trim(), ignoreCase = true) ||
                    it.topic.contains(query.trim(), ignoreCase = true)
        }
        .filter { session ->
            val filterDate = selectedDateMillis ?: return@filter true
            isSameLocalDay(session.startAtMillis, filterDate)
        }
        .sortedBy { it.startAtMillis }

    Column(modifier = modifier.fillMaxSize()) {
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showDatePicker = true }
                ) {
                    AppTextField(
                        value = selectedDateMillis?.let { formatDateOnly(it) }.orEmpty(),
                        onValueChange = {},
                        label = "Date filter (optional)",
                        placeholder = "Tap to filter by date",
                        leadingIcon = Icons.Outlined.CalendarMonth,
                        readOnly = true,
                    )
                }
                if (selectedDateMillis != null) {
                    IconButton(onClick = { selectedDateMillis = null }) {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = "Clear date filter",
                        )
                    }
                }
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            if (visible.isEmpty()) {
                item {
                    val emptyMessage = when {
                        query.isNotBlank() && selectedDateMillis != null ->
                            "No sessions match \"$query\" on ${formatDateOnly(selectedDateMillis!!)}."
                        query.isNotBlank() ->
                            "No sessions match \"$query\"."
                        selectedDateMillis != null ->
                            "No sessions scheduled on ${formatDateOnly(selectedDateMillis!!)}."
                        else ->
                            "No upcoming study sessions right now. Tap + Create to host one!"
                    }
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(items = visible, key = { it.sessionId }) { session ->
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

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val utcMillis = pickerState.selectedDateMillis
                        if (utcMillis != null) {
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = utcMillis
                            }
                            val localCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                                set(Calendar.HOUR_OF_DAY, 12)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            selectedDateMillis = localCal.timeInMillis
                        }
                        showDatePicker = false
                    }
                ) { Text("Filter") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

internal fun isSameLocalDay(millisA: Long, millisB: Long): Boolean {
    val calA = Calendar.getInstance().apply { timeInMillis = millisA }
    val calB = Calendar.getInstance().apply { timeInMillis = millisB }
    return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
            calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
}
