package ca.georgebrown.studygroupfinder.data.repository

import android.content.Context
import android.content.SharedPreferences
import ca.georgebrown.studygroupfinder.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

/**
 * Prototype stand-in for Firebase Authentication.
 *
 * Accounts and the "remember me" session are kept in SharedPreferences so that
 * sign-up, sign-in, sign-out and staying signed in across launches all really work on the device.
 * Passwords are stored as a SHA-256 hash, never plain text. This is NOT production security
 * (no salt, no server); it only exists until Firebase Auth replaces it.
 */
class LocalAuthRepository(context: Context) : AuthRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        // A demo account so the marker can sign in without creating one.
        if (!prefs.contains(keyPassword(DEMO_EMAIL))) {
            saveUser(
                User(
                    userId = "demo-user",
                    name = "Aisha Patel",
                    email = DEMO_EMAIL,
                    program = "Computer Programming",
                    enrolledCourses = listOf("COMP3074", "COMP3095"),
                ),
                password = DEMO_PASSWORD,
            )
        }
    }

    override suspend fun restoreSession(): User? = withContext(Dispatchers.IO) {
        val email = prefs.getString(KEY_SIGNED_IN_EMAIL, null) ?: return@withContext null
        loadUser(email).also { _currentUser.value = it }
    }

    override suspend fun signIn(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        delay(FAKE_NETWORK_DELAY_MS) // lets the loading state be seen in the demo video
        val normalizedEmail = email.trim().lowercase()
        val storedHash = prefs.getString(keyPassword(normalizedEmail), null)
        when {
            storedHash == null -> AuthResult.Error("No account found for that email. Try Sign Up.")
            storedHash != hash(password) -> AuthResult.Error("Incorrect password.")
            else -> {
                val user = loadUser(normalizedEmail)
                    ?: return@withContext AuthResult.Error("Account data is missing. Please sign up again.")
                prefs.edit().putString(KEY_SIGNED_IN_EMAIL, normalizedEmail).apply()
                _currentUser.value = user
                AuthResult.Success(user)
            }
        }
    }

    override suspend fun signUp(fullName: String, email: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            delay(FAKE_NETWORK_DELAY_MS)
            val normalizedEmail = email.trim().lowercase()
            if (prefs.contains(keyPassword(normalizedEmail))) {
                return@withContext AuthResult.Error("An account with that email already exists. Try Login.")
            }
            val user = User(userId = UUID.randomUUID().toString(), name = fullName.trim(), email = normalizedEmail)
            saveUser(user, password)
            prefs.edit().putString(KEY_SIGNED_IN_EMAIL, normalizedEmail).apply()
            _currentUser.value = user
            AuthResult.Success(user)
        }

    override suspend fun updateProfile(updated: User) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putString(keyName(updated.email), updated.name)
            .putString(keyProgram(updated.email), updated.program)
            .putString(keyCourses(updated.email), updated.enrolledCourses.joinToString(","))
            .apply()
        _currentUser.value = updated
    }

    override fun signOut() {
        prefs.edit().remove(KEY_SIGNED_IN_EMAIL).apply()
        _currentUser.value = null
    }

    // --- storage helpers: one flat key per field keeps this readable without a JSON library ---

    private fun saveUser(user: User, password: String) {
        prefs.edit()
            .putString(keyPassword(user.email), hash(password))
            .putString(keyId(user.email), user.userId)
            .putString(keyName(user.email), user.name)
            .putString(keyProgram(user.email), user.program)
            .putString(keyCourses(user.email), user.enrolledCourses.joinToString(","))
            .apply()
    }

    private fun loadUser(email: String): User? {
        val id = prefs.getString(keyId(email), null) ?: return null
        return User(
            userId = id,
            name = prefs.getString(keyName(email), "").orEmpty(),
            email = email,
            program = prefs.getString(keyProgram(email), "").orEmpty(),
            enrolledCourses = prefs.getString(keyCourses(email), "").orEmpty()
                .split(",").map { it.trim() }.filter { it.isNotEmpty() },
        )
    }

    private fun hash(password: String): String =
        MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private fun keyPassword(email: String) = "user.$email.password"
    private fun keyId(email: String) = "user.$email.id"
    private fun keyName(email: String) = "user.$email.name"
    private fun keyProgram(email: String) = "user.$email.program"
    private fun keyCourses(email: String) = "user.$email.courses"

    companion object {
        private const val PREFS_NAME = "local_auth"
        private const val KEY_SIGNED_IN_EMAIL = "signed_in_email"
        private const val FAKE_NETWORK_DELAY_MS = 600L
        const val DEMO_EMAIL = "aisha.patel@georgebrown.ca"
        const val DEMO_PASSWORD = "password123"
    }
}
