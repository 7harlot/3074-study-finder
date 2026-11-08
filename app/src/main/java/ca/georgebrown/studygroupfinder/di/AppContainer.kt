package ca.georgebrown.studygroupfinder.di

import android.content.Context
import ca.georgebrown.studygroupfinder.data.repository.AuthRepository
import ca.georgebrown.studygroupfinder.data.repository.FakeSessionRepository
import ca.georgebrown.studygroupfinder.data.repository.FirebaseAuthRepository
import ca.georgebrown.studygroupfinder.data.repository.LocalAuthRepository
import ca.georgebrown.studygroupfinder.data.repository.SessionRepository

/**
 * Manual dependency injection: the one place that decides which implementation each screen gets.
 * Screens only see the interfaces, so swapping an implementation never touches UI code.
 */
class AppContainer(context: Context) {

    val authRepository: AuthRepository =
        if (USE_FIREBASE_AUTH) FirebaseAuthRepository() else LocalAuthRepository(context.applicationContext)

    // TODO(Munir): swap for FirestoreSessionRepository() once it implements SessionRepository.
    val sessionRepository: SessionRepository = FakeSessionRepository()

    private companion object {
        /**
         * true = real Firebase accounts (needs Email/Password enabled in the Firebase console).
         * false = the offline prototype accounts, incl. the demo login, for demos without internet.
         */
        const val USE_FIREBASE_AUTH = true
    }
}
