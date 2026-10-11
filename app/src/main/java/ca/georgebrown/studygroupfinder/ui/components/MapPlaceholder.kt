package ca.georgebrown.studygroupfinder.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import ca.georgebrown.studygroupfinder.ui.theme.MapLand
import ca.georgebrown.studygroupfinder.ui.theme.MapRoad

/**
 * Drawn stand-in for Google Maps in Prototype 1.
 *
 * Why not the real map yet: Maps Compose needs an API key in local.properties, and a missing key
 * shows a blank grey map. Ben swaps this for GoogleMap { Marker(...) } in the Maps & Location sprint (week 8);
 * callers do not change because they only pass a label.
 */
@Composable
fun MapPlaceholder(
    modifier: Modifier = Modifier,
    destinationLabel: String = "Session destination",
    showCurrentLocation: Boolean = false,
) {
    Box(modifier = modifier.clip(MaterialTheme.shapes.medium).background(MapLand)) {
        Canvas(Modifier.fillMaxSize()) {
            // A loose street grid: a few horizontal, vertical and one diagonal road.
            val roadWidth = 10f
            for (i in 1..4) {
                val y = size.height * i / 5f
                drawLine(MapRoad, Offset(0f, y), Offset(size.width, y), strokeWidth = roadWidth)
            }
            for (i in 1..3) {
                val x = size.width * i / 4f
                drawLine(MapRoad, Offset(x, 0f), Offset(x, size.height), strokeWidth = roadWidth)
            }
            drawLine(MapRoad, Offset(0f, size.height), Offset(size.width, 0f), strokeWidth = roadWidth * 1.5f)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center).padding(bottom = 24.dp),
        ) {
            Icon(Icons.Filled.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            Text(destinationLabel, style = MaterialTheme.typography.labelLarge)
        }
        if (showCurrentLocation) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
                Text("Current location", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
