package ca.georgebrown.studygroupfinder.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// The mockups are dark-only, so the app always uses this scheme (no light theme, no dynamic color).
private val AppColorScheme = darkColorScheme(
    primary = OffWhite,
    onPrimary = Charcoal900,
    secondary = Gray300,
    onSecondary = Charcoal900,
    background = Charcoal900,
    onBackground = OffWhite,
    surface = Charcoal800,
    onSurface = OffWhite,
    surfaceVariant = Charcoal700,
    onSurfaceVariant = Gray300,
    surfaceContainer = Charcoal800,
    outline = Gray500,
    outlineVariant = Charcoal700,
    error = ErrorRed,
    onError = Charcoal900,
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(Dimens.CornerRadius),
    large = RoundedCornerShape(16.dp),
)

@Composable
fun StudyGroupTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
