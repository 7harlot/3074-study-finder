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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.georgebrown.studygroupfinder.data.repository.AuthRepository
import ca.georgebrown.studygroupfinder.ui.components.AppTextField
import ca.georgebrown.studygroupfinder.ui.components.AppTopBar
import ca.georgebrown.studygroupfinder.ui.components.MissingContent
import ca.georgebrown.studygroupfinder.ui.components.PrimaryButton
import ca.georgebrown.studygroupfinder.ui.components.SecondaryButton
import ca.georgebrown.studygroupfinder.ui.theme.Dimens
import kotlinx.coroutines.launch

/** Screen 10: Edit Profile (FR-16). Name, program and course chips; email is read-only because it is the login. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditProfileScreen(
    authRepository: AuthRepository,
    onBack: () -> Unit,
) {
    val user = authRepository.currentUser.value
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "Edit Profile", onBack = onBack)
        if (user == null) {
            MissingContent("You're signed out. Sign in again to edit your profile.")
            return@Column
        }
        var name by rememberSaveable { mutableStateOf(user.name) }
        var program by rememberSaveable { mutableStateOf(user.program) }
        var courses by rememberSaveable { mutableStateOf(user.enrolledCourses) }
        var newCourse by rememberSaveable { mutableStateOf("") }
        val nameError = if (name.isBlank()) "Name can't be empty." else null

        Column(
            modifier = Modifier.imePadding().verticalScroll(rememberScrollState()).padding(horizontal = Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
        ) {
            AppTextField(name, { name = it }, "Name", leadingIcon = Icons.Outlined.Badge, errorText = nameError)
            AppTextField(user.email, {}, "Email", leadingIcon = Icons.Outlined.Email, readOnly = true, enabled = false,
                supportingText = "Your email is your login and can't be changed here.")
            AppTextField(program, { program = it }, "Program", placeholder = "Computer Programming", leadingIcon = Icons.Outlined.School)

            Text("Enrolled courses", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = Dimens.ItemSpacing))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing)) {
                courses.forEach { course ->
                    InputChip(
                        selected = false,
                        onClick = { courses = courses - course },
                        label = { Text(course) },
                        trailingIcon = { Icon(Icons.Outlined.Close, contentDescription = "Remove $course") },
                    )
                }
            }
            Row {
                AppTextField(
                    newCourse, { newCourse = it.uppercase().take(10) }, "Add course code",
                    placeholder = "COMP3074", modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        val code = newCourse.trim()
                        if (code.isNotEmpty() && code !in courses) courses = courses + code
                        newCourse = ""
                    },
                    modifier = Modifier.padding(top = 8.dp),
                ) { Icon(Icons.Outlined.Add, contentDescription = "Add course") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing), modifier = Modifier.padding(vertical = Dimens.ItemSpacing)) {
                SecondaryButton("Cancel", onClick = onBack, icon = Icons.Outlined.Close, modifier = Modifier.weight(1f))
                PrimaryButton(
                    "Save",
                    enabled = nameError == null,
                    icon = Icons.Outlined.Check,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            authRepository.updateProfile(user.copy(name = name.trim(), program = program.trim(), enrolledCourses = courses))
                            onBack()
                        }
                    },
                )
            }
        }
    }
}
