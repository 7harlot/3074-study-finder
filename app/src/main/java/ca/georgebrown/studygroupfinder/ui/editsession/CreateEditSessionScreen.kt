package ca.georgebrown.studygroupfinder.ui.editsession

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import ca.georgebrown.studygroupfinder.data.model.LocationType
import ca.georgebrown.studygroupfinder.data.model.StudySession
import ca.georgebrown.studygroupfinder.data.model.User
import ca.georgebrown.studygroupfinder.data.repository.SessionRepository
import ca.georgebrown.studygroupfinder.data.repository.SessionResult
import ca.georgebrown.studygroupfinder.ui.components.AppTextField
import ca.georgebrown.studygroupfinder.ui.components.AppTopBar
import ca.georgebrown.studygroupfinder.ui.components.PrimaryButton
import ca.georgebrown.studygroupfinder.ui.components.SecondaryButton
import ca.georgebrown.studygroupfinder.ui.components.SegmentedToggle
import ca.georgebrown.studygroupfinder.ui.theme.Dimens
import ca.georgebrown.studygroupfinder.ui.util.formatDateOnly
import ca.georgebrown.studygroupfinder.ui.util.formatTimeOnly
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditSessionScreen(
    existing: StudySession?,
    sessionRepository: SessionRepository,
    currentUser: User?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEdit = existing != null
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val defaultStartMillis = existing?.startAtMillis
        ?: (System.currentTimeMillis() + 24 * 60 * 60 * 1000L)

    var courseCode by rememberSaveable { mutableStateOf(existing?.courseCode.orEmpty()) }
    var topic by rememberSaveable { mutableStateOf(existing?.topic.orEmpty()) }
    var description by rememberSaveable { mutableStateOf(existing?.description.orEmpty()) }
    var startAtMillis by rememberSaveable { mutableLongStateOf(defaultStartMillis) }
    var hasSelectedDateTime by rememberSaveable { mutableStateOf(existing != null) }
    var typeIndex by rememberSaveable { mutableIntStateOf(if (existing?.locationType == LocationType.ONLINE) 1 else 0) }
    var campus by rememberSaveable { mutableStateOf(existing?.campusName?.ifBlank { "Casa Loma" } ?: "Casa Loma") }
    var room by rememberSaveable { mutableStateOf(existing?.locationLabel.orEmpty()) }
    var meetingLink by rememberSaveable { mutableStateOf(existing?.meetingUrl.orEmpty()) }
    var maxParticipants by rememberSaveable { mutableStateOf(existing?.maxParticipants?.toString() ?: "6") }

    var validationError by rememberSaveable { mutableStateOf<String?>(null) }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    val dateText = if (hasSelectedDateTime) formatDateOnly(startAtMillis) else ""
    val timeText = if (hasSelectedDateTime) formatTimeOnly(startAtMillis) else ""

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = if (isEdit) "Edit Session" else "Create Session", onBack = onBack)
        Column(
            modifier = Modifier
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
        ) {
            SectionHeading("Session info")
            AppTextField(
                value = courseCode,
                onValueChange = { courseCode = it.uppercase(); validationError = null },
                label = "Course code",
                placeholder = "COMP3074",
                leadingIcon = Icons.Outlined.Book,
            )
            AppTextField(
                value = topic,
                onValueChange = { topic = it; validationError = null },
                label = "Topic",
                placeholder = "Midterm Exam Review",
                leadingIcon = Icons.Outlined.Title,
            )
            AppTextField(
                value = description,
                onValueChange = { description = it; validationError = null },
                label = "Description",
                placeholder = "Describe the session here",
                leadingIcon = Icons.AutoMirrored.Outlined.Notes,
                singleLine = false,
                minLines = 3,
            )

            SectionHeading("Logistics")
            Box(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                AppTextField(
                    value = dateText,
                    onValueChange = {},
                    label = "Date",
                    placeholder = "Tap to select date",
                    leadingIcon = Icons.Outlined.CalendarMonth,
                    readOnly = true,
                )
            }
            Box(modifier = Modifier.fillMaxWidth().clickable { showTimePicker = true }) {
                AppTextField(
                    value = timeText,
                    onValueChange = {},
                    label = "Time",
                    placeholder = "Tap to select time",
                    leadingIcon = Icons.Outlined.AccessTime,
                    readOnly = true,
                )
            }

            SegmentedToggle(
                options = listOf("On campus", "Online"),
                selectedIndex = typeIndex,
                onSelect = { typeIndex = it; validationError = null },
            )
            if (typeIndex == 0) {
                AppTextField(
                    value = campus,
                    onValueChange = { campus = it; validationError = null },
                    label = "Campus",
                    placeholder = "Casa Loma / St. James / Waterfront",
                    leadingIcon = Icons.Outlined.Place,
                )
                AppTextField(
                    value = room,
                    onValueChange = { room = it; validationError = null },
                    label = "Room",
                    placeholder = "Campus library, Study room 2",
                    leadingIcon = Icons.Outlined.MeetingRoom,
                )
            } else {
                AppTextField(
                    value = meetingLink,
                    onValueChange = { meetingLink = it; validationError = null },
                    label = "Meeting link",
                    placeholder = "https://meet.google.com/...",
                    leadingIcon = Icons.Outlined.Link,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
            }

            SectionHeading("Final details")
            AppTextField(
                value = maxParticipants,
                onValueChange = { new ->
                    maxParticipants = new.filter { it.isDigit() }.take(2)
                    validationError = null
                },
                label = "Maximum participants (2–20)",
                leadingIcon = Icons.Outlined.Groups,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            if (validationError != null) {
                Text(
                    text = validationError.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
                modifier = Modifier.padding(vertical = Dimens.ItemSpacing),
            ) {
                SecondaryButton(
                    text = "Cancel",
                    onClick = onBack,
                    icon = Icons.Outlined.Close,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = if (isSaving) "Saving..." else if (isEdit) "Save" else "Create",
                    enabled = !isSaving,
                    icon = Icons.Outlined.Check,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val error = validateSessionInput(
                            courseCode = courseCode,
                            topic = topic,
                            hasSelectedDateTime = hasSelectedDateTime,
                            startAtMillis = startAtMillis,
                            nowMillis = System.currentTimeMillis(),
                            isOnline = typeIndex == 1,
                            campus = campus,
                            room = room,
                            meetingLink = meetingLink,
                            maxParticipantsStr = maxParticipants,
                            currentParticipantCount = existing?.participantIds?.size ?: 1,
                        )
                        if (error != null) {
                            validationError = error
                            return@PrimaryButton
                        }
                        val user = currentUser
                        if (user == null) {
                            validationError = "You must be signed in to save a session."
                            return@PrimaryButton
                        }

                        val maxSeats = maxParticipants.toIntOrNull() ?: 2
                        val locationType = if (typeIndex == 1) LocationType.ONLINE else LocationType.ON_CAMPUS
                        val sessionToSave = StudySession(
                            sessionId = existing?.sessionId.orEmpty(),
                            creatorId = existing?.creatorId ?: user.userId,
                            organizerName = existing?.organizerName ?: user.name.ifBlank { user.email },
                            courseCode = courseCode.trim().uppercase(),
                            topic = topic.trim(),
                            description = description.trim(),
                            startAtMillis = startAtMillis,
                            locationType = locationType,
                            campusName = if (locationType == LocationType.ON_CAMPUS) campus.trim() else "",
                            locationLabel = if (locationType == LocationType.ON_CAMPUS) room.trim() else "",
                            meetingUrl = if (locationType == LocationType.ONLINE) meetingLink.trim() else "",
                            latitude = existing?.latitude,
                            longitude = existing?.longitude,
                            maxParticipants = maxSeats,
                            participantIds = existing?.participantIds ?: listOf(user.userId),
                        )

                        isSaving = true
                        validationError = null
                        scope.launch {
                            val result = if (isEdit) {
                                sessionRepository.updateSession(sessionToSave, requesterId = user.userId)
                            } else {
                                sessionRepository.createSession(sessionToSave)
                            }
                            isSaving = false
                            when (result) {
                                is SessionResult.Success -> {
                                    val msg = if (isEdit) "Session updated" else "Session created"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                                is SessionResult.Error -> validationError = result.message
                            }
                        }
                    },
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startAtMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedUtcMillis = datePickerState.selectedDateMillis
                        if (selectedUtcMillis != null) {
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = selectedUtcMillis
                            }
                            val localCal = Calendar.getInstance().apply {
                                timeInMillis = startAtMillis
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                            }
                            startAtMillis = localCal.timeInMillis
                            hasSelectedDateTime = true
                            validationError = null
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = startAtMillis }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select session time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updatedCal = Calendar.getInstance().apply {
                            timeInMillis = startAtMillis
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        startAtMillis = updatedCal.timeInMillis
                        hasSelectedDateTime = true
                        validationError = null
                        showTimePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
        )
    }
}

internal fun validateSessionInput(
    courseCode: String,
    topic: String,
    hasSelectedDateTime: Boolean,
    startAtMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
    isOnline: Boolean,
    campus: String,
    room: String,
    meetingLink: String,
    maxParticipantsStr: String,
    currentParticipantCount: Int = 1,
): String? {
    if (courseCode.isBlank()) return "Course code is required (e.g., COMP3074)."
    if (topic.isBlank()) return "Session topic is required."
    if (!hasSelectedDateTime) return "Please select both a date and time."
    if (startAtMillis <= nowMillis) return "Session date and time must be in the future."

    if (isOnline) {
        val link = meetingLink.trim()
        if (link.isBlank() || (!link.startsWith("https://") && !link.startsWith("http://"))) {
            return "Online sessions require a valid meeting link starting with https:// or http://."
        }
    } else {
        if (campus.isBlank()) return "Campus name is required for on-campus sessions."
        if (room.isBlank()) return "Room or building location is required for on-campus sessions."
    }

    val maxSeats = maxParticipantsStr.toIntOrNull()
        ?: return "Please enter a valid number for maximum participants (2–20)."
    if (maxSeats !in 2..20) {
        return "Maximum participants must be between 2 and 20."
    }
    if (maxSeats < currentParticipantCount) {
        return "Maximum participants cannot be less than the people already joined ($currentParticipantCount)."
    }
    return null
}

@Composable
private fun SectionHeading(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = Dimens.ItemSpacing))
}