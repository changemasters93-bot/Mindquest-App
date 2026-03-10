package com.android.mindquest.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mindquest.core.util.Resource
import com.android.mindquest.core.util.UiState
import com.android.mindquest.domain.model.DailyChallenge
import com.android.mindquest.domain.model.DashboardData
import com.android.mindquest.domain.usecase.GetDashboardUseCase
import com.android.mindquest.domain.usecase.GetDailyChallengesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getDashboard: GetDashboardUseCase,
    private val getDailyChallenges: GetDailyChallengesUseCase,
) : ViewModel() {

    private val _dashboardState = MutableStateFlow<UiState<DashboardData>>(UiState.Loading)
    val dashboardState: StateFlow<UiState<DashboardData>> = _dashboardState.asStateFlow()

    private val _dailyChallenges = MutableStateFlow<UiState<List<DailyChallenge>>>(UiState.Loading)
    val dailyChallenges: StateFlow<UiState<List<DailyChallenge>>> = _dailyChallenges.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // TODO: Replace with actual userId from auth session
    private val userId: String = "current_user"

    init {
        loadDashboard()
        loadChallenges()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _dashboardState.update { UiState.Loading }
            when (val result = getDashboard(userId)) {
                is Resource.Success -> _dashboardState.update { UiState.Success(result.data) }
                is Resource.Error -> _dashboardState.update { UiState.Error(result.message) }
                is Resource.Loading -> { /* no-op */ }
            }
        }
    }

    fun loadChallenges() {
        viewModelScope.launch {
            _dailyChallenges.update { UiState.Loading }
            when (val result = getDailyChallenges(userId)) {
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
    }

    fun refreshAll() {
        viewModelScope.launch {
            _isRefreshing.update { true }
            loadDashboard()
            loadChallenges()
            _isRefreshing.update { false }
        }
    }
}
