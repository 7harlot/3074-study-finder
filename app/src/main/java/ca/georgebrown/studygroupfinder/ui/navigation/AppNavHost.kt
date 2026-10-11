package ca.georgebrown.studygroupfinder.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ca.georgebrown.studygroupfinder.di.AppContainer
import ca.georgebrown.studygroupfinder.ui.about.AboutScreen
import ca.georgebrown.studygroupfinder.ui.auth.AuthScreen
import ca.georgebrown.studygroupfinder.ui.auth.AuthViewModel
import ca.georgebrown.studygroupfinder.ui.details.SessionDetailsScreen
import ca.georgebrown.studygroupfinder.ui.editsession.CreateEditSessionScreen
import ca.georgebrown.studygroupfinder.ui.feed.FeedScreen
import ca.georgebrown.studygroupfinder.ui.map.SessionMapScreen
import ca.georgebrown.studygroupfinder.ui.mysessions.MySessionsScreen
import ca.georgebrown.studygroupfinder.ui.profile.EditProfileScreen
import ca.georgebrown.studygroupfinder.ui.profile.ProfileScreen
import ca.georgebrown.studygroupfinder.ui.splash.SplashScreen
import ca.georgebrown.studygroupfinder.ui.splash.SplashViewModel

/**
 * The whole navigation graph in one place (Section 5 nav diagram).
 * One root Scaffold owns the bottom bar and system-bar insets, so individual screens stay simple.
 */
@Composable
fun AppNavHost(
    container: AppContainer,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in Routes.signedInRoutes
    val selectedTab = tabFor(currentRoute)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(selectedTab = selectedTab, onTabSelected = { navController.navigateToTab(it) })
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            composable(Routes.SPLASH) {
                val vm: SplashViewModel = viewModel(factory = SplashViewModel.factory(container.authRepository))
                SplashScreen(
                    viewModel = vm,
                    onSignedIn = { navController.navigateClearingBackStack(Routes.FEED) },
                    onSignedOut = { navController.navigateClearingBackStack(Routes.AUTH) },
                )
            }
            composable(Routes.AUTH) {
                val vm: AuthViewModel = viewModel(factory = AuthViewModel.factory(container.authRepository))
                AuthScreen(
                    viewModel = vm,
                    onAuthenticated = { navController.navigateClearingBackStack(Routes.FEED) },
                )
            }
            composable(Routes.FEED) {
                FeedScreen(
                    sessionRepository = container.sessionRepository,
                    onOpenSession = { navController.navigate(Routes.sessionDetails(it)) },
                    onCreateSession = { navController.navigate(Routes.createSession()) },
                )
            }
            composable(
                Routes.SESSION_DETAILS,
                arguments = listOf(navArgument(Routes.ARG_SESSION_ID) { type = NavType.StringType }),
            ) { entry ->
                val sessionId = entry.arguments?.getString(Routes.ARG_SESSION_ID).orEmpty()
                SessionDetailsScreen(
                    session = container.sessionRepository.getSession(sessionId),
                    currentUserId = container.authRepository.currentUser.value?.userId,
                    onBack = { navController.popBackStack() },
                    onViewMap = { navController.navigate(Routes.sessionMap(sessionId)) },
                    onEdit = { navController.navigate(Routes.editSession(sessionId)) },
                )
            }
            composable(
                Routes.SESSION_MAP,
                arguments = listOf(navArgument(Routes.ARG_SESSION_ID) { type = NavType.StringType }),
            ) { entry ->
                val sessionId = entry.arguments?.getString(Routes.ARG_SESSION_ID).orEmpty()
                SessionMapScreen(
                    session = container.sessionRepository.getSession(sessionId),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                Routes.EDIT_SESSION,
                arguments = listOf(navArgument(Routes.ARG_SESSION_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }),
            ) { entry ->
                val sessionId = entry.arguments?.getString(Routes.ARG_SESSION_ID)
                CreateEditSessionScreen(
                    existing = sessionId?.let { container.sessionRepository.getSession(it) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.MY_SESSIONS) {
                MySessionsScreen(
                    sessionRepository = container.sessionRepository,
                    currentUserId = container.authRepository.currentUser.value?.userId,
                    onOpenSession = { navController.navigate(Routes.sessionDetails(it)) },
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    authRepository = container.authRepository,
                    onEditProfile = { navController.navigate(Routes.EDIT_PROFILE) },
                    onAbout = { navController.navigate(Routes.ABOUT) },
                    onSignedOut = { navController.navigateClearingBackStack(Routes.AUTH) },
                )
            }
            composable(Routes.EDIT_PROFILE) {
                EditProfileScreen(
                    authRepository = container.authRepository,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.ABOUT) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

/** Which tab to highlight. Detail screens highlight the tab they were reached from most often. */
private fun tabFor(route: String?): TopLevelTab? = when (route) {
    Routes.FEED, Routes.SESSION_DETAILS, Routes.SESSION_MAP, Routes.EDIT_SESSION -> TopLevelTab.HOME
    Routes.MY_SESSIONS -> TopLevelTab.MY_SESSIONS
    Routes.PROFILE, Routes.EDIT_PROFILE, Routes.ABOUT -> TopLevelTab.PROFILE
    else -> null
}

/** Standard bottom-bar behaviour: one copy of each tab, state restored when you come back. */
private fun NavHostController.navigateToTab(tab: TopLevelTab) {
    navigate(tab.route) {
        // Feed is the root of the signed-in back stack (Splash/Login are cleared on sign-in).
        popUpTo(Routes.FEED) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Used after sign-in / sign-out so Back can't return to Splash, Login, or a signed-out user's screens. */
private fun NavHostController.navigateClearingBackStack(route: String) {
    navigate(route) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}
