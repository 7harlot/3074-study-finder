package ca.georgebrown.studygroupfinder.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.georgebrown.studygroupfinder.ui.components.AppTextField
import ca.georgebrown.studygroupfinder.ui.components.AppTopBar
import ca.georgebrown.studygroupfinder.ui.components.MissingContent
import ca.georgebrown.studygroupfinder.ui.components.PrimaryButton
import ca.georgebrown.studygroupfinder.ui.components.SecondaryButton
import ca.georgebrown.studygroupfinder.ui.theme.Dimens

/**
 * Screen 10: Edit Profile (FR-16). Name, program and course chips; email is read-only because it is the login.
 * All rules and saving live in [ProfileViewModel]; this file only draws its state.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.editState.collectAsStateWithLifecycle()

    // Leave the screen once the save has really finished (not before, or a failed save would be lost).
    LaunchedEffect(state.isSaved) { if (state.isSaved) onBack() }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "Edit Profile", onBack = onBack)
        if (state.isSignedOut) {
            MissingContent("You're signed out. Sign in again to edit your profile.")
            return@Column
        }

        Column(
            modifier = Modifier.imePadding().verticalScroll(rememberScrollState()).padding(horizontal = Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
        ) {
            AppTextField(state.name, viewModel::onNameChange, "Name", leadingIcon = Icons.Outlined.Badge, errorText = state.nameError)
            AppTextField(state.email, {}, "Email", leadingIcon = Icons.Outlined.Email, readOnly = true, enabled = false,
                supportingText = "Your email is your login and can't be changed here.")
            AppTextField(state.program, viewModel::onProgramChange, "Program", placeholder = "Computer Programming",
                leadingIcon = Icons.Outlined.School, errorText = state.programError)

            Text("Enrolled courses", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = Dimens.ItemSpacing))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing)) {
                state.courses.forEach { course ->
                    InputChip(
                        selected = false,
                        onClick = { viewModel.removeCourse(course) },
                        label = { Text(course) },
                        trailingIcon = { Icon(Icons.Outlined.Close, contentDescription = "Remove $course") },
                    )
                }
            }
            Row {
                AppTextField(
                    state.newCourse, viewModel::onNewCourseChange, "Add course code",
                    placeholder = "COMP3074", errorText = state.courseError, modifier = Modifier.weight(1f),
                )
                IconButton(onClick = viewModel::addCourse, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add course")
                }
            }
            state.saveError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing), modifier = Modifier.padding(vertical = Dimens.ItemSpacing)) {
                SecondaryButton("Cancel", onClick = onBack, icon = Icons.Outlined.Close, modifier = Modifier.weight(1f))
                PrimaryButton(
                    if (state.isSaving) "Saving..." else "Save",
                    enabled = state.canSave,
                    icon = Icons.Outlined.Check,
                    modifier = Modifier.weight(1f),
                    onClick = viewModel::save,
                )
            }
        }
    }
}
