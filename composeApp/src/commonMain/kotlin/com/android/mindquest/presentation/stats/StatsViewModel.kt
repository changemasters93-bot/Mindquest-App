package com.android.mindquest.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mindquest.core.util.Resource
import com.android.mindquest.core.util.UiState
import com.android.mindquest.domain.model.StatsData
import com.android.mindquest.domain.usecase.GetUserStatsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StatsViewModel(
    private val getUserStats: GetUserStatsUseCase,
) : ViewModel() {

    private val _statsState = MutableStateFlow<UiState<StatsData>>(UiState.Loading)
    val statsState: StateFlow<UiState<StatsData>> = _statsState.asStateFlow()

    fun loadStats(userId: String) {
        viewModelScope.launch {
            _statsState.value = UiState.Loading
            when (val result = getUserStats(userId, "all_time")) {
                is Resource.Success -> _statsState.value = UiState.Success(result.data)
                is Resource.Error -> _statsState.value = UiState.Error(result.message)
                is Resource.Loading -> { /* no-op */ }
            }
        }
    }
}
