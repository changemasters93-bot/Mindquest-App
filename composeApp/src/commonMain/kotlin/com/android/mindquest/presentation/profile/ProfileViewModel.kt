package com.android.mindquest.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mindquest.core.util.Resource
import com.android.mindquest.core.util.UiState
import com.android.mindquest.domain.model.ProfileData
import com.android.mindquest.domain.usecase.GetProfileUseCase
import com.android.mindquest.domain.usecase.UpdateProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val getProfile: GetProfileUseCase,
    private val updateProfile: UpdateProfileUseCase,
) : ViewModel() {

    private val _profileState = MutableStateFlow<UiState<ProfileData>>(UiState.Loading)
    val profileState: StateFlow<UiState<ProfileData>> = _profileState.asStateFlow()

    private val _updateState = MutableStateFlow<UiState<Unit>?>(null)
    val updateState: StateFlow<UiState<Unit>?> = _updateState.asStateFlow()

    fun loadProfile(userId: String) {
        viewModelScope.launch {
            _profileState.value = UiState.Loading
            when (val result = getProfile(userId)) {
                is Resource.Success -> _profileState.value = UiState.Success(result.data)
                is Resource.Error -> _profileState.value = UiState.Error(result.message)
                is Resource.Loading -> { /* no-op */ }
            }
        }
    }

    fun updateDisplayName(userId: String, newName: String) {
        viewModelScope.launch {
            _updateState.value = UiState.Loading
            when (val result = updateProfile(userId, mapOf("display_name" to newName))) {
                is Resource.Success -> {
                    _updateState.value = UiState.Success(Unit)
                    loadProfile(userId)
                }
                is Resource.Error -> _updateState.value = UiState.Error(result.message)
                is Resource.Loading -> { /* no-op */ }
            }
        }
    }

    fun updateAvatar(userId: String, avatarId: Int) {
        viewModelScope.launch {
            _updateState.value = UiState.Loading
            when (val result = updateProfile(userId, mapOf("avatar_id" to avatarId))) {
                is Resource.Success -> {
                    _updateState.value = UiState.Success(Unit)
                    loadProfile(userId)
                }
                is Resource.Error -> _updateState.value = UiState.Error(result.message)
                is Resource.Loading -> { /* no-op */ }
            }
        }
    }
}
