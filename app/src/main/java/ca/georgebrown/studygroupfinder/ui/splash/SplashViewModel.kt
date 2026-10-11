package ca.georgebrown.studygroupfinder.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ca.georgebrown.studygroupfinder.data.repository.AuthRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What the splash screen should do next. */
sealed interface SplashState {
    data object Loading : SplashState
    data object SignedIn : SplashState
    data object SignedOut : SplashState
}

/**
 * Splash logic (FR-02 "remembers a signed-in user"): check for a saved session while the logo shows,
 * then route to Feed or Login. A minimum display time stops the logo from flashing on fast devices.
 */
class SplashViewModel(
    private val authRepository: AuthRepository,
    private val minimumDisplayMillis: Long = 1_200L,
) : ViewModel() {

    private val _state = MutableStateFlow<SplashState>(SplashState.Loading)
    val state: StateFlow<SplashState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val restore = async { runCatching { authRepository.restoreSession() }.getOrNull() }
            delay(minimumDisplayMillis)
            // Any failure while restoring means "not signed in", never a crash.
            _state.value = if (restore.await() != null) SplashState.SignedIn else SplashState.SignedOut
        }
    }

    companion object {
        fun factory(authRepository: AuthRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { SplashViewModel(authRepository) }
        }
    }
}
