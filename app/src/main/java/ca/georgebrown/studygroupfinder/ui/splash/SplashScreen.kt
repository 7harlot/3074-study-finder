package ca.georgebrown.studygroupfinder.ui.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Screen 1: logo + "Loading..." while [SplashViewModel] checks for a remembered sign-in. */
@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onSignedIn: () -> Unit,
    onSignedOut: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Navigate exactly once when the check finishes.
    LaunchedEffect(state) {
        when (state) {
            SplashState.SignedIn -> onSignedIn()
            SplashState.SignedOut -> onSignedOut()
            SplashState.Loading -> Unit
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(96.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text("Campus Study Group Finder", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(40.dp))
        Text("Loading...", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            modifier = Modifier.width(180.dp).semantics { contentDescription = "Checking sign-in" },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}
