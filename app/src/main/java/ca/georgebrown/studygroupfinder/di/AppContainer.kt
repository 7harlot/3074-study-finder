package ca.georgebrown.studygroupfinder.di

import android.content.Context
import ca.georgebrown.studygroupfinder.data.repository.AuthRepository
import ca.georgebrown.studygroupfinder.data.repository.FakeSessionRepository
import ca.georgebrown.studygroupfinder.data.repository.LocalAuthRepository
import ca.georgebrown.studygroupfinder.data.repository.SessionRepository

/**
 * Manual dependency injection: one place that decides which implementation each screen gets.
 * When Firebase is added, only these two lines change. (Hilt can replace this later if the team wants.)
 */
class AppContainer(context: Context) {
    val authRepository: AuthRepository = LocalAuthRepository(context.applicationContext)
    val sessionRepository: SessionRepository = FakeSessionRepository()
}
