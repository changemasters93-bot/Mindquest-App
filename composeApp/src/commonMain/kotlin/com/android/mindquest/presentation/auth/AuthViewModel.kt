package com.android.mindquest.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mindquest.core.util.UiState
import com.android.mindquest.domain.model.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthScreenState {
    MAIN,
    PHONE,
    OTP,
    VERIFIED,
    LINKING,
}

class AuthViewModel : ViewModel() {

    private val _authState = MutableStateFlow<UiState<User?>>(UiState.Empty)
    val authState: StateFlow<UiState<User?>> = _authState.asStateFlow()

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _otpCode = MutableStateFlow("")
    val otpCode: StateFlow<String> = _otpCode.asStateFlow()

    private val _authScreen = MutableStateFlow(AuthScreenState.MAIN)
    val authScreen: StateFlow<AuthScreenState> = _authScreen.asStateFlow()

    private val _linkReason = MutableStateFlow("general")
    val linkReason: StateFlow<String> = _linkReason.asStateFlow()

    private val _isOtpSent = MutableStateFlow(false)
    val isOtpSent: StateFlow<Boolean> = _isOtpSent.asStateFlow()

    private val _resendTimer = MutableStateFlow(0)
    val resendTimer: StateFlow<Int> = _resendTimer.asStateFlow()

    private val _isLinkingSheetVisible = MutableStateFlow(false)
    val isLinkingSheetVisible: StateFlow<Boolean> = _isLinkingSheetVisible.asStateFlow()

    fun updatePhoneNumber(phone: String) {
        _phoneNumber.update { phone }
    }

    fun updateOtpCode(code: String) {
        if (code.length <= 6) {
            _otpCode.update { code }
        }
    }

    fun navigateToPhone() {
        _authScreen.update { AuthScreenState.PHONE }
    }

    fun navigateToMain() {
        _authScreen.update { AuthScreenState.MAIN }
        _phoneNumber.update { "" }
        _otpCode.update { "" }
        _isOtpSent.update { false }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            _authState.update { UiState.Loading }
            try {
                // TODO: Integrate with actual Google Sign-In
                delay(1500)
                _authState.update { UiState.Success(null) }
            } catch (e: Exception) {
                _authState.update { UiState.Error(e.message ?: "Google sign-in failed") }
            }
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            _authState.update { UiState.Loading }
            try {
                // TODO: Integrate with actual anonymous sign-in
                delay(1000)
                _authState.update { UiState.Success(null) }
            } catch (e: Exception) {
                _authState.update { UiState.Error(e.message ?: "Anonymous sign-in failed") }
            }
        }
    }

    fun sendOtp(phone: String) {
        viewModelScope.launch {
            _authState.update { UiState.Loading }
            try {
                // TODO: Integrate with actual OTP sending
                delay(1000)
                _isOtpSent.update { true }
                _authScreen.update { AuthScreenState.OTP }
                _authState.update { UiState.Empty }
                startResendTimer()
            } catch (e: Exception) {
                _authState.update { UiState.Error(e.message ?: "Failed to send OTP") }
            }
        }
    }

    fun verifyOtp(code: String) {
        viewModelScope.launch {
            _authState.update { UiState.Loading }
            try {
                // TODO: Integrate with actual OTP verification
                delay(1500)
                _authScreen.update { AuthScreenState.VERIFIED }
                _authState.update { UiState.Success(null) }
            } catch (e: Exception) {
                _authState.update { UiState.Error(e.message ?: "OTP verification failed") }
            }
        }
    }

    fun resendOtp() {
        if (_resendTimer.value == 0) {
            sendOtp(_phoneNumber.value)
        }
    }

    private fun startResendTimer() {
        viewModelScope.launch {
            _resendTimer.update { 30 }
            while (_resendTimer.value > 0) {
                delay(1000)
                _resendTimer.update { it - 1 }
            }
        }
    }

    fun showLinkingSheet(reason: String = "general") {
        _linkReason.update { reason }
        _isLinkingSheetVisible.update { true }
    }

    fun hideLinkingSheet() {
        _isLinkingSheetVisible.update { false }
    }

    fun linkAccount(provider: String) {
        viewModelScope.launch {
            _authState.update { UiState.Loading }
            try {
                // TODO: Integrate with actual account linking
                delay(1500)
                _isLinkingSheetVisible.update { false }
                _authState.update { UiState.Success(null) }
            } catch (e: Exception) {
                _authState.update { UiState.Error(e.message ?: "Account linking failed") }
            }
        }
    }

    fun clearError() {
        _authState.update { UiState.Empty }
    }
}
