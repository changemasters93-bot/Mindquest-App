package com.android.mindquest.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mindquest.core.prefs.SessionPrefs
import com.android.mindquest.core.util.AppLogger
import com.android.mindquest.core.util.Resource
import com.android.mindquest.core.util.SnackbarManager
import com.android.mindquest.core.analytics.AnalyticsEvent
import com.android.mindquest.core.analytics.AnalyticsTracker
import com.android.mindquest.core.util.UiState
import com.android.mindquest.domain.model.ProfileData
import com.android.mindquest.domain.repository.AuthRepository
import com.android.mindquest.domain.usecase.GetProfileUseCase
import com.android.mindquest.domain.usecase.UpdateProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val getProfile: GetProfileUseCase,
    private val updateProfile: UpdateProfileUseCase,
    private val authRepository: AuthRepository,
    private val sessionPrefs: SessionPrefs,
    private val snackbarManager: SnackbarManager,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        AppLogger.e("ProfileViewModel", "Unhandled coroutine exception", throwable as? Exception)
    }

    private val _profileState = MutableStateFlow<UiState<ProfileData>>(UiState.Loading)
    val profileState: StateFlow<UiState<ProfileData>> = _profileState.asStateFlow()

    private val _updateState = MutableStateFlow<UiState<Unit>?>(null)
    val updateState: StateFlow<UiState<Unit>?> = _updateState.asStateFlow()

    fun loadProfile(userId: String) {
        viewModelScope.launch(exceptionHandler) {
            _profileState.value = UiState.Loading
            when (val result = getProfile(userId)) {
                is Resource.Success -> _profileState.value = UiState.Success(result.data)
                is Resource.Error -> {
                    _profileState.value = UiState.Error(result.message)
                    snackbarManager.showError("Failed to load profile. Please try again.")
                }
                is Resource.Loading -> { /* no-op */ }
            }
        }
    }

    fun updateDisplayName(userId: String, newName: String) {
        viewModelScope.launch(exceptionHandler) {
            _updateState.value = UiState.Loading
            when (val result = updateProfile(userId, mapOf("display_name" to newName))) {
                is Resource.Success -> {
                    _updateState.value = UiState.Success(Unit)
                    snackbarManager.showSuccess("Display name updated!")
                    analyticsTracker.logEvent(AnalyticsEvent.ProfileUpdated(fieldsChanged = listOf("display_name")))
                    loadProfile(userId)
                }
                is Resource.Error -> {
                    _updateState.value = UiState.Error(result.message)
                    snackbarManager.showError("Failed to update name. Please try again.")
                }
                is Resource.Loading -> { /* no-op */ }
            }
        }
    }

    fun updateAvatar(userId: String, avatarId: Int) {
        viewModelScope.launch(exceptionHandler) {
            _updateState.value = UiState.Loading
            when (val result = updateProfile(userId, mapOf("avatar_id" to avatarId))) {
                is Resource.Success -> {
                    _updateState.value = UiState.Success(Unit)
                    snackbarManager.showSuccess("Avatar updated!")
                    analyticsTracker.logEvent(AnalyticsEvent.ProfileUpdated(fieldsChanged = listOf("avatar_id")))
                    loadProfile(userId)
                }
                is Resource.Error -> {
                    _updateState.value = UiState.Error(result.message)
                    snackbarManager.showError("Failed to update avatar. Please try again.")
                }
                is Resource.Loading -> { /* no-op */ }
            }
        }
    }

    /** Clears persisted session and signs out from backend. */
    fun signOut() {
        viewModelScope.launch(exceptionHandler) {
            try { authRepository.signOut() } catch (_: Exception) { /* best-effort */ }
            sessionPrefs.clear()
            analyticsTracker.logEvent(AnalyticsEvent.Logout)
            analyticsTracker.setUserId(null)
        }
    }
}
