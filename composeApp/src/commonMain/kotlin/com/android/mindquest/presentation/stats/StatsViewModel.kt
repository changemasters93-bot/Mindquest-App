package com.android.mindquest.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mindquest.core.util.AppLogger
import com.android.mindquest.core.util.Resource
import com.android.mindquest.core.util.SnackbarManager
import com.android.mindquest.core.util.UiState
import com.android.mindquest.domain.model.StatsData
import com.android.mindquest.domain.usecase.GetUserStatsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

class StatsViewModel(
    private val getUserStats: GetUserStatsUseCase,
    private val snackbarManager: SnackbarManager,
) : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        AppLogger.e("StatsViewModel", "Unhandled coroutine exception", throwable as? Exception)
    }

    private val _statsState = MutableStateFlow<UiState<StatsData>>(UiState.Loading)
    val statsState: StateFlow<UiState<StatsData>> = _statsState.asStateFlow()

    private val _activePeriod = MutableStateFlow("last_week")
    val activePeriod: StateFlow<String> = _activePeriod.asStateFlow()

    /** True while the activity card is reloading after a period switch. */
    private val _activityLoading = MutableStateFlow(false)
    val activityLoading: StateFlow<Boolean> = _activityLoading.asStateFlow()

    private var currentUserId: String = ""

    fun loadStats(userId: String) {
        currentUserId = userId
        loadStatsForPeriod(userId, _activePeriod.value)
    }

    /**
     * Switches the activity period and reloads **only** the daily-activity
     * data — the rest of the page stays visible; no full-screen spinner.
     */
    fun changePeriod(period: String) {
        _activePeriod.value = period
        if (currentUserId.isNotEmpty()) {
            viewModelScope.launch(exceptionHandler) {
                _activityLoading.value = true
                when (val result = getUserStats(currentUserId, period)) {
                    is Resource.Success -> {
                        // Merge: only replace dailyActivity, keep other stats intact
                        val current = (_statsState.value as? UiState.Success)?.data
                        if (current != null) {
                            _statsState.value = UiState.Success(
                                current.copy(dailyActivity = result.data.dailyActivity),
                            )
                        } else {
                            _statsState.value = UiState.Success(result.data)
                        }
                    }
                    is Resource.Error -> { /* Keep existing data visible */ }
                    is Resource.Loading -> { /* no-op */ }
                }
                _activityLoading.value = false
            }
        }
    }

    private fun loadStatsForPeriod(userId: String, period: String) {
        viewModelScope.launch(exceptionHandler) {
            _statsState.value = UiState.Loading
            when (val result = getUserStats(userId, period)) {
                is Resource.Success -> _statsState.value = UiState.Success(result.data)
                is Resource.Error -> {
                    _statsState.value = UiState.Error(result.message)
                    snackbarManager.showError("Failed to load stats. Please try again.")
                }
                is Resource.Loading -> { /* no-op */ }
            }
        }
    }
}
