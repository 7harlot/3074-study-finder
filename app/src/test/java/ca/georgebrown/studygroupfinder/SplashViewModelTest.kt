package ca.georgebrown.studygroupfinder

import ca.georgebrown.studygroupfinder.data.model.User
import ca.georgebrown.studygroupfinder.data.repository.AuthRepository
import ca.georgebrown.studygroupfinder.data.repository.AuthResult
import ca.georgebrown.studygroupfinder.ui.splash.SplashState
import ca.georgebrown.studygroupfinder.ui.splash.SplashViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** Splash routing: remembered user -> Feed, nobody -> Login, and a broken store never crashes. */
@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `remembered user goes to signed in`() = runTest(dispatcher) {
        val vm = SplashViewModel(FakeAuth(restored = User("1", "Aisha", "a@georgebrown.ca")), minimumDisplayMillis = 10)
        assertEquals(SplashState.Loading, vm.state.value)
        advanceUntilIdle()
        assertEquals(SplashState.SignedIn, vm.state.value)
    }

    @Test fun `no saved user goes to signed out`() = runTest(dispatcher) {
        val vm = SplashViewModel(FakeAuth(restored = null), minimumDisplayMillis = 10)
        advanceUntilIdle()
        assertEquals(SplashState.SignedOut, vm.state.value)
    }

    @Test fun `restore failure is treated as signed out`() = runTest(dispatcher) {
        val vm = SplashViewModel(FakeAuth(restored = null, throwOnRestore = true), minimumDisplayMillis = 10)
        advanceUntilIdle()
        assertEquals(SplashState.SignedOut, vm.state.value)
    }

    private class FakeAuth(private val restored: User?, private val throwOnRestore: Boolean = false) : AuthRepository {
        override val currentUser: StateFlow<User?> = MutableStateFlow(null)
        override suspend fun restoreSession(): User? = if (throwOnRestore) error("disk error") else restored
        override suspend fun signIn(email: String, password: String) = AuthResult.Error("unused")
        override suspend fun signUp(fullName: String, email: String, password: String) = AuthResult.Error("unused")
        override suspend fun updateProfile(updated: User) = Unit
        override fun signOut() = Unit
    }
}
