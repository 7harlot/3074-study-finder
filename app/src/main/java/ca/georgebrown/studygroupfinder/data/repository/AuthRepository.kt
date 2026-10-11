package ca.georgebrown.studygroupfinder.data.repository

import ca.georgebrown.studygroupfinder.data.model.User
import kotlinx.coroutines.flow.StateFlow

/**
 * Everything the UI needs from authentication. The prototype uses [LocalAuthRepository];
 * the Firebase version (Zuheib's area) implements this same interface, so no screen changes.
 */
interface AuthRepository {
    /** The signed-in user, or null when signed out. */
    val currentUser: StateFlow<User?>

    /** Returns the user that was remembered from a previous launch, or null. Used by Splash (FR-02). */
    suspend fun restoreSession(): User?

    suspend fun signIn(email: String, password: String): AuthResult

    suspend fun signUp(fullName: String, email: String, password: String): AuthResult

    suspend fun updateProfile(updated: User)

    fun signOut()
}

/** Result of a sign-in or sign-up attempt. Errors carry a message that is safe to show the user. */
sealed interface AuthResult {
    data class Success(val user: User) : AuthResult
    data class Error(val message: String) : AuthResult
}
