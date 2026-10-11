package ca.georgebrown.studygroupfinder.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ca.georgebrown.studygroupfinder.data.repository.AuthRepository
import ca.georgebrown.studygroupfinder.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode { LOGIN, SIGN_UP }

/** Everything the Login / Sign Up screen draws. Field errors only appear after the first submit attempt. */
data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val fullNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val formError: String? = null,
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val hasAttemptedSubmit: Boolean = false,
)

/** Login / Sign Up logic: validation, calling the repository, and loading / error states. */
class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onModeChange(mode: AuthMode) = _uiState.update {
        // Switching tabs keeps the email (handy) but clears passwords and stale errors.
        AuthUiState(mode = mode, email = it.email)
    }

    fun onFullNameChange(value: String) = updateAndRevalidate { it.copy(fullName = value) }
    fun onEmailChange(value: String) = updateAndRevalidate { it.copy(email = value) }
    fun onPasswordChange(value: String) = updateAndRevalidate { it.copy(password = value) }
    fun onConfirmPasswordChange(value: String) = updateAndRevalidate { it.copy(confirmPassword = value) }

    fun submit() {
        val validated = validate(_uiState.value.copy(hasAttemptedSubmit = true, formError = null))
        _uiState.value = validated
        if (validated.hasFieldErrors() || validated.isLoading) return

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val state = _uiState.value
            val result = try {
                when (state.mode) {
                    AuthMode.LOGIN -> authRepository.signIn(state.email, state.password)
                    AuthMode.SIGN_UP -> authRepository.signUp(state.fullName, state.email, state.password)
                }
            } catch (e: Exception) {
                AuthResult.Error("Something went wrong. Please try again.")
            }
            _uiState.update {
                when (result) {
                    is AuthResult.Success -> it.copy(isLoading = false, isAuthenticated = true)
                    is AuthResult.Error -> it.copy(isLoading = false, formError = result.message)
                }
            }
        }
    }

    /** Live re-validation, but only once the user has tried to submit (no red errors while typing at first). */
    private fun updateAndRevalidate(change: (AuthUiState) -> AuthUiState) = _uiState.update {
        val changed = change(it).copy(formError = null)
        if (changed.hasAttemptedSubmit) validate(changed) else changed
    }

    private fun validate(state: AuthUiState): AuthUiState = when (state.mode) {
        AuthMode.LOGIN -> state.copy(
            fullNameError = null,
            emailError = AuthValidator.validateEmail(state.email),
            passwordError = AuthValidator.validateLoginPassword(state.password),
            confirmPasswordError = null,
        )
        AuthMode.SIGN_UP -> state.copy(
            fullNameError = AuthValidator.validateFullName(state.fullName),
            emailError = AuthValidator.validateEmail(state.email),
            passwordError = AuthValidator.validateNewPassword(state.password),
            confirmPasswordError = AuthValidator.validateConfirmPassword(state.password, state.confirmPassword),
        )
    }

    private fun AuthUiState.hasFieldErrors() =
        listOf(fullNameError, emailError, passwordError, confirmPasswordError).any { it != null }

    companion object {
        fun factory(authRepository: AuthRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { AuthViewModel(authRepository) }
        }
    }
}
