package ca.georgebrown.studygroupfinder.ui.mysessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ca.georgebrown.studygroupfinder.data.model.StudySession
import ca.georgebrown.studygroupfinder.data.repository.AuthRepository
import ca.georgebrown.studygroupfinder.data.repository.SessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

enum class MySessionsTab { JOINED, CREATED }

data class MySessionsUiState(
    val isLoading: Boolean = true,
    val tab: MySessionsTab = MySessionsTab.JOINED,
    val joined: List<StudySession> = emptyList(),
    val created: List<StudySession> = emptyList(),
    val errorMessage: String? = null,
) {
    val visible: List<StudySession> get() = if (tab == MySessionsTab.JOINED) joined else created
}

/**
 * My Sessions (FR-10): live Joined and Created lists for whoever is signed in.
 *
 * Follows the signed-in user, so signing in as someone else swaps the lists automatically.
 * Uses [SessionRepository.sessionsJoinedBy]/[SessionRepository.sessionsCreatedBy], which become
 * Firestore queries once FirestoreSessionRepository lands. No change needed here.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MySessionsViewModel(
    sessionRepository: SessionRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val selectedTab = MutableStateFlow(MySessionsTab.JOINED)

    private val lists = authRepository.currentUser.flatMapLatest { user ->
        if (user == null) {
            flowOf(Lists(emptyList(), emptyList()))
        } else {
            combine(sessionRepository.sessionsJoinedBy(user.userId), sessionRepository.sessionsCreatedBy(user.userId)) { joined, created ->
                Lists(joined, created)
            }
        }
    }.catch { emit(Lists(emptyList(), emptyList(), error = "Couldn't load your sessions. Leave and reopen this tab to retry.")) }

    val uiState: StateFlow<MySessionsUiState> = combine(lists, selectedTab) { lists, tab ->
        MySessionsUiState(isLoading = false, tab = tab, joined = lists.joined, created = lists.created, errorMessage = lists.error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MySessionsUiState())

    fun selectTab(tab: MySessionsTab) {
        selectedTab.value = tab
    }

    private data class Lists(val joined: List<StudySession>, val created: List<StudySession>, val error: String? = null)

    companion object {
        fun factory(sessionRepository: SessionRepository, authRepository: AuthRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { MySessionsViewModel(sessionRepository, authRepository) } }
    }
}
