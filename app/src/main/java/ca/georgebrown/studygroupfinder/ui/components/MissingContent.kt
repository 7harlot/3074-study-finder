package ca.georgebrown.studygroupfinder.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ca.georgebrown.studygroupfinder.ui.theme.Dimens

/** Shown instead of crashing when a screen is opened with an id that no longer exists. */
@Composable
fun MissingContent(message: String, modifier: Modifier = Modifier) {
    Text(
        message,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(Dimens.ScreenPadding),
    )
}
