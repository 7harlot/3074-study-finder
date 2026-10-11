package ca.georgebrown.studygroupfinder.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Place
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ca.georgebrown.studygroupfinder.data.model.StudySession
import ca.georgebrown.studygroupfinder.ui.components.AppTopBar
import ca.georgebrown.studygroupfinder.ui.components.InfoRow
import ca.georgebrown.studygroupfinder.ui.components.MapPlaceholder
import ca.georgebrown.studygroupfinder.ui.components.MissingContent
import ca.georgebrown.studygroupfinder.ui.components.SecondaryButton
import ca.georgebrown.studygroupfinder.ui.components.SectionCard
import ca.georgebrown.studygroupfinder.ui.theme.Dimens

/**
 * Screen 6: Full-Screen Map. Prototype 1 uses [MapPlaceholder]; the Maps & Location sprint (Ben, week 8)
 * swaps in Google Maps, the location permission prompt and the Get Directions intent (FR-11, FR-12).
 */
@Composable
fun SessionMapScreen(
    session: StudySession?,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "Session Location", onBack = onBack)
        if (session == null) {
            MissingContent("This session no longer exists.")
            return@Column
        }
        MapPlaceholder(
            destinationLabel = "Session destination",
            showCurrentLocation = true,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = Dimens.ScreenPadding),
        )
        Column(
            modifier = Modifier.padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            SectionCard { InfoRow(Icons.Outlined.Place, "${session.campusName} - ${session.locationLabel}") }
            SecondaryButton("Get Directions", onClick = {}, icon = Icons.Outlined.Directions, modifier = Modifier.fillMaxWidth())
        }
    }
}
