package com.android.mindquest.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mindquest.core.session.SessionProvider
import com.android.mindquest.core.util.AppLogger
import com.android.mindquest.core.util.Resource
import com.android.mindquest.core.util.SnackbarManager
import com.android.mindquest.core.util.UiState
import com.android.mindquest.domain.model.DailyChallenge
import com.android.mindquest.domain.model.DashboardData
import com.android.mindquest.domain.model.Module
import com.android.mindquest.domain.usecase.GenerateDailyChallengesUseCase
import com.android.mindquest.domain.usecase.GetDashboardUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getDashboard: GetDashboardUseCase,
    private val generateDailyChallenges: GenerateDailyChallengesUseCase,
    private val sessionProvider: SessionProvider,
    private val snackbarManager: SnackbarManager,
) : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        AppLogger.e("HomeViewModel", "Unhandled coroutine exception", throwable as? Exception)
    }

    private val _dashboardState = MutableStateFlow<UiState<DashboardData>>(UiState.Loading)
    val dashboardState: StateFlow<UiState<DashboardData>> = _dashboardState.asStateFlow()

    private val _dailyChallenges = MutableStateFlow<UiState<List<DailyChallenge>>>(UiState.Loading)
    val dailyChallenges: StateFlow<UiState<List<DailyChallenge>>> = _dailyChallenges.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val userId: String get() = sessionProvider.userId

    init {
        loadAll()
    }

    /** Load dashboard, then generate daily challenges from modules. */
    private fun loadAll() {
        viewModelScope.launch(exceptionHandler) {
            _dashboardState.update { UiState.Loading }

            // Wait for session to be ready (userId might be empty right after sign-in)
            var currentUserId = userId
            if (currentUserId.isBlank()) {
                AppLogger.d("MQ_HOME", "loadAll: userId is EMPTY — waiting for session (max 5s)...")
                repeat(10) {
                    delay(500)
                    currentUserId = userId
                    if (currentUserId.isNotBlank()) return@repeat
                }
            }

            if (currentUserId.isBlank()) {
                AppLogger.e("MQ_HOME", "loadAll: userId still EMPTY after wait — aborting")
                _dashboardState.update { UiState.Error("Session not ready. Please try again.") }
                snackbarManager.showError("Session not ready. Please try again.")
                return@launch
            }

            AppLogger.d("MQ_HOME", "loadAll: loading dashboard for userId=${currentUserId.take(8)}...")
            // 1. Load dashboard
            when (val result = getDashboard(currentUserId)) {
                is Resource.Success -> {
                    AppLogger.d("MQ_HOME", "loadAll: dashboard SUCCESS")
                    _dashboardState.update { UiState.Success(result.data) }
                    // 2. Chain: generate daily challenges from loaded modules
                    generateChallenges(result.data.modules)
                }
                is Resource.Error -> {
                    AppLogger.e("MQ_HOME", "loadAll: dashboard ERROR = ${result.message}")
                    _dashboardState.update { UiState.Error(result.message) }
                    _dailyChallenges.update { UiState.Empty }
                    snackbarManager.showError("Failed to load dashboard. Pull down to retry.")
                }
                is Resource.Loading -> { /* no-op */ }
            }
        }
    }

    /** Generate daily challenges client-side from the given modules. */
    private suspend fun generateChallenges(modules: List<Module>) {
        _dailyChallenges.update { UiState.Loading }
        when (val result = generateDailyChallenges(modules, userId)) {
            is Resource.Success -> {
                if (result.data.isEmpty()) {
                    _dailyChallenges.update { UiState.Empty }
                } else {
                    _dailyChallenges.update { UiState.Success(result.data) }
                }
            }
            is Resource.Error -> _dailyChallenges.update { UiState.Error(result.message) }
            is Resource.Loading -> { /* no-op */ }
        }
    }

    /** Refresh dashboard + daily challenges (used by pull-to-refresh & needsRefresh). */
    fun refreshAll() {
        viewModelScope.launch(exceptionHandler) {
            _isRefreshing.update { true }

            _dashboardState.update { UiState.Loading }
            when (val result = getDashboard(userId)) {
                is Resource.Success -> {
                    _dashboardState.update { UiState.Success(result.data) }
                    generateChallenges(result.data.modules)
                }
                is Resource.Error -> {
                    _dashboardState.update { UiState.Error(result.message) }
                    _dailyChallenges.update { UiState.Empty }
                    snackbarManager.showError("Refresh failed. Please try again.")
                }
                is Resource.Loading -> { /* no-op */ }
            }

            _isRefreshing.update { false }
        }
    }

    /**
     * Mark a daily challenge as done locally (no API call).
     * Called when a quiz is completed — from either daily-mission or chapter listing.
     */
    fun markChallengeDone(quizId: String) {
        val updated = generateDailyChallenges.markDone(quizId)
        if (updated != null) {
            AppLogger.d("MQ_AUTH", "HomeVM: markChallengeDone($quizId) → ${updated.count { it.isDone }}/${updated.size} done")
            _dailyChallenges.update {
                if (updated.isEmpty()) UiState.Empty else UiState.Success(updated)
            }
        } else {
            AppLogger.d("MQ_AUTH", "HomeVM: markChallengeDone($quizId) — no cached challenges found")
        }
    }
}
