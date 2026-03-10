package com.android.mindquest.presentation.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mindquest.core.util.Resource
import com.android.mindquest.core.util.UiState
import com.android.mindquest.domain.model.LeaderboardData
import com.android.mindquest.domain.model.LeaderboardFilter
import com.android.mindquest.domain.usecase.GetLeaderboardUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LeaderboardViewModel(
    private val getLeaderboard: GetLeaderboardUseCase,
) : ViewModel() {

    private val _leaderboardState =
        MutableStateFlow<UiState<LeaderboardData>>(UiState.Loading)
    val leaderboardState: StateFlow<UiState<LeaderboardData>> = _leaderboardState.asStateFlow()

    /** 0 = Country, 1 = City, 2 = School */
    private val _activeFilterIndex = MutableStateFlow(0)
    val activeFilterIndex: StateFlow<Int> = _activeFilterIndex.asStateFlow()

    val activeFilter: LeaderboardFilter
        get() = indexToFilter(_activeFilterIndex.value)

    // ── Public API ───────────────────────────────────────────────────────

    fun loadLeaderboard(userId: String, filter: LeaderboardFilter, filterId: String? = null) {
        viewModelScope.launch {
            _leaderboardState.value = UiState.Loading
            when (val result = getLeaderboard(userId, filter, filterId)) {
                is Resource.Success -> {
                    val data = result.data
                    if (data.rankedUsers.isEmpty()) {
                        _leaderboardState.value = UiState.Empty
                    } else {
                        _leaderboardState.value = UiState.Success(data)
                    }
                }
                is Resource.Error -> {
                    _leaderboardState.value = UiState.Error(result.message)
                }
                is Resource.Loading -> { /* no-op */ }
            }
        }
    }

    fun setFilter(index: Int) {
        _activeFilterIndex.value = index.coerceIn(0, 2)
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun indexToFilter(index: Int): LeaderboardFilter = when (index) {
        0 -> LeaderboardFilter.COUNTRY
        1 -> LeaderboardFilter.CITY
        2 -> LeaderboardFilter.SCHOOL
        else -> LeaderboardFilter.COUNTRY
    }

    companion object {
        val FILTER_LABELS = listOf("Country", "City", "School")
        val FILTER_EMOJIS = listOf("\uD83C\uDF0D", "\uD83C\uDFD9\uFE0F", "\uD83C\uDF93")
    }
}
