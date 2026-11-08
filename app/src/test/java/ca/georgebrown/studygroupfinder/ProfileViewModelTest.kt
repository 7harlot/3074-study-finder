package ca.georgebrown.studygroupfinder

import ca.georgebrown.studygroupfinder.data.model.User
import ca.georgebrown.studygroupfinder.data.repository.AuthRepository
import ca.georgebrown.studygroupfinder.data.repository.AuthResult
import ca.georgebrown.studygroupfinder.ui.profile.ProfileValidator
import ca.georgebrown.studygroupfinder.ui.profile.ProfileViewModel
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Edit Profile (FR-16): validation, course chips, saving, failed saves, and signed-out users. */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val aisha = User("u1", "Aisha Patel", "aisha@georgebrown.ca", "Computer Programming", listOf("COMP3074"))

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `form starts from the signed in user`() {
        val state = ProfileViewModel(FakeAuth(aisha)).editState.value
        assertEquals("Aisha Patel", state.name)
        assertEquals(listOf("COMP3074"), state.courses)
        assertTrue(state.canSave)
    }

    @Test fun `signed out user sees signed out state and cannot save`() {
        val vm = ProfileViewModel(FakeAuth(null))
        assertTrue(vm.editState.value.isSignedOut)
        assertFalse(vm.editState.value.canSave)
    }

    @Test fun `blank or too long name blocks saving`() {
        val vm = ProfileViewModel(FakeAuth(aisha))
        vm.onNameChange("   ")
        assertNotNull(vm.editState.value.nameError)
        assertFalse(vm.editState.value.canSave)
        vm.onNameChange("x".repeat(ProfileValidator.MAX_NAME_LENGTH + 1))
        assertNotNull(vm.editState.value.nameError)
        vm.onNameChange("Aisha P.")
        assertNull(vm.editState.value.nameError)
    }

    @Test fun `course codes are normalised and validated`() {
        val vm = ProfileViewModel(FakeAuth(aisha))
        vm.onNewCourseChange("comp 3095")
        vm.addCourse()
        assertEquals(listOf("COMP3074", "COMP3095"), vm.editState.value.courses)
        assertEquals("", vm.editState.value.newCourse)

        vm.onNewCourseChange("COMP3074") // duplicate
        vm.addCourse()
        assertEquals("COMP3074 is already in your list.", vm.editState.value.courseError)

        vm.onNewCourseChange("hello") // wrong shape
        vm.addCourse()
        assertEquals("Use a code like COMP3074.", vm.editState.value.courseError)
        assertEquals(2, vm.editState.value.courses.size)

        vm.removeCourse("COMP3074")
        assertEquals(listOf("COMP3095"), vm.editState.value.courses)
    }

    @Test fun `course limit is enforced`() {
        val full = (1..ProfileValidator.MAX_COURSES).map { "COMP%04d".format(it) }
        assertEquals("You can list up to 10 courses.", ProfileValidator.validateNewCourse("COMP9999", full))
        assertEquals("Type a course code first.", ProfileValidator.validateNewCourse("", emptyList()))
    }

    @Test fun `save trims fields and reports success`() = runTest(dispatcher) {
        val auth = FakeAuth(aisha)
        val vm = ProfileViewModel(auth)
        vm.onNameChange("  Aisha Patel  ")
        vm.onProgramChange(" Software Engineering ")
        vm.save()
        assertTrue(vm.editState.value.isSaving)
        advanceUntilIdle()
        assertTrue(vm.editState.value.isSaved)
        assertFalse(vm.editState.value.isSaving)
        assertEquals("Aisha Patel", auth.saved?.name)
        assertEquals("Software Engineering", auth.saved?.program)
        assertEquals("u1", auth.saved?.userId) // id and email are never changed by the form
    }

    @Test fun `failed save shows a message and keeps the form`() = runTest(dispatcher) {
        val vm = ProfileViewModel(FakeAuth(aisha, failSave = true))
        vm.onProgramChange("Game Design")
        vm.save()
        advanceUntilIdle()
        val state = vm.editState.value
        assertFalse(state.isSaved)
        assertFalse(state.isSaving)
        assertNotNull(state.saveError)
        assertEquals("Game Design", state.program)
    }

    @Test fun `user who signed out elsewhere cannot save`() = runTest(dispatcher) {
        val auth = FakeAuth(aisha)
        val vm = ProfileViewModel(auth)
        auth.signOut()
        vm.save()
        advanceUntilIdle()
        assertTrue(vm.editState.value.isSignedOut)
        assertNull(auth.saved)
    }

    private class FakeAuth(user: User?, private val failSave: Boolean = false) : AuthRepository {
        private val state = MutableStateFlow(user)
        var saved: User? = null
        override val currentUser: StateFlow<User?> = state
        override suspend fun restoreSession(): User? = state.value
        override suspend fun signIn(email: String, password: String) = AuthResult.Error("unused")
        override suspend fun signUp(fullName: String, email: String, password: String) = AuthResult.Error("unused")
        override suspend fun updateProfile(updated: User) {
            if (failSave) error("network down")
            saved = updated
            state.value = updated
        }
        override fun signOut() { state.value = null }
    }
}
