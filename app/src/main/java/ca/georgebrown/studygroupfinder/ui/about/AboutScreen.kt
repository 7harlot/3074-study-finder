package ca.georgebrown.studygroupfinder.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ca.georgebrown.studygroupfinder.ui.components.AppTopBar
import ca.georgebrown.studygroupfinder.ui.components.InfoRow
import ca.georgebrown.studygroupfinder.ui.components.SectionCard
import ca.georgebrown.studygroupfinder.ui.theme.Dimens

/** The team, in one place, so the About screen and any credits stay in sync. */
private val teamMembers = listOf(
    "Abdirahman, Zuheib" to "Authentication & Session Management",
    "Howlader, Munir" to "Data & Discovery",
    "Morrison, Ben" to "Architecture & Location",
)

/** Screen 9: About (FR-14). App name, version and team members. */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "About", onBack = onBack)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(Dimens.ScreenPadding),
        ) {
            Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null, modifier = Modifier.size(72.dp))
            Text("Campus Study Group Finder", style = MaterialTheme.typography.titleLarge)
            Text("Version 1.0 (Prototype 1)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Find and organize study sessions with classmates at George Brown College.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            SectionCard {
                Text("Team members", style = MaterialTheme.typography.titleMedium)
                teamMembers.forEach { (name, role) ->
                    InfoRow(Icons.Outlined.Person, "$name: $role")
                }
            }
            Text(
                "COMP3074 Mobile Application Development, Group P100",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
