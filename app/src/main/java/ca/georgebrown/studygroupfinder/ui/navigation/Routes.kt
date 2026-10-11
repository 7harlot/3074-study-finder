package ca.georgebrown.studygroupfinder.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Every destination in the app, matching the navigation diagram in the design proposal.
 * Routes with arguments expose a create(...) helper so callers never hand-build strings.
 */
object Routes {
    const val SPLASH = "splash"
    const val AUTH = "auth"
    const val FEED = "feed"
    const val MY_SESSIONS = "my_sessions"
    const val PROFILE = "profile"
    const val EDIT_PROFILE = "edit_profile"
    const val ABOUT = "about"

    const val ARG_SESSION_ID = "sessionId"

    const val SESSION_DETAILS = "session/{$ARG_SESSION_ID}"
    fun sessionDetails(sessionId: String) = "session/$sessionId"

    const val SESSION_MAP = "session/{$ARG_SESSION_ID}/map"
    fun sessionMap(sessionId: String) = "session/$sessionId/map"

    /** Create when sessionId is absent, Edit when present: one screen, two modes (FR-03, FR-13). */
    const val EDIT_SESSION = "edit_session?$ARG_SESSION_ID={$ARG_SESSION_ID}"
    fun createSession() = "edit_session"
    fun editSession(sessionId: String) = "edit_session?$ARG_SESSION_ID=$sessionId"

    /** Screens that show the bottom bar. Splash and Login sit outside it (users aren't signed in yet). */
    val signedInRoutes = setOf(
        FEED, MY_SESSIONS, PROFILE, EDIT_PROFILE, ABOUT, SESSION_DETAILS, SESSION_MAP, EDIT_SESSION,
    )
}

/** The three bottom-bar tabs. */
enum class TopLevelTab(val route: String, val label: String, val icon: ImageVector) {
    HOME(Routes.FEED, "Home", Icons.Filled.Home),
    MY_SESSIONS(Routes.MY_SESSIONS, "My Sessions", Icons.AutoMirrored.Filled.List),
    PROFILE(Routes.PROFILE, "Profile", Icons.Filled.Person),
}
