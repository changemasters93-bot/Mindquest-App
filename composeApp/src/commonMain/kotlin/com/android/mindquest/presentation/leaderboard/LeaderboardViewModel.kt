package com.android.mindquest.presentation.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mindquest.core.util.AppLogger
import com.android.mindquest.core.util.Resource
import com.android.mindquest.core.util.SnackbarManager
import com.android.mindquest.core.analytics.AnalyticsTracker
import com.android.mindquest.core.util.UiState
import com.android.mindquest.domain.model.LeaderboardData
import com.android.mindquest.domain.model.LeaderboardFilter
import com.android.mindquest.domain.usecase.GetLeaderboardUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

class LeaderboardViewModel(
    private val getLeaderboard: GetLeaderboardUseCase,
    private val snackbarManager: SnackbarManager,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        AppLogger.e("LeaderboardViewModel", "Unhandled coroutine exception", throwable as? Exception)
    }

    private val _leaderboardState =
        MutableStateFlow<UiState<LeaderboardData>>(UiState.Loading)
    val leaderboardState: StateFlow<UiState<LeaderboardData>> = _leaderboardState.asStateFlow()

    /** 0 = Global, 1 = Country, 2 = City */
    private val _activeFilterIndex = MutableStateFlow(0)
    val activeFilterIndex: StateFlow<Int> = _activeFilterIndex.asStateFlow()

    val activeFilter: LeaderboardFilter
        get() = indexToFilter(_activeFilterIndex.value)

    // ── Pagination state ─────────────────────────────────────────────────

    private var currentOffset = 0
    var hasMorePages = true
        private set

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    /** Remembers the last userId / filterId so loadMore can re-use them. */
    private var lastUserId: String = ""
    private var lastFilterId: String? = null

    // ── Public API ───────────────────────────────────────────────────────

    fun loadLeaderboard(userId: String, filter: LeaderboardFilter, filterId: String? = null) {
        lastUserId = userId
        lastFilterId = filterId

        // Reset pagination
        currentOffset = 0
        hasMorePages = true

        viewModelScope.launch(exceptionHandler) {
            _leaderboardState.value = UiState.Loading
            when (val result = getLeaderboard(userId, filter, filterId, limit = PAGE_SIZE, offset = 0)) {
                is Resource.Success -> {
                    val data = result.data
                    hasMorePages = data.rankedUsers.size == PAGE_SIZE
                    if (data.rankedUsers.isEmpty()) {
                        _leaderboardState.value = UiState.Empty
                    } else {
                        currentOffset = data.rankedUsers.size
                        _leaderboardState.value = UiState.Success(data)
                    }
                }
                is Resource.Error -> {
                    _leaderboardState.value = UiState.Error(result.message)
                    snackbarManager.showError("Failed to load leaderboard.")
                }
                is Resource.Loading -> { /* no-op */ }
            }
        }
    }

    fun loadMoreLeaderboard(userId: String) {
        if (_isLoadingMore.value || !hasMorePages) return

        val currentData = (_leaderboardState.value as? UiState.Success)?.data ?: return

        viewModelScope.launch(exceptionHandler) {
            _isLoadingMore.value = true
            when (val result = getLeaderboard(
                userId = userId,
                filter = activeFilter,
                filterId = lastFilterId,
                limit = PAGE_SIZE,
                offset = currentOffset,
            )) {
                is Resource.Success -> {
                    val newEntries = result.data.rankedUsers
                    hasMorePages = newEntries.size == PAGE_SIZE
                    if (newEntries.isNotEmpty()) {
                        currentOffset += newEntries.size
                        val merged = currentData.copy(
                            rankedUsers = currentData.rankedUsers + newEntries,
                            userRank = result.data.userRank,
                        )
                        _leaderboardState.value = UiState.Success(merged)
                    }
                }
                is Resource.Error -> {
                    snackbarManager.showError("Failed to load more entries.")
                }
                is Resource.Loading -> { /* no-op */ }
            }
            _isLoadingMore.value = false
        }
    }

    fun setFilter(index: Int) {
        _activeFilterIndex.value = index.coerceIn(0, 2)
        // Reset pagination when filter changes
        currentOffset = 0
        hasMorePages = true
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun indexToFilter(index: Int): LeaderboardFilter = when (index) {
        0 -> LeaderboardFilter.GLOBAL
        1 -> LeaderboardFilter.COUNTRY
        2 -> LeaderboardFilter.CITY
        else -> LeaderboardFilter.GLOBAL
    }

    companion object {
        const val PAGE_SIZE = 30
        val FILTER_LABELS = listOf("Global", "Country", "City")
        val FILTER_EMOJIS = listOf("\uD83C\uDF10", "\uD83C\uDDEE\uD83C\uDDF3", "\uD83C\uDFD9\uFE0F")
    }
}
