package ca.georgebrown.studygroupfinder.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ca.georgebrown.studygroupfinder.data.model.User
import ca.georgebrown.studygroupfinder.data.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the Edit Profile form draws. Errors are messages safe to show the user. */
data class EditProfileUiState(
    val isSignedOut: Boolean = false,
    val name: String = "",
    val email: String = "",
    val program: String = "",
    val courses: List<String> = emptyList(),
    val newCourse: String = "",
    val nameError: String? = null,
    val programError: String? = null,
    val courseError: String? = null,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    /** Becomes true once the save succeeds; the screen navigates back when it sees this. */
    val isSaved: Boolean = false,
) {
    val canSave: Boolean get() = !isSignedOut && !isSaving && nameError == null && programError == null
}

/**
 * Profile + Edit Profile logic (FR-16).
 *
 * Talks only to [AuthRepository], so it works today with LocalAuthRepository and unchanged once
 * FirebaseAuthRepository saves to users/{uid} in Firestore. Saving can fail on a real network,
 * so failures become a message on screen instead of a crash or a stuck spinner.
 */
class ProfileViewModel(private val authRepository: AuthRepository) : ViewModel() {

    /** The signed-in user for the read-only Profile screen. */
    val user: StateFlow<User?> = authRepository.currentUser

    private val _editState = MutableStateFlow(initialEditState(authRepository.currentUser.value))
    val editState: StateFlow<EditProfileUiState> = _editState.asStateFlow()

    fun onNameChange(value: String) = _editState.update {
        it.copy(name = value, nameError = ProfileValidator.validateName(value), saveError = null)
    }

    fun onProgramChange(value: String) = _editState.update {
        it.copy(program = value, programError = ProfileValidator.validateProgram(value), saveError = null)
    }

    fun onNewCourseChange(value: String) = _editState.update {
        it.copy(newCourse = ProfileValidator.normalizeCourse(value).take(10), courseError = null)
    }

    fun addCourse() = _editState.update {
        val code = ProfileValidator.normalizeCourse(it.newCourse)
        val error = ProfileValidator.validateNewCourse(code, it.courses)
        if (error != null) it.copy(courseError = error) else it.copy(courses = it.courses + code, newCourse = "", courseError = null)
    }

    fun removeCourse(code: String) = _editState.update { it.copy(courses = it.courses - code, courseError = null) }

    fun save() {
        val state = _editState.value
        val current = authRepository.currentUser.value
        if (current == null) {
            _editState.update { it.copy(isSignedOut = true) }
            return
        }
        // Re-check in case save is called without any edits (errors are only computed on change).
        val nameError = ProfileValidator.validateName(state.name)
        val programError = ProfileValidator.validateProgram(state.program)
        if (nameError != null || programError != null || state.isSaving) {
            _editState.update { it.copy(nameError = nameError, programError = programError) }
            return
        }

        _editState.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            val updated = current.copy(name = state.name.trim(), program = state.program.trim(), enrolledCourses = state.courses)
            try {
                authRepository.updateProfile(updated)
                _editState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: CancellationException) {
                throw e // Leaving the screen cancels the save; that's not an error to show.
            } catch (e: Exception) {
                _editState.update { it.copy(isSaving = false, saveError = "Couldn't save your profile. Check your connection and try again.") }
            }
        }
    }

    fun signOut() = authRepository.signOut()

    companion object {
        private fun initialEditState(user: User?): EditProfileUiState =
            if (user == null) EditProfileUiState(isSignedOut = true)
            else EditProfileUiState(name = user.name, email = user.email, program = user.program, courses = user.enrolledCourses)

        fun factory(authRepository: AuthRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { ProfileViewModel(authRepository) }
        }
    }
}
