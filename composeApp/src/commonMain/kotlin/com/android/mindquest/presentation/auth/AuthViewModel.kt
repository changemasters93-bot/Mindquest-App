package com.android.mindquest.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mindquest.core.constants.AppConstants
import com.android.mindquest.core.prefs.SessionPrefs
import com.android.mindquest.core.util.AppLogger
import com.android.mindquest.core.util.Resource
import com.android.mindquest.core.util.UiState
import com.android.mindquest.domain.model.City
import com.android.mindquest.domain.model.Country
import com.android.mindquest.domain.model.Grade
import com.android.mindquest.domain.model.User
import com.android.mindquest.domain.repository.AuthRepository
import com.android.mindquest.domain.repository.ReferenceDataRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

enum class AuthScreenState {
    MAIN,
    PHONE,
    OTP,
    VERIFIED,
    LINKING,
}

enum class SessionCheck { CHECKING, LOGGED_IN, NOT_LOGGED_IN }

/**
 * Profile data collected during onboarding (Steps 1 & 2).
 * Passed to auth methods so the ViewModel can create the user row after sign-in.
 */
data class OnboardingProfile(
    val displayName: String,
    val avatarId: Int,
    val gradeId: String,
    val countryId: String? = null,
    val cityId: String? = null,
    val schoolName: String? = null,
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val referenceDataRepository: ReferenceDataRepository,
    private val sessionPrefs: SessionPrefs,
) : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        AppLogger.e("AuthViewModel", "Unhandled coroutine exception", throwable as? Exception)
    }

    private val _sessionCheck = MutableStateFlow(SessionCheck.CHECKING)
    val sessionCheck: StateFlow<SessionCheck> = _sessionCheck.asStateFlow()

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

    /** Grades fetched from backend — used by onboarding STEP2 to map labels → IDs. */
    private val _grades = MutableStateFlow<List<Grade>>(emptyList())
    val grades: StateFlow<List<Grade>> = _grades.asStateFlow()

    /** Countries fetched from backend — used by onboarding STEP1. */
    private val _countries = MutableStateFlow<List<Country>>(emptyList())
    val countries: StateFlow<List<Country>> = _countries.asStateFlow()

    /** Cities for the currently selected country. */
    private val _cities = MutableStateFlow<List<City>>(emptyList())
    val cities: StateFlow<List<City>> = _cities.asStateFlow()

    /**
     * Temporarily stores the onboarding profile so it's available after
     * OTP verification (since Phone auth has STEP3 → PHONE_AUTH navigation).
     */
    private var pendingProfile: OnboardingProfile? = null

    init {
        loadGrades()
        loadCountries()
    }

    // ── Session check ────────────────────────────────────────────────────

    fun checkSession() {
        viewModelScope.launch(exceptionHandler) {
            try {
                if (AppConstants.USE_MOCK_DATA) {
                    _sessionCheck.update {
                        if (sessionPrefs.isLoggedIn) SessionCheck.LOGGED_IN
                        else SessionCheck.NOT_LOGGED_IN
                    }
                    return@launch
                }
                // Restore session: both anonymous and identified users that
                // completed onboarding (isLoggedIn == true) should stay logged in.
                val user = authRepository.getCurrentUser()
                _sessionCheck.update {
                    if (user != null && sessionPrefs.isLoggedIn) SessionCheck.LOGGED_IN
                    else if (user != null && !user.isAnonymous) SessionCheck.LOGGED_IN
                    else SessionCheck.NOT_LOGGED_IN
                }
            } catch (_: Exception) {
                _sessionCheck.update { SessionCheck.NOT_LOGGED_IN }
            }
        }
    }

    // ── Reference data ───────────────────────────────────────────────────

    fun loadGrades() {
        viewModelScope.launch(exceptionHandler) {
            try {
                AppLogger.d("MQ_AUTH", "loadGrades() starting...")
                when (val result = referenceDataRepository.getGrades()) {
                    is Resource.Success -> {
                        AppLogger.d("MQ_AUTH", "loadGrades() SUCCESS: ${result.data.size} grades loaded: ${result.data.map { "${it.label}→${it.id}" }}")
                        _grades.update { result.data }
                    }
                    is Resource.Error -> {
                        AppLogger.e("MQ_AUTH", "loadGrades() ERROR: ${result.message}")
                    }
                    is Resource.Loading -> { /* no-op */ }
                }
            } catch (e: Exception) {
                AppLogger.e("MQ_AUTH", "loadGrades() EXCEPTION", e)
            }
        }
    }

    /** Map a grade label (e.g. "Grade 5") to its backend UUID using loaded grades. */
    fun resolveGradeId(label: String): String {
        val resolved = _grades.value.find { it.label == label }?.id ?: ""
        AppLogger.d("MQ_AUTH", "resolveGradeId('$label') → '$resolved' (grades count=${_grades.value.size})")
        return resolved
    }

    // ── Country / City ──────────────────────────────────────────────────

    fun loadCountries() {
        viewModelScope.launch(exceptionHandler) {
            try {
                AppLogger.d("MQ_AUTH", "loadCountries() starting...")
                when (val result = referenceDataRepository.getCountries()) {
                    is Resource.Success -> {
                        AppLogger.d("MQ_AUTH", "loadCountries() SUCCESS: ${result.data.size} countries loaded: ${result.data.map { it.name }}")
                        _countries.update { result.data }
                        // Auto-load cities for default country (India, or first available)
                        val defaultCountry = result.data.find { it.name == "India" }
                            ?: result.data.firstOrNull()
                        AppLogger.d("MQ_AUTH", "loadCountries() defaultCountry=${defaultCountry?.name} (${defaultCountry?.id})")
                        defaultCountry?.let { loadCities(it.id) }
                    }
                    is Resource.Error -> {
                        AppLogger.e("MQ_AUTH", "loadCountries() ERROR: ${result.message}")
                    }
                    is Resource.Loading -> { /* no-op */ }
                }
            } catch (e: Exception) {
                AppLogger.e("MQ_AUTH", "loadCountries() EXCEPTION", e)
            }
        }
    }

    fun loadCities(countryId: String) {
        viewModelScope.launch(exceptionHandler) {
            try {
                AppLogger.d("MQ_AUTH", "loadCities($countryId) starting...")
                when (val result = referenceDataRepository.getCities(countryId)) {
                    is Resource.Success -> {
                        AppLogger.d("MQ_AUTH", "loadCities() SUCCESS: ${result.data.size} cities loaded: ${result.data.map { it.name }}")
                        _cities.update { result.data }
                    }
                    is Resource.Error -> {
                        AppLogger.e("MQ_AUTH", "loadCities() ERROR: ${result.message}")
                    }
                    is Resource.Loading -> { /* no-op */ }
                }
            } catch (e: Exception) {
                AppLogger.e("MQ_AUTH", "loadCities() EXCEPTION", e)
            }
        }
    }

    /** Map a country name to its backend UUID. */
    fun resolveCountryId(name: String): String {
        val resolved = _countries.value.find { it.name == name }?.id ?: ""
        AppLogger.d("MQ_AUTH", "resolveCountryId('$name') → '$resolved' (countries count=${_countries.value.size})")
        return resolved
    }

    /** Map a city name to its backend UUID. */
    fun resolveCityId(name: String): String {
        val resolved = _cities.value.find { it.name == name }?.id ?: ""
        AppLogger.d("MQ_AUTH", "resolveCityId('$name') → '$resolved' (cities count=${_cities.value.size})")
        return resolved
    }

    // ── Auth: Google ─────────────────────────────────────────────────────

    /**
     * Sign in with Google.
     * @param profile Non-null for new-user signup (onboarding data to persist).
     *               Null for existing-user login (profile already exists).
     */
    fun signInWithGoogle(profile: OnboardingProfile? = null) {
        AppLogger.d("MQ_AUTH", "signInWithGoogle() called, profile=$profile")
        viewModelScope.launch(exceptionHandler) {
            // Reset previous state so LaunchedEffect always detects changes
            _authState.update { UiState.Empty }
            AppLogger.d("MQ_AUTH", "Google: authState → Empty")
            // Guard: new-user signup requires a resolved grade
            if (profile != null && profile.gradeId.isBlank()) {
                AppLogger.e("MQ_AUTH", "Google: gradeId is BLANK — aborting")
                _authState.update { UiState.Error("Please select a grade before continuing.") }
                return@launch
            }
            _authState.update { UiState.Loading }
            AppLogger.d("MQ_AUTH", "Google: authState → Loading")
            try {
                AppLogger.d("MQ_AUTH", "Google: calling authRepository.signInWithGoogle()...")
                val result = authRepository.signInWithGoogle()
                AppLogger.d("MQ_AUTH", "Google: signIn result = $result")
                when (result) {
                    is Resource.Success -> {
                        val user = result.data!!
                        AppLogger.d("MQ_AUTH", "Google: signIn SUCCESS, userId=${user.id}")
                        // New-user signup → create the public.users row
                        if (profile != null) {
                            AppLogger.d("MQ_AUTH", "Google: calling upsertUserRow...")
                            val upsertResult = authRepository.upsertUserRow(
                                userId = user.id,
                                displayName = profile.displayName,
                                avatarId = profile.avatarId,
                                gradeId = profile.gradeId,
                                authProvider = "google",
                                countryId = profile.countryId,
                                cityId = profile.cityId,
                                schoolName = profile.schoolName,
                            )
                            AppLogger.d("MQ_AUTH", "Google: upsertUserRow result = $upsertResult")
                        }
                        sessionPrefs.isLoggedIn = true
                        _authState.update { UiState.Success(user) }
                        AppLogger.d("MQ_AUTH", "Google: authState → Success")
                    }
                    is Resource.Error -> {
                        AppLogger.e("MQ_AUTH", "Google: signIn ERROR = ${result.message}")
                        _authState.update { UiState.Error(result.message) }
                    }
                    is Resource.Loading -> { /* no-op */ }
                }
            } catch (e: Exception) {
                AppLogger.e("MQ_AUTH", "Google: EXCEPTION", e)
                _authState.update { UiState.Error(e.message ?: "Google sign-in failed") }
            }
        }
    }

    // ── Auth: Anonymous ──────────────────────────────────────────────────

    fun signInAnonymously(profile: OnboardingProfile? = null) {
        AppLogger.d("MQ_AUTH", "signInAnonymously() called, profile=$profile")
        viewModelScope.launch(exceptionHandler) {
            // Reset previous state so LaunchedEffect always detects changes
            _authState.update { UiState.Empty }
            AppLogger.d("MQ_AUTH", "Anon: authState → Empty")
            // Guard: new-user signup requires a resolved grade
            if (profile != null && profile.gradeId.isBlank()) {
                AppLogger.e("MQ_AUTH", "Anon: gradeId is BLANK — aborting")
                _authState.update { UiState.Error("Please select a grade before continuing.") }
                return@launch
            }
            _authState.update { UiState.Loading }
            AppLogger.d("MQ_AUTH", "Anon: authState → Loading")
            try {
                AppLogger.d("MQ_AUTH", "Anon: calling authRepository.signInAnonymously()...")
                val result = authRepository.signInAnonymously()
                AppLogger.d("MQ_AUTH", "Anon: signIn result = $result")
                when (result) {
                    is Resource.Success -> {
                        val user = result.data!!
                        AppLogger.d("MQ_AUTH", "Anon: signIn SUCCESS, userId=${user.id}")
                        // Always create a user row
                        AppLogger.d("MQ_AUTH", "Anon: calling upsertUserRow gradeId='${profile?.gradeId}'...")
                        val upsertResult = authRepository.upsertUserRow(
                            userId = user.id,
                            displayName = profile?.displayName ?: "Guest Player",
                            avatarId = profile?.avatarId ?: 1,
                            gradeId = profile?.gradeId ?: "",
                            authProvider = "anonymous",
                            countryId = profile?.countryId,
                            cityId = profile?.cityId,
                            schoolName = profile?.schoolName,
                        )
                        AppLogger.d("MQ_AUTH", "Anon: upsertUserRow result = $upsertResult")
                        sessionPrefs.isLoggedIn = true
                        _authState.update { UiState.Success(user) }
                        AppLogger.d("MQ_AUTH", "Anon: authState → Success")
                    }
                    is Resource.Error -> {
                        AppLogger.e("MQ_AUTH", "Anon: signIn ERROR = ${result.message}")
                        _authState.update { UiState.Error(result.message) }
                    }
                    is Resource.Loading -> { /* no-op */ }
                }
            } catch (e: Exception) {
                AppLogger.e("MQ_AUTH", "Anon: EXCEPTION", e)
                _authState.update { UiState.Error(e.message ?: "Anonymous sign-in failed") }
            }
        }
    }

    // ── Auth: Phone OTP ──────────────────────────────────────────────────

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

    /**
     * Stash onboarding data before entering the phone flow,
     * since the STEP3 → PHONE_AUTH navigation loses the composable state.
     */
    fun setPendingProfile(profile: OnboardingProfile?) {
        pendingProfile = profile
    }

    fun sendOtp(phone: String) {
        viewModelScope.launch(exceptionHandler) {
            _authState.update { UiState.Empty }
            _authState.update { UiState.Loading }
            try {
                val result = authRepository.signInWithPhone(phone)
                when (result) {
                    is Resource.Success -> {
                        _isOtpSent.update { true }
                        _authScreen.update { AuthScreenState.OTP }
                        _authState.update { UiState.Empty }
                        startResendTimer()
                    }
                    is Resource.Error -> {
                        _authState.update { UiState.Error(result.message) }
                    }
                    is Resource.Loading -> { /* no-op */ }
                }
            } catch (e: Exception) {
                _authState.update { UiState.Error(e.message ?: "Failed to send OTP") }
            }
        }
    }

    fun verifyOtp(code: String) {
        viewModelScope.launch(exceptionHandler) {
            // Reset previous state so LaunchedEffect always detects changes
            _authState.update { UiState.Empty }
            // Guard: if onboarding data was stashed, grade must be resolved
            val profile = pendingProfile
            if (profile != null && profile.gradeId.isBlank()) {
                _authState.update { UiState.Error("Please select a grade before continuing.") }
                return@launch
            }
            _authState.update { UiState.Loading }
            try {
                val result = authRepository.verifyOtp(_phoneNumber.value, code)
                when (result) {
                    is Resource.Success -> {
                        val user = result.data!!
                        // If onboarding data was stashed, create the user row
                        if (profile != null) {
                            authRepository.upsertUserRow(
                                userId = user.id,
                                displayName = profile.displayName,
                                avatarId = profile.avatarId,
                                gradeId = profile.gradeId,
                                authProvider = "phone",
                                countryId = profile.countryId,
                                cityId = profile.cityId,
                                schoolName = profile.schoolName,
                            )
                            pendingProfile = null
                        }
                        sessionPrefs.isLoggedIn = true
                        _authScreen.update { AuthScreenState.VERIFIED }
                        _authState.update { UiState.Success(user) }
                    }
                    is Resource.Error -> {
                        _authState.update { UiState.Error(result.message) }
                    }
                    is Resource.Loading -> { /* no-op */ }
                }
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
        viewModelScope.launch(exceptionHandler) {
            _resendTimer.update { 30 }
            while (_resendTimer.value > 0) {
                delay(1000)
                _resendTimer.update { it - 1 }
            }
        }
    }

    // ── Account linking ──────────────────────────────────────────────────

    fun showLinkingSheet(reason: String = "general") {
        _linkReason.update { reason }
        _isLinkingSheetVisible.update { true }
    }

    fun hideLinkingSheet() {
        _isLinkingSheetVisible.update { false }
    }

    fun linkAccount(provider: String) {
        viewModelScope.launch(exceptionHandler) {
            _authState.update { UiState.Empty }
            _authState.update { UiState.Loading }
            try {
                val result = when (provider) {
                    "google" -> authRepository.linkAccountWithGoogle()
                    else -> Resource.Error("Unknown provider: $provider")
                }
                when (result) {
                    is Resource.Success -> {
                        _isLinkingSheetVisible.update { false }
                        _authState.update { UiState.Success(null) }
                    }
                    is Resource.Error -> {
                        _authState.update { UiState.Error(result.message) }
                    }
                    is Resource.Loading -> { /* no-op */ }
                }
            } catch (e: Exception) {
                _authState.update { UiState.Error(e.message ?: "Account linking failed") }
            }
        }
    }

    // ── Sign out ─────────────────────────────────────────────────────────

    fun signOut() {
        viewModelScope.launch(exceptionHandler) {
            try { authRepository.signOut() } catch (_: Exception) { /* best-effort */ }
            sessionPrefs.clear()
        }
    }

    fun clearError() {
        _authState.update { UiState.Empty }
    }
}
