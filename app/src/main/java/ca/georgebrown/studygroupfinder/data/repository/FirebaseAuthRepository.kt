package ca.georgebrown.studygroupfinder.data.repository

import android.util.Log
import ca.georgebrown.studygroupfinder.data.model.User
import ca.georgebrown.studygroupfinder.ui.auth.AuthValidator
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase Authentication (email/password) + the users/{uid} profile document in Firestore (FR-01, FR-02, FR-16).
 *
 * - Firebase keeps the login on the device by itself, so [restoreSession] just reads it back.
 * - Email domains follow AuthValidator.allowedEmailDomains (any email for now), checked here too so no screen can skip it.
 * - Firebase errors become friendly messages; nothing here throws for expected problems like a wrong password.
 * - If Firestore is unreachable we still let a signed-in user in, using what Firebase Auth knows (uid, name, email).
 */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        // Live auth state: if Firebase signs the user out (account deleted, token revoked), the app follows.
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser == null) _currentUser.value = null
        }
    }

    override suspend fun restoreSession(): User? {
        val firebaseUser = auth.currentUser ?: return null
        return loadOrCreateProfile(firebaseUser).also { _currentUser.value = it }
    }

    override suspend fun signIn(email: String, password: String): AuthResult = safeAuthCall {
        val normalizedEmail = email.trim().lowercase()
        domainError(normalizedEmail)?.let { return@safeAuthCall AuthResult.Error(it) }
        val firebaseUser = auth.signInWithEmailAndPassword(normalizedEmail, password).await().user
            ?: return@safeAuthCall AuthResult.Error(GENERIC_ERROR)
        val user = loadOrCreateProfile(firebaseUser)
        _currentUser.value = user
        AuthResult.Success(user)
    }

    override suspend fun signUp(fullName: String, email: String, password: String): AuthResult = safeAuthCall {
        val normalizedEmail = email.trim().lowercase()
        domainError(normalizedEmail)?.let { return@safeAuthCall AuthResult.Error(it) }
        val firebaseUser = auth.createUserWithEmailAndPassword(normalizedEmail, password).await().user
            ?: return@safeAuthCall AuthResult.Error(GENERIC_ERROR)
        // Store the name on the Auth account too, so it's available even before Firestore answers.
        firebaseUser.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(fullName.trim()).build()).await()
        val user = User(userId = firebaseUser.uid, name = fullName.trim(), email = normalizedEmail)
        usersCollection().document(user.userId).set(user.toFirestore()).await()
        _currentUser.value = user
        AuthResult.Success(user)
    }

    /** Throws on failure so the caller (ProfileViewModel) can show "Couldn't save". */
    override suspend fun updateProfile(updated: User) {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        require(updated.userId == uid) { "Can't edit another user's profile" }
        // Only the editable fields; email and createdAt never change from here.
        usersCollection().document(uid).update(
            mapOf(
                FIELD_NAME to updated.name,
                FIELD_PROGRAM to updated.program,
                FIELD_COURSES to updated.enrolledCourses,
            ),
        ).await()
        _currentUser.value = updated
    }

    override fun signOut() {
        auth.signOut()
        _currentUser.value = null
    }

    // --- helpers ---

    private fun usersCollection() = firestore.collection(USERS)

    /** Reads users/{uid}; creates it if missing (e.g. account made in the Firebase console). Falls back offline. */
    private suspend fun loadOrCreateProfile(firebaseUser: FirebaseUser): User {
        val fallback = User(
            userId = firebaseUser.uid,
            name = firebaseUser.displayName.orEmpty(),
            email = firebaseUser.email.orEmpty(),
        )
        return try {
            val doc = usersCollection().document(firebaseUser.uid).get().await()
            if (doc.exists()) {
                doc.toUser() ?: fallback
            } else {
                usersCollection().document(firebaseUser.uid).set(fallback.toFirestore()).await()
                fallback
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't load users/${firebaseUser.uid}; using Auth profile", e)
            fallback
        }
    }

    private fun domainError(email: String): String? =
        if (AuthValidator.isAllowedDomain(email)) null else "Use your college email (@${AuthValidator.allowedEmailDomains.first()})."

    /** Runs a Firebase call and turns its exceptions into messages a student can act on. */
    private suspend fun safeAuthCall(block: suspend () -> AuthResult): AuthResult = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Auth call failed", e)
        AuthResult.Error(messageFor(e))
    }

    private fun messageFor(e: Exception): String = when (e) {
        is FirebaseAuthWeakPasswordException -> "That password is too weak. Use at least 6 characters."
        is FirebaseAuthUserCollisionException -> "An account with that email already exists. Try Login."
        is FirebaseAuthInvalidUserException -> "No account found for that email. Try Sign Up."
        // Newer Firebase versions report wrong passwords and unknown emails the same way, on purpose.
        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
        is FirebaseNetworkException -> "No internet connection. Check your connection and try again."
        is FirebaseAuthException ->
            if (e.errorCode == "ERROR_OPERATION_NOT_ALLOWED") "Email sign-in isn't enabled for this app yet." else GENERIC_ERROR
        else -> GENERIC_ERROR
    }

    private fun User.toFirestore(): Map<String, Any?> = mapOf(
        FIELD_NAME to name,
        FIELD_EMAIL to email,
        FIELD_PROGRAM to program,
        FIELD_COURSES to enrolledCourses,
        FIELD_PHOTO to profilePhotoUrl,
        FIELD_CREATED to createdAtMillis,
    )

    private fun DocumentSnapshot.toUser(): User? {
        val email = getString(FIELD_EMAIL) ?: return null
        val courses = (get(FIELD_COURSES) as? List<*>)?.filterIsInstance<String>().orEmpty()
        return User(
            userId = id,
            name = getString(FIELD_NAME).orEmpty(),
            email = email,
            program = getString(FIELD_PROGRAM).orEmpty(),
            enrolledCourses = courses,
            profilePhotoUrl = getString(FIELD_PHOTO),
            createdAtMillis = getLong(FIELD_CREATED) ?: System.currentTimeMillis(),
        )
    }

    companion object {
        private const val TAG = "FirebaseAuthRepository"
        private const val GENERIC_ERROR = "Something went wrong. Please try again."

        // Firestore layout (proposal Section 7). Keep these in sync with firestore.rules.
        const val USERS = "users"
        const val FIELD_NAME = "name"
        const val FIELD_EMAIL = "email"
        const val FIELD_PROGRAM = "program"
        const val FIELD_COURSES = "enrolledCourses"
        const val FIELD_PHOTO = "profilePhotoUrl"
        const val FIELD_CREATED = "createdAtMillis"
    }
}
