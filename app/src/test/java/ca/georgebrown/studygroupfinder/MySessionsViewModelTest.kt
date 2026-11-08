package ca.georgebrown.studygroupfinder

import ca.georgebrown.studygroupfinder.data.model.LocationType
import ca.georgebrown.studygroupfinder.data.model.StudySession
import ca.georgebrown.studygroupfinder.data.model.User
import ca.georgebrown.studygroupfinder.data.repository.AuthRepository
import ca.georgebrown.studygroupfinder.data.repository.AuthResult
import ca.georgebrown.studygroupfinder.data.repository.FakeSessionRepository
import ca.georgebrown.studygroupfinder.ui.mysessions.MySessionsTab
import ca.georgebrown.studygroupfinder.ui.mysessions.MySessionsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

/** My Sessions (FR-10): tabs, live updates after joining, and switching users. */
@OptIn(ExperimentalCoroutinesApi::class)
class MySessionsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun session(id: String, creator: String, participants: List<String>) = StudySession(
        sessionId = id, creatorId = creator, organizerName = creator, courseCode = "COMP3074", topic = id,
        description = "", startAtMillis = 0L, locationType = LocationType.ONLINE, maxParticipants = 5, participantIds = participants,
    )

    private val repo = FakeSessionRepository(
        listOf(
            session("mine", creator = "ben", participants = listOf("ben")),
            session("joined", creator = "munir", participants = listOf("munir", "ben")),
            session("other", creator = "munir", participants = listOf("munir")),
        ),
    )

    @Test fun `shows joined by default and created on the other tab`() = runTest(dispatcher) {
        val vm = MySessionsViewModel(repo, FakeAuth(User("ben", "Ben", "ben@georgebrown.ca")))
        backgroundScope.launch { vm.uiState.collect {} } // stateIn only runs while someone is watching

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(listOf("mine", "joined"), vm.uiState.value.visible.map { it.sessionId })
        vm.selectTab(MySessionsTab.CREATED)
        assertEquals(listOf("mine"), vm.uiState.value.visible.map { it.sessionId })
    }

    @Test fun `joining a session updates the list live`() = runTest(dispatcher) {
        val vm = MySessionsViewModel(repo, FakeAuth(User("ben", "Ben", "ben@georgebrown.ca")))
        backgroundScope.launch { vm.uiState.collect {} }

        repo.joinSession("other", "ben")
        assertEquals(3, vm.uiState.value.joined.size)
    }

    @Test fun `signing out empties the lists and signing in as someone else swaps them`() = runTest(dispatcher) {
        val auth = FakeAuth(User("ben", "Ben", "ben@georgebrown.ca"))
        val vm = MySessionsViewModel(repo, auth)
        backgroundScope.launch { vm.uiState.collect {} }

        auth.user.value = null
        assertEquals(0, vm.uiState.value.joined.size)
        auth.user.value = User("munir", "Munir", "munir@georgebrown.ca")
        assertEquals(listOf("joined", "other"), vm.uiState.value.created.map { it.sessionId })
    }

    private class FakeAuth(initial: User?) : AuthRepository {
        val user = MutableStateFlow(initial)
        override val currentUser: StateFlow<User?> = user
        override suspend fun restoreSession(): User? = user.value
        override suspend fun signIn(email: String, password: String) = AuthResult.Error("unused")
        override suspend fun signUp(fullName: String, email: String, password: String) = AuthResult.Error("unused")
        override suspend fun updateProfile(updated: User) = Unit
        override fun signOut() { user.value = null }
    }
}
