package ca.georgebrown.studygroupfinder.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.georgebrown.studygroupfinder.data.repository.AuthRepository
import ca.georgebrown.studygroupfinder.ui.components.AppTopBar
import ca.georgebrown.studygroupfinder.ui.components.InfoRow
import ca.georgebrown.studygroupfinder.ui.components.SecondaryButton
import ca.georgebrown.studygroupfinder.ui.components.SectionCard
import ca.georgebrown.studygroupfinder.ui.theme.Dimens

/** Screen 8: Profile. Shows the signed-in user; Sign Out really signs out (FR-02, FR-16). */
@Composable
fun ProfileScreen(
    authRepository: AuthRepository,
    onEditProfile: () -> Unit,
    onAbout: () -> Unit,
    onSignedOut: () -> Unit,
) {
    val user by authRepository.currentUser.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "Profile")
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Icon(Icons.Filled.Person, contentDescription = "Profile photo", modifier = Modifier.size(64.dp))
            }
            SectionCard {
                InfoRow(Icons.Outlined.Badge, "Name: ${user?.name.orEmpty()}")
                InfoRow(Icons.Outlined.Email, "Email: ${user?.email.orEmpty()}")
            }
            Text("Optional profile information", style = MaterialTheme.typography.titleSmall)
            SectionCard {
                InfoRow(Icons.Outlined.School, "Program: ${user?.program?.ifBlank { "Not set" } ?: "Not set"}")
                InfoRow(Icons.Outlined.Book, "Courses: ${user?.enrolledCourses?.joinToString().orEmpty().ifBlank { "None yet" }}")
            }
            SecondaryButton("Edit Profile", onClick = onEditProfile, icon = Icons.Outlined.Edit, modifier = Modifier.fillMaxWidth())
            SecondaryButton("About", onClick = onAbout, icon = Icons.Outlined.Info, modifier = Modifier.fillMaxWidth())
            SecondaryButton(
                "Sign Out",
                onClick = {
                    authRepository.signOut()
                    onSignedOut()
                },
                icon = Icons.AutoMirrored.Outlined.Logout,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
