package ca.georgebrown.studygroupfinder.ui.editsession

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import ca.georgebrown.studygroupfinder.data.model.LocationType
import ca.georgebrown.studygroupfinder.data.model.StudySession
import ca.georgebrown.studygroupfinder.ui.components.AppTextField
import ca.georgebrown.studygroupfinder.ui.components.AppTopBar
import ca.georgebrown.studygroupfinder.ui.components.PrimaryButton
import ca.georgebrown.studygroupfinder.ui.components.SecondaryButton
import ca.georgebrown.studygroupfinder.ui.components.SegmentedToggle
import ca.georgebrown.studygroupfinder.ui.theme.Dimens
import ca.georgebrown.studygroupfinder.ui.util.formatDateOnly
import ca.georgebrown.studygroupfinder.ui.util.formatTimeOnly

/**
 * Screen 5: Create / Edit Session. One screen, two modes: [existing] null = Create, otherwise Edit (pre-filled).
 * Prototype 1 is UI only: Save just returns. Validation and saving are Zuheib's Session Creation sprint (FR-03, FR-04).
 */
@Composable
fun CreateEditSessionScreen(
    existing: StudySession?,
    onBack: () -> Unit,
) {
    val isEdit = existing != null
    var courseCode by rememberSaveable { mutableStateOf(existing?.courseCode.orEmpty()) }
    var topic by rememberSaveable { mutableStateOf(existing?.topic.orEmpty()) }
    var description by rememberSaveable { mutableStateOf(existing?.description.orEmpty()) }
    var typeIndex by rememberSaveable { mutableIntStateOf(if (existing?.locationType == LocationType.ONLINE) 1 else 0) }
    var campus by rememberSaveable { mutableStateOf(existing?.campusName.orEmpty()) }
    var room by rememberSaveable { mutableStateOf(existing?.locationLabel.orEmpty()) }
    var meetingLink by rememberSaveable { mutableStateOf(existing?.meetingUrl.orEmpty()) }
    var maxParticipants by rememberSaveable { mutableStateOf(existing?.maxParticipants?.toString().orEmpty()) }
    val dateText = existing?.let { formatDateOnly(it.startAtMillis) }.orEmpty()
    val timeText = existing?.let { formatTimeOnly(it.startAtMillis) }.orEmpty()

    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = if (isEdit) "Edit Session" else "Create Session", onBack = onBack)
        Column(
            modifier = Modifier
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
        ) {
            SectionHeading("Session info")
            AppTextField(courseCode, { courseCode = it.uppercase() }, "Course code", placeholder = "COMP3074", leadingIcon = Icons.Outlined.Book)
            AppTextField(topic, { topic = it }, "Topic", leadingIcon = Icons.Outlined.Title)
            AppTextField(
                description, { description = it }, "Description",
                placeholder = "Describe the session here", leadingIcon = Icons.AutoMirrored.Outlined.Notes,
                singleLine = false, minLines = 3,
            )

            SectionHeading("Logistics")
            AppTextField(dateText, {}, "Date", placeholder = "Select date", leadingIcon = Icons.Outlined.CalendarMonth, readOnly = true)
            AppTextField(timeText, {}, "Time", placeholder = "Select time", leadingIcon = Icons.Outlined.AccessTime, readOnly = true)
            SegmentedToggle(listOf("On campus", "Online"), typeIndex, { typeIndex = it })
            if (typeIndex == 0) {
                AppTextField(campus, { campus = it }, "Campus", placeholder = "Select campus", leadingIcon = Icons.Outlined.Place)
                AppTextField(room, { room = it }, "Room", placeholder = "Enter room", leadingIcon = Icons.Outlined.MeetingRoom)
            } else {
                AppTextField(
                    meetingLink, { meetingLink = it }, "Meeting link", placeholder = "https://", leadingIcon = Icons.Outlined.Link,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
            }

            SectionHeading("Final details")
            AppTextField(
                maxParticipants, { new -> maxParticipants = new.filter { it.isDigit() }.take(3) }, "Maximum participants",
                leadingIcon = Icons.Outlined.Groups, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing), modifier = Modifier.padding(vertical = Dimens.ItemSpacing)) {
                SecondaryButton("Cancel", onClick = onBack, icon = Icons.Outlined.Close, modifier = Modifier.weight(1f))
                PrimaryButton(if (isEdit) "Save" else "Create", onClick = onBack, icon = Icons.Outlined.Check, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = Dimens.ItemSpacing))
}
