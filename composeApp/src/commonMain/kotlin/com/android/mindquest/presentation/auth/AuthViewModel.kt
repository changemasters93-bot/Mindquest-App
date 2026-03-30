package com.android.mindquest.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mindquest.core.constants.AppConstants
import com.android.mindquest.core.prefs.SessionPrefs
import com.android.mindquest.core.util.AppLogger
import com.android.mindquest.core.util.Resource
import com.android.mindquest.core.util.SnackbarManager
import com.android.mindquest.core.util.UiState
import com.android.mindquest.domain.model.City
import com.android.mindquest.domain.model.Country
import com.android.mindquest.domain.model.Grade
import com.android.mindquest.domain.model.User
import com.android.mindquest.domain.repository.AuthRepository
import com.android.mindquest.domain.repository.ExistingUserInfo
import com.android.mindquest.domain.repository.ReferenceDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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
 * Label fields carry raw UI selections; VM resolves to IDs via [awaitGrades].
 */
data class OnboardingProfile(
    val displayName: String,
    val avatarId: Int,
    val gradeId: String = "",
    val gradeLabel: String = "",
    val countryId: String? = null,
    val countryName: String? = null,
    val cityId: String? = null,
    val cityName: String? = null,
    val schoolName: String? = null,
)

/**
 * Shown when a duplicate account is detected during sign-in.
 * The UI can display a "Merge Accounts" or "Create New" prompt.
 */
data class MergeSuggestion(
    val existingUser: ExistingUserInfo,
    val newUserId: String,
    val newUserEmail: String? = null,
    val newUserPhone: String? = null,
    val trigger: String, // "google_signup", "phone_signup", "guest_upgrade"
    /** Stashed profile for the new user (if from onboarding). */
    val pendingProfile: OnboardingProfile? = null,
    /** Stashed User from the sign-in result. */
    val pendingUser: User? = null,
)

/**
 * Confirmation prompt before linking a provider to the current account.
 */
data class LinkConfirmation(
    val provider: String,
    val currentAuthProvider: String,
)

/**
 * Duplicate email scenario — user tried to sign up but email already exists.
 */
data class DuplicateEmailError(
    val email: String,
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val referenceDataRepository: ReferenceDataRepository,
    private val sessionPrefs: SessionPrefs,
    private val snackbarManager: SnackbarManager,
) : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        AppLogger.e("AuthViewModel", "Unhandled coroutine exception: ${throwable::class.simpleName}: ${throwable.message}", throwable as? Exception)
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

    /** Grades fetched from backend — used by onboarding STEP2. */
    private val _grades = MutableStateFlow<List<Grade>>(emptyList())
    val grades: StateFlow<List<Grade>> = _grades.asStateFlow()

    /** Countries fetched from backend — used by onboarding STEP1. */
    private val _countries = MutableStateFlow<List<Country>>(emptyList())
    val countries: StateFlow<List<Country>> = _countries.asStateFlow()

    /** Cities for the currently selected country. */
    private val _cities = MutableStateFlow<List<City>>(emptyList())
    val cities: StateFlow<List<City>> = _cities.asStateFlow()

    // ── Duplicate detection / merge suggestion ──────────────────────────

    private val _mergeSuggestion = MutableStateFlow<MergeSuggestion?>(null)
    val mergeSuggestion: StateFlow<MergeSuggestion?> = _mergeSuggestion.asStateFlow()

    // ── Link confirmation ────────────────────────────────────────────────

    private val _linkConfirmation = MutableStateFlow<LinkConfirmation?>(null)
    val linkConfirmation: StateFlow<LinkConfirmation?> = _linkConfirmation.asStateFlow()

    // ── Duplicate email error (FIX #5) ────────────────────────────────────

    private val _duplicateEmailError = MutableStateFlow<DuplicateEmailError?>(null)
    val duplicateEmailError: StateFlow<DuplicateEmailError?> = _duplicateEmailError.asStateFlow()

    /**
     * Temporarily stores the onboarding profile so it's available after
     * OTP verification (since Phone auth has STEP3 → PHONE_AUTH navigation).
     */
    private var pendingProfile: OnboardingProfile? = null

    /**
     * Stashed onboarding profile for Google OAuth callback.
     */
    private var pendingGoogleProfile: OnboardingProfile? = null

    /**
     * Set when account linking is in progress (browser-based OAuth).
     * The session observer uses this to detect when linking completes.
     */
    private var pendingLinkProvider: String? = null

    /**
     * Saves the original user during phone linking flow.
     * Phone sign-in replaces the session, so we need the original user's data after OTP verification.
     * We save the full User object to avoid getCurrentUser() failing (it would try to fetch the new phone user).
     */
    private var pendingPhoneLinkUser: User? = null

    /** Flag to prevent concurrent Google sign-in attempts (debounce). */
    private var isGoogleSignInInProgress = false

    /**
     * Tracks whether a Google sign-in is in progress (from ANY path: onboarding or existing login).
     * When pendingGoogleProfile is null (e.g., EXISTING_LOGIN), the session observer still needs
     * to know that a Google sign-in was initiated so it can handle the OAuth completion.
     */
    private var pendingGoogleSignIn = false

    /** Flag to track if Google provider was selected in an earlier step (not on Step 3). */
    private var googleWasPreSelected = false

    /** Account recovery message (shown when returning user is detected). */
    private val _accountRecoveryMessage = MutableStateFlow<String?>(null)
    val accountRecoveryMessage: StateFlow<String?> = _accountRecoveryMessage.asStateFlow()

    /** @deprecated Use SnackbarManager instead. Kept for backward compatibility. */
    val linkingSnackbarMessage: StateFlow<String?> = MutableStateFlow(null)

    /** Flag to indicate user needs onboarding (new user who chose "I already have an account" but has no profile). */
    private val _needsOnboarding = MutableStateFlow(false)

    /**
     * Stashed Google user for the EXISTING_LOGIN → onboarding path.
     * The user has an auth.users entry but no public.users row yet.
     * After onboarding (STEP1 → STEP2), completeGoogleOnboarding() uses this to create the row.
     */
    private var pendingGoogleUser: User? = null

    /**
     * Stashed anonymous user ID for the "Link Google" workaround.
     * Since we use signInWith(Google) instead of linkIdentity(Google),
     * the session is REPLACED (not linked). We need the old anon ID to move the identity back.
     */
    private var pendingMergeAnonId: String? = null


    val needsOnboarding: StateFlow<Boolean> = _needsOnboarding.asStateFlow()

    /** Whether the user has already seen the onboarding slides on this device. */
    val hasSeenOnboarding: Boolean get() = sessionPrefs.hasSeenOnboarding

    /** Mark onboarding slides as viewed — persists across sessions. */
    fun markOnboardingSeen() { sessionPrefs.hasSeenOnboarding = true }

    init {
        AppLogger.d("MQ_AUTH", "AuthViewModel init{} START")
        viewModelScope.launch(Dispatchers.Default + exceptionHandler) {
            loadGradesInternal()
        }
        viewModelScope.launch(Dispatchers.Default + exceptionHandler) {
            loadCountriesInternal()
        }
        observeSessionForGoogleCallback()
        AppLogger.d("MQ_AUTH", "AuthViewModel init{} END — coroutines launched")
    }

    /**
     * Resets stale linking / sign-in flags so the session observer
     * doesn't confuse a brand-new auth flow with a previous one.
     * Called at the start of every auth entry point.
     */
    private fun resetStaleAuthState() {
        if (pendingLinkProvider != null) {
            AppLogger.d("MQ_AUTH", "resetStaleAuthState: clearing stale pendingLinkProvider=$pendingLinkProvider")
        }
        pendingLinkProvider = null
        pendingMergeAnonId = null
        pendingGoogleProfile = null
        pendingGoogleSignIn = false
        isGoogleSignInInProgress = false
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

    private suspend fun loadGradesInternal() {
        AppLogger.d("MQ_AUTH", "loadGrades() starting...")
        try {
            when (val result = referenceDataRepository.getGrades()) {
                is Resource.Success -> {
                    AppLogger.d("MQ_AUTH", "loadGrades() SUCCESS: ${result.data.size} grades")
                    _grades.update { result.data }
                }
                is Resource.Error -> AppLogger.e("MQ_AUTH", "loadGrades() ERROR: ${result.message}")
                is Resource.Loading -> {}
            }
        } catch (e: Exception) {
            AppLogger.e("MQ_AUTH", "loadGrades() EXCEPTION", e)
        }
    }

    fun loadGrades() {
        viewModelScope.launch(Dispatchers.Default + exceptionHandler) { loadGradesInternal() }
    }

    private suspend fun awaitGrades(): List<Grade> {
        if (_grades.value.isNotEmpty()) return _grades.value
        AppLogger.d("MQ_AUTH", "awaitGrades() — waiting (max 10s)...")
        return withTimeoutOrNull(10_000L) {
            _grades.first { it.isNotEmpty() }
        } ?: run {
            AppLogger.e("MQ_AUTH", "awaitGrades() TIMED OUT")
            emptyList()
        }
    }

    private suspend fun awaitCountries(): List<Country> {
        if (_countries.value.isNotEmpty()) return _countries.value
        return withTimeoutOrNull(10_000L) { _countries.first { it.isNotEmpty() } } ?: emptyList()
    }

    fun resolveGradeId(label: String): String {
        val resolved = _grades.value.find { it.label == label }?.id ?: ""
        AppLogger.d("MQ_AUTH", "resolveGradeId('$label') → '$resolved' (grades count=${_grades.value.size})")
        return resolved
    }

    private suspend fun resolveProfileIds(profile: OnboardingProfile): OnboardingProfile {
        val grades = awaitGrades()
        val countries = awaitCountries()
        val cities = _cities.value

        val resolvedGradeId = if (profile.gradeId.isNotBlank()) profile.gradeId
            else grades.find { it.label == profile.gradeLabel }?.id ?: ""
        val resolvedCountryId = if (!profile.countryId.isNullOrBlank()) profile.countryId
            else countries.find { it.name == profile.countryName }?.id
        val resolvedCityId = if (!profile.cityId.isNullOrBlank()) profile.cityId
            else cities.find { it.name == profile.cityName }?.id

        AppLogger.d("MQ_AUTH", "resolveProfileIds: grade='${profile.gradeLabel}'→'$resolvedGradeId'")
        return profile.copy(gradeId = resolvedGradeId, countryId = resolvedCountryId, cityId = resolvedCityId)
    }

    // ── Country / City ──────────────────────────────────────────────────

    private suspend fun loadCountriesInternal() {
        AppLogger.d("MQ_AUTH", "loadCountries() starting...")
        try {
            when (val result = referenceDataRepository.getCountries()) {
                is Resource.Success -> {
                    AppLogger.d("MQ_AUTH", "loadCountries() SUCCESS: ${result.data.size} countries")
                    _countries.update { result.data }
                    val defaultCountry = result.data.find { it.name == "India" } ?: result.data.firstOrNull()
                    defaultCountry?.let { loadCitiesInternal(it.id) }
                }
                is Resource.Error -> AppLogger.e("MQ_AUTH", "loadCountries() ERROR: ${result.message}")
                is Resource.Loading -> {}
            }
        } catch (e: Exception) {
            AppLogger.e("MQ_AUTH", "loadCountries() EXCEPTION", e)
        }
    }

    fun loadCountries() {
        viewModelScope.launch(Dispatchers.Default + exceptionHandler) { loadCountriesInternal() }
    }

    private suspend fun loadCitiesInternal(countryId: String) {
        try {
            when (val result = referenceDataRepository.getCities(countryId)) {
                is Resource.Success -> _cities.update { result.data }
                is Resource.Error -> AppLogger.e("MQ_AUTH", "loadCities() ERROR: ${result.message}")
                is Resource.Loading -> {}
            }
        } catch (e: Exception) {
            AppLogger.e("MQ_AUTH", "loadCities() EXCEPTION", e)
        }
    }

    fun loadCities(countryId: String) {
        viewModelScope.launch(Dispatchers.Default + exceptionHandler) { loadCitiesInternal(countryId) }
    }

    fun resolveCountryId(name: String): String = _countries.value.find { it.name == name }?.id ?: ""
    fun resolveCityId(name: String): String = _cities.value.find { it.name == name }?.id ?: ""

    // ── Google pre-selection tracking (Phase 2.2) ──────────────────────────
    // Used to indicate that Google was selected before Step 3, so we can skip Step 3
    // if the user started the flow with Google on a dedicated screen

    fun markGoogleAsPreSelected() {
        googleWasPreSelected = true
        AppLogger.d("MQ_AUTH", "markGoogleAsPreSelected(): Google marked as pre-selected")
    }

    fun isGooglePreSelected(): Boolean = googleWasPreSelected

    fun resetGooglePreSelection() {
        googleWasPreSelected = false
        AppLogger.d("MQ_AUTH", "resetGooglePreSelection(): Google pre-selection cleared")
    }

    /** Get the stashed Google user (for EXISTING_LOGIN → onboarding path). */
    fun getPendingGoogleUser(): User? = pendingGoogleUser

    /**
     * Called after onboarding STEP2 when Google was pre-selected via EXISTING_LOGIN.
     * Creates the public.users row with collected onboarding data + stashed Google user.
     */
    fun completeGoogleOnboarding(profile: OnboardingProfile) {
        val user = pendingGoogleUser
        if (user == null) {
            AppLogger.e("MQ_AUTH", "completeGoogleOnboarding: pendingGoogleUser is null!")
            return
        }
        AppLogger.d("MQ_AUTH", "completeGoogleOnboarding: creating user row with profile for userId=${user.id}")
        viewModelScope.launch(exceptionHandler) {
            _authState.update { UiState.Loading }
            try {
                val resolved = resolveProfileIds(profile)
                if (resolved.gradeId.isBlank()) {
                    _authState.update { UiState.Error("Grades not loaded yet. Please try again.") }
                    return@launch
                }
                handleGoogleSignInSuccess(user, resolved)
                pendingGoogleUser = null
                _needsOnboarding.update { false }
                AppLogger.d("MQ_AUTH", "completeGoogleOnboarding: SUCCESS — user row created, navigating to home")
            } catch (e: Exception) {
                AppLogger.e("MQ_AUTH", "completeGoogleOnboarding: FAILED", e)
                _authState.update { UiState.Error("Failed to complete setup. Please try again.") }
            }
        }
    }

    // ── Auth: Google ─────────────────────────────────────────────────────

    fun signInWithGoogle(profile: OnboardingProfile? = null) {
        // Prevent concurrent Google sign-in calls (debounce)
        if (isGoogleSignInInProgress) {
            AppLogger.d("MQ_AUTH", "signInWithGoogle() BLOCKED — already in progress")
            return
        }
        // Clear stale linking state from any previous failed flow
        resetStaleAuthState()
        isGoogleSignInInProgress = true
        pendingGoogleSignIn = true
        AppLogger.d("MQ_AUTH", "signInWithGoogle() called, profile=${profile != null}, pendingGoogleSignIn=true")
        viewModelScope.launch(exceptionHandler) {
            _authState.update { UiState.Empty }

            val resolvedProfile = if (profile != null) {
                val resolved = resolveProfileIds(profile)
                if (resolved.gradeId.isBlank()) {
                    _authState.update { UiState.Error("Grades not loaded yet. Please try again.") }
                    pendingGoogleSignIn = false
                    isGoogleSignInInProgress = false
                    return@launch
                }
                resolved
            } else null

            _authState.update { UiState.Loading }
            pendingGoogleProfile = resolvedProfile

            // 60s timeout safety net — covers BOTH onboarding and existing login paths
            viewModelScope.launch(exceptionHandler) {
                delay(60_000)
                if (_authState.value is UiState.Loading && pendingGoogleSignIn) {
                    pendingGoogleProfile = null
                    pendingGoogleSignIn = false
                    isGoogleSignInInProgress = false
                    _authState.update { UiState.Error("Google sign-in timed out. Please try again.") }
                    AppLogger.d("MQ_AUTH", "Google: 60s timeout — all flags reset")
                }
            }

            try {
                val result = authRepository.signInWithGoogle()
                when (result) {
                    is Resource.Success -> {
                        // Guard: if session observer already handled this (race condition), skip
                        if (_authState.value is UiState.Success) {
                            AppLogger.d("MQ_AUTH", "Google: session observer already completed the flow — skipping inline handling")
                            return@launch
                        }
                        val user = result.data ?: return@launch
                        val googleEmail = user.email
                        AppLogger.d("MQ_AUTH", "Google: userId=${user.id}, email='$googleEmail'")

                        // ── Duplicate detection + returning user (#4, #16, #17) ──
                        if (!googleEmail.isNullOrBlank()) {
                            try {
                                val existing = authRepository.findExistingUser(email = googleEmail)
                                when {
                                    // Same user returning — skip onboarding, go straight to login
                                    existing != null && existing.id == user.id -> {
                                        AppLogger.d("MQ_AUTH", "Google: RETURNING USER (same id=${user.id}), skipping onboarding")
                                        handleGoogleSignInSuccess(user, null) // null profile = returning user
                                        return@launch
                                    }
                                    // Different user with same email — block and show error
                                    existing != null && existing.id != user.id -> {
                                        AppLogger.e("MQ_AUTH", "Google: DUPLICATE BLOCKED! existing=${existing.id}, new=${user.id}, email=$googleEmail")
                                        // FIX #4: Sign out the orphaned auth user to prevent DB clutter
                                        try {
                                            authRepository.signOut()
                                            AppLogger.d("MQ_AUTH", "Google: signed out orphaned auth user")
                                        } catch (e: Exception) {
                                            AppLogger.e("MQ_AUTH", "Google: failed to sign out orphaned user", e)
                                        }
                                        // FIX #5: Show duplicate error state with recovery options
                                        _duplicateEmailError.update { DuplicateEmailError(email = googleEmail) }
                                        pendingGoogleSignIn = false
                                        isGoogleSignInInProgress = false
                                        _authState.update { UiState.Empty }
                                        return@launch
                                    }
                                    // No existing user — new signup
                                    else -> {
                                        AppLogger.d("MQ_AUTH", "Google: no existing user for email=$googleEmail, new signup")
                                        // FIX #3: If resolvedProfile is null (EXISTING_LOGIN path),
                                        // redirect to onboarding instead of calling handleGoogleSignInSuccess(user, null)
                                        // which would skip upsertUserRow() and leave no public.users row!
                                        if (resolvedProfile == null) {
                                            AppLogger.d("MQ_AUTH", "Google: new user from EXISTING_LOGIN — redirecting to onboarding")
                                            pendingGoogleUser = user
                                            _needsOnboarding.update { true }
                                            pendingGoogleSignIn = false
                                            isGoogleSignInInProgress = false
                                            _authState.update { UiState.Empty }
                                            return@launch
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                AppLogger.e("MQ_AUTH", "Google: duplicate check failed (continuing)", e)
                            }
                        }

                        handleGoogleSignInSuccess(user, resolvedProfile)
                    }
                    is Resource.Error -> {
                        AppLogger.e("MQ_AUTH", "Google: sign-in failed: ${result.message}")
                        // The signInWith(Google) returned before OAuth completed (browser redirect).
                        // DON'T set authState to Error — the session observer (Case 1 or Case 3)
                        // will handle the completion when the browser redirects back.
                        // BUT we must reset isGoogleSignInInProgress so user can retry if needed
                        // (the 30s UI timeout in ExistingLoginScreen may reset the loading state).
                        isGoogleSignInInProgress = false
                        AppLogger.d("MQ_AUTH", "Google: inline Error — isGoogleSignInInProgress reset, session observer will handle completion")
                    }
                    is Resource.Loading -> {}
                }
            } catch (e: Exception) {
                AppLogger.e("MQ_AUTH", "Google: exception during sign-in", e)
                pendingGoogleSignIn = false
                isGoogleSignInInProgress = false
                _authState.update { UiState.Error(e.message ?: "Google sign-in failed") }
            } finally {
                // Reset flag in case of timeout or other edge cases
                delay(500)
                if (_authState.value is UiState.Success) {
                    isGoogleSignInInProgress = false
                    pendingGoogleSignIn = false
                    AppLogger.d("MQ_AUTH", "signInWithGoogle() completed successfully, flags reset")
                }
            }
        }
    }

    private suspend fun handleGoogleSignInSuccess(user: User?, profile: OnboardingProfile?) {
        if (user == null) return
        AppLogger.d("MQ_AUTH", "Google: signIn SUCCESS, userId=${user.id}, displayName='${user.displayName}', email='${user.email}'")

        if (profile != null) {
            // New user signup — create full user row with onboarding data
            val displayName = profile.displayName.ifBlank { user.displayName }
            // Get fresh email from session (in case user.email is empty)
            val sessionEmail = authRepository.getCurrentUserEmail()
            val emailToSave = user.email?.takeIf { it.isNotBlank() } ?: sessionEmail
            AppLogger.d("MQ_AUTH", "Google: upsertUserRow — user.email='${user.email}', sessionEmail='${sessionEmail?.takeIf { it.isNotBlank() } ?: "NULL"}', emailToSave='${emailToSave?.takeIf { it.isNotBlank() } ?: "NULL"}'")

            val upsertResult = authRepository.upsertUserRow(
                userId = user.id,
                displayName = displayName,
                avatarId = profile.avatarId,
                gradeId = profile.gradeId,
                authProvider = "google",
                countryId = profile.countryId,
                cityId = profile.cityId,
                schoolName = profile.schoolName,
                email = emailToSave,
            )
            AppLogger.d("MQ_AUTH", "Google: upsertUserRow (new user) result = $upsertResult")
        } else {
            // Returning user login — update auth_provider to google and show recovery message
            if (user.id.isNotBlank()) {
                try {
                    AppLogger.d("MQ_AUTH", "Google: returning user detected (ACCOUNT RECOVERY), updating auth_provider to 'google'")
                    authRepository.updateProfile(user.id, mapOf("auth_provider" to "google"))
                    AppLogger.d("MQ_AUTH", "Google: auth_provider updated to 'google' for returning user")

                    // Show account recovery message (Phase 2.2 Requirement 4)
                    val recoveryMessage = "Account recovered! Welcome back ${user.displayName}"
                    _accountRecoveryMessage.update { recoveryMessage }
                    AppLogger.d("MQ_AUTH", "Google: showing recovery message: $recoveryMessage")
                } catch (e: Exception) {
                    AppLogger.e("MQ_AUTH", "Google: failed to update auth_provider", e)
                }
            }
        }
        pendingGoogleProfile = null
        pendingGoogleSignIn = false
        isGoogleSignInInProgress = false
        sessionPrefs.isLoggedIn = true
        sessionPrefs.lastAuthProvider = "google"
        _authState.update { UiState.Success(user) }
        AppLogger.d("MQ_AUTH", "Google: authState → Success, all flags reset")
    }

    private fun observeSessionForGoogleCallback() {
        viewModelScope.launch(exceptionHandler) {
            authRepository.observeSessionUserId().collect { userId ->
                if (userId == null) return@collect

                // Case 1: Google sign-in during onboarding (new user)
                val profile = pendingGoogleProfile
                if (profile != null && _authState.value is UiState.Loading) {
                    AppLogger.d("MQ_AUTH", "Session observer: Google sign-in completed, userId=$userId")
                    try {
                        val user = authRepository.getCurrentUser() ?: User(
                            id = userId,
                            displayName = profile.displayName,
                            avatarId = profile.avatarId,
                            gradeId = profile.gradeId,
                            authProvider = "google",
                            isAnonymous = false,
                        )
                        handleGoogleSignInSuccess(user, profile)
                    } catch (e: Exception) {
                        pendingGoogleProfile = null
                        _authState.update { UiState.Error("Sign-in failed. Please try again.") }
                    }
                }

                // Case 2: Account linking (anonymous → Google) via merge approach
                // signInWith(Google) created a NEW auth entry + session.
                // We merge the anonymous user's data into this Google user.
                val linkProvider = pendingLinkProvider
                if (linkProvider != null && _authState.value is UiState.Loading) {
                    val anonId = pendingMergeAnonId

                    // CRITICAL: The session observer fires TWICE after signInWith(Google):
                    // 1st emission: OLD anonymous userId (session re-auth) → SKIP
                    // 2nd emission: NEW Google userId → THIS is the one we want
                    if (userId == anonId) {
                        AppLogger.d("MQ_AUTH", "Session observer: Case 2 — SKIP: userId=$userId matches anonId (old session), waiting for Google userId")
                        return@collect  // Don't clear pendingLinkProvider — wait for next emission
                    }

                    val googleId = userId
                    pendingLinkProvider = null
                    pendingMergeAnonId = null
                    AppLogger.d("MQ_AUTH", "Session observer: Case 2 — merge anon=$anonId → google=$googleId")

                    try {
                        // Extract email with retry (session may need a moment to update)
                        var googleEmail = ""
                        for (attempt in 1..3) {
                            googleEmail = authRepository.getCurrentUserEmail() ?: ""
                            if (googleEmail.isNotBlank()) break
                            if (attempt < 3) delay(500)
                        }
                        if (googleEmail.isBlank()) {
                            throw Exception("Google email not available after OAuth")
                        }

                        // Validate email isn't used by a different existing user
                        try {
                            val existing = authRepository.findExistingUser(email = googleEmail)
                            if (existing != null && existing.id != anonId && existing.id != googleId) {
                                snackbarManager.showError("This email is already linked to another account.")
                                _isLinkingSheetVisible.update { false }
                                _authState.update { UiState.Empty }
                                return@collect
                            }
                        } catch (_: Exception) {}

                        // Merge: clone anon profile → Google user, transfer child data, delete anon
                        if (anonId != null && anonId != googleId) {
                            authRepository.mergeAnonymousToGoogle(anonId, googleId, googleEmail)
                            AppLogger.d("MQ_AUTH", "Session observer: Case 2 — merge SUCCESS")
                        }

                        sessionPrefs.isLoggedIn = true
                        sessionPrefs.lastAuthProvider = "google"
                        _isLinkingSheetVisible.update { false }
                        snackbarManager.showSuccess("Google account linked successfully!")
                        _authState.update { UiState.Success(null) }
                    } catch (e: Exception) {
                        AppLogger.e("MQ_AUTH", "Session observer: Case 2 — FAILED: ${e.message}", e)
                        snackbarManager.showError("Account linking failed. Please try again.")
                        _isLinkingSheetVisible.update { false }
                        _authState.update { UiState.Error("Account linking failed. Please try again.") }
                    }
                }

                // Case 3: Google sign-in from EXISTING_LOGIN path (no pending profile)
                // When user clicks "I already have an account" → "Continue with Google",
                // pendingGoogleProfile is null but pendingGoogleSignIn is true.
                // The inline signInWithGoogle() may return before OAuth completes (browser redirect),
                // so the session observer must handle the completion here.
                if (pendingGoogleSignIn && pendingGoogleProfile == null && pendingLinkProvider == null
                    && _authState.value !is UiState.Success) {
                    AppLogger.d("MQ_AUTH", "Session observer: Case 3 — EXISTING_LOGIN Google sign-in completed, userId=$userId")
                    try {
                        // Get email from session directly (doesn't need public.users row)
                        val googleEmail = authRepository.getCurrentUserEmail()
                        AppLogger.d("MQ_AUTH", "Session observer: Case 3 — email=${if (googleEmail.isNullOrBlank()) "NULL" else "${googleEmail.length} chars"}")

                        // Try to get full user from public.users (may fail for new users)
                        val user = try { authRepository.getCurrentUser() } catch (_: Exception) { null }

                        if (user != null) {
                            // User has a public.users row — check if it's the same user or duplicate
                            AppLogger.d("MQ_AUTH", "Session observer: Case 3 — existing profile found")
                            if (!googleEmail.isNullOrBlank()) {
                                val existing = try { authRepository.findExistingUser(email = googleEmail) } catch (_: Exception) { null }
                                when {
                                    existing != null && existing.id == user.id -> {
                                        AppLogger.d("MQ_AUTH", "Session observer: Case 3 — RETURNING USER (same id)")
                                        handleGoogleSignInSuccess(user, null)
                                    }
                                    existing != null && existing.id != user.id -> {
                                        AppLogger.e("MQ_AUTH", "Session observer: Case 3 — DUPLICATE email detected!")
                                        try { authRepository.signOut() } catch (_: Exception) {}
                                        _duplicateEmailError.update { DuplicateEmailError(email = googleEmail) }
                                        pendingGoogleSignIn = false
                                        isGoogleSignInInProgress = false
                                        _authState.update { UiState.Empty }
                                    }
                                    else -> {
                                        // User exists in public.users but findExistingUser by email didn't match
                                        // Treat as returning user
                                        AppLogger.d("MQ_AUTH", "Session observer: Case 3 — user has profile, treating as returning")
                                        handleGoogleSignInSuccess(user, null)
                                    }
                                }
                            } else {
                                // No email but user exists — returning user
                                handleGoogleSignInSuccess(user, null)
                            }
                        } else {
                            // FIX #1: getCurrentUser() returned null = NO public.users row
                            // User authenticated in auth.users but needs onboarding to create public.users row
                            AppLogger.d("MQ_AUTH", "Session observer: Case 3 — NO public.users row, needs onboarding")

                            // Build minimal User from session data (email + userId only)
                            // Display name will be collected during onboarding STEP1
                            val minimalUser = User(
                                id = userId,
                                displayName = "",
                                email = googleEmail,
                                authProvider = "google",
                                isAnonymous = false,
                                avatarId = 1,
                                gradeId = "",
                            )
                            pendingGoogleUser = minimalUser
                            _needsOnboarding.update { true }
                            pendingGoogleSignIn = false
                            isGoogleSignInInProgress = false
                            _authState.update { UiState.Empty }
                            AppLogger.d("MQ_AUTH", "Session observer: Case 3 — stashed user id=$userId, redirecting to onboarding")
                        }
                    } catch (e: Exception) {
                        AppLogger.e("MQ_AUTH", "Session observer: Case 3 — exception", e)
                        pendingGoogleSignIn = false
                        isGoogleSignInInProgress = false
                        _authState.update { UiState.Error("Sign-in failed. Please try again.") }
                    }
                }
            }
        }
    }

    // ── Auth: Anonymous ──────────────────────────────────────────────────

    fun signInAnonymously(profile: OnboardingProfile? = null) {
        AppLogger.d("MQ_AUTH", "signInAnonymously() called")
        resetStaleAuthState()
        viewModelScope.launch(exceptionHandler) {
            _authState.update { UiState.Empty }

            val resolvedProfile = if (profile != null) {
                val resolved = resolveProfileIds(profile)
                if (resolved.gradeId.isBlank()) {
                    _authState.update { UiState.Error("Grades not loaded yet. Please try again.") }
                    return@launch
                }
                resolved
            } else null

            _authState.update { UiState.Loading }
            try {
                val result = authRepository.signInAnonymously()
                when (result) {
                    is Resource.Success -> {
                        val user = result.data ?: return@launch
                        authRepository.upsertUserRow(
                            userId = user.id,
                            displayName = resolvedProfile?.displayName ?: "Guest Player",
                            avatarId = resolvedProfile?.avatarId ?: 1,
                            gradeId = resolvedProfile?.gradeId ?: "",
                            authProvider = "anonymous",
                            countryId = resolvedProfile?.countryId,
                            cityId = resolvedProfile?.cityId,
                            schoolName = resolvedProfile?.schoolName,
                        )
                        sessionPrefs.isLoggedIn = true
                        sessionPrefs.lastAuthProvider = "anonymous"
                        _authState.update { UiState.Success(user) }
                    }
                    is Resource.Error -> _authState.update { UiState.Error(result.message) }
                    is Resource.Loading -> {}
                }
            } catch (e: Exception) {
                _authState.update { UiState.Error(e.message ?: "Anonymous sign-in failed") }
            }
        }
    }

    // ── Auth: Phone OTP ──────────────────────────────────────────────────

    fun updatePhoneNumber(phone: String) { _phoneNumber.update { phone } }

    fun updateOtpCode(code: String) {
        if (code.length <= 6) _otpCode.update { code }
    }

    fun navigateToPhone() { _authScreen.update { AuthScreenState.PHONE } }

    fun navigateToMain() {
        _authScreen.update { AuthScreenState.MAIN }
        _phoneNumber.update { "" }
        _otpCode.update { "" }
        _isOtpSent.update { false }
    }

    fun setPendingProfile(profile: OnboardingProfile?) { pendingProfile = profile }

    fun sendOtp(phone: String) {
        resetStaleAuthState()
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
                    is Resource.Error -> _authState.update { UiState.Error(result.message) }
                    is Resource.Loading -> {}
                }
            } catch (e: Exception) {
                _authState.update { UiState.Error(e.message ?: "Failed to send OTP") }
            }
        }
    }

    fun verifyOtp(code: String) {
        viewModelScope.launch(exceptionHandler) {
            _authState.update { UiState.Empty }

            val profile = pendingProfile?.let { resolveProfileIds(it) }
            if (profile != null && profile.gradeId.isBlank()) {
                _authState.update { UiState.Error("Grades not loaded yet. Please try again.") }
                return@launch
            }

            _authState.update { UiState.Loading }
            try {
                val result = authRepository.verifyOtp(_phoneNumber.value, code)
                when (result) {
                    is Resource.Success -> {
                        val user = result.data ?: return@launch
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
                                phone = _phoneNumber.value,
                            )
                            pendingProfile = null
                        }
                        sessionPrefs.isLoggedIn = true
                        sessionPrefs.lastAuthProvider = "phone"
                        _authScreen.update { AuthScreenState.VERIFIED }
                        _authState.update { UiState.Success(user) }
                    }
                    is Resource.Error -> _authState.update { UiState.Error(result.message) }
                    is Resource.Loading -> {}
                }
            } catch (e: Exception) {
                _authState.update { UiState.Error(e.message ?: "OTP verification failed") }
            }
        }
    }

    fun resendOtp() {
        if (_resendTimer.value == 0) sendOtp(_phoneNumber.value)
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

    fun clearLinkingSnackbar() {
        // No-op: snackbar is now managed globally by SnackbarManager
    }

    // ── Link confirmation (#13: Account takeover protection) ──────────────

    /** Show confirmation dialog before linking. */
    fun showLinkConfirmation(provider: String) {
        viewModelScope.launch(exceptionHandler) {
            val currentUser = authRepository.getCurrentUser()
            _linkConfirmation.update {
                LinkConfirmation(
                    provider = provider,
                    currentAuthProvider = currentUser?.authProvider ?: "unknown",
                )
            }
        }
    }

    /** User confirmed — proceed with actual linking. */
    fun confirmAndLink() {
        val confirmation = _linkConfirmation.value ?: return
        _linkConfirmation.update { null }
        linkAccount(confirmation.provider)
    }

    /** User cancelled linking. */
    fun cancelLinking() {
        _linkConfirmation.update { null }
        _isLinkingSheetVisible.update { false }
    }

    fun linkAccount(provider: String) {
        // Prevent double-tap: if linking is already in progress, ignore
        if (pendingLinkProvider != null) {
            AppLogger.d("MQ_AUTH", "linkAccount($provider): BLOCKED — already in progress (pendingLinkProvider=${pendingLinkProvider})")
            return
        }
        viewModelScope.launch(exceptionHandler) {
            _authState.update { UiState.Empty }
            _authState.update { UiState.Loading }
            AppLogger.d("MQ_AUTH", "linkAccount($provider): starting")

            // Stash anonymous user ID BEFORE signInWith replaces the session
            try {
                val currentUser = authRepository.getCurrentUser()
                pendingMergeAnonId = currentUser?.id
                AppLogger.d("MQ_AUTH", "linkAccount: stashed anonId=${pendingMergeAnonId}")
            } catch (e: Exception) {
                AppLogger.e("MQ_AUTH", "linkAccount: failed to stash anonymous user", e)
            }

            // Set pending flag so session observer can detect when browser OAuth completes
            pendingLinkProvider = provider

            // 60s timeout for browser-based linking
            viewModelScope.launch(exceptionHandler) {
                delay(60_000)
                if (pendingLinkProvider != null && _authState.value is UiState.Loading) {
                    AppLogger.e("MQ_AUTH", "linkAccount: 60s timeout — resetting")
                    pendingLinkProvider = null
                    _authState.update { UiState.Error("Account linking timed out. Please try again.") }
                    snackbarManager.showError("Account linking timed out. Please try again.")
                }
            }

            try {
                val result = when (provider) {
                    "google" -> authRepository.linkAccountWithGoogle()
                    else -> Resource.Error("Unknown provider: $provider")
                }
                // On mobile, linkIdentity(Google) may not return a meaningful result
                // because the browser redirect breaks the suspend chain.
                // The session observer will handle completion.
                when (result) {
                    is Resource.Success -> {
                        // linkIdentity(Google) returns immediately after opening the browser.
                        // The actual linking + DB update happens in session observer Case 2
                        // when the user returns from OAuth and the session updates.
                        AppLogger.d("MQ_AUTH", "linkAccount($provider): browser opened, waiting for session observer Case 2")
                    }
                    is Resource.Error -> {
                        pendingLinkProvider = null
                        _authState.update { UiState.Error(result.message) }
                        snackbarManager.showError(result.message)
                        AppLogger.d("MQ_AUTH", "linkAccount: error: ${result.message}")
                    }
                    is Resource.Loading -> {}
                }
            } catch (e: Exception) {
                // Don't clear pendingLinkProvider — browser may still redirect back
                AppLogger.e("MQ_AUTH", "linkAccount: exception (session observer may still complete)", e)
            }
        }
    }

    // ── Phone linking with OTP (#3) ────────────────────────────────────

    /** Initiate phone linking — sends OTP to the phone number. */
    fun linkPhoneStart(phone: String) {
        viewModelScope.launch(exceptionHandler) {
            _authState.update { UiState.Loading }
            _phoneNumber.update { phone }
            try {
                // FIX #3: Save original user BEFORE phone sign-in replaces the session
                // We save the full User object, not just ID, because getCurrentUser() will fail
                // after the session is replaced (it would try to fetch the new phone user)
                val originalUser = authRepository.getCurrentUser()
                if (originalUser != null) {
                    pendingPhoneLinkUser = originalUser
                    AppLogger.d("MQ_AUTH", "linkPhoneStart: saved original user ${originalUser.id}")
                } else {
                    AppLogger.e("MQ_AUTH", "linkPhoneStart: could not get current user")
                    _authState.update { UiState.Error("Failed to prepare phone linking") }
                    return@launch
                }

                val result = authRepository.linkPhoneInitiate(phone)
                when (result) {
                    is Resource.Success -> {
                        _isOtpSent.update { true }
                        _authScreen.update { AuthScreenState.LINKING }
                        _authState.update { UiState.Empty }
                        startResendTimer()
                        AppLogger.d("MQ_AUTH", "linkPhoneStart: OTP sent to $phone")
                    }
                    is Resource.Error -> {
                        pendingPhoneLinkUser = null
                        _authState.update { UiState.Error(result.message) }
                    }
                    is Resource.Loading -> {}
                }
            } catch (e: Exception) {
                pendingPhoneLinkUser = null
                _authState.update { UiState.Error(e.message ?: "Failed to send OTP") }
            }
        }
    }

    /** Verify OTP and complete phone linking. */
    fun linkPhoneVerify(otp: String) {
        viewModelScope.launch(exceptionHandler) {
            _authState.update { UiState.Loading }
            try {
                val result = authRepository.linkPhoneVerify(_phoneNumber.value, otp)
                when (result) {
                    is Resource.Success -> {
                        AppLogger.d("MQ_AUTH", "linkPhoneVerify: SUCCESS")

                        // FIX #3: Use saved original user, not current user (session was replaced)
                        // We have the full User object saved in linkPhoneStart() so we don't need to fetch it
                        val originalUser = pendingPhoneLinkUser
                        if (originalUser != null) {
                            val newProvider = when (originalUser.authProvider) {
                                "google" -> "google_and_phone"
                                "anonymous" -> "phone"
                                else -> "google_and_phone"
                            }
                            authRepository.upsertUserRow(
                                userId = originalUser.id,
                                displayName = originalUser.displayName,
                                avatarId = originalUser.avatarId,
                                gradeId = originalUser.gradeId,
                                authProvider = newProvider,
                                phone = _phoneNumber.value,
                            )
                            AppLogger.d("MQ_AUTH", "linkPhoneVerify: updated original userId=${originalUser.id} with provider='$newProvider'")
                        }
                        pendingPhoneLinkUser = null
                        _isLinkingSheetVisible.update { false }
                        _authScreen.update { AuthScreenState.MAIN }
                        _authState.update { UiState.Success(null) }
                        sessionPrefs.lastAuthProvider = "phone"
                    }
                    is Resource.Error -> {
                        pendingPhoneLinkUser = null
                        _authState.update { UiState.Error(result.message) }
                    }
                    is Resource.Loading -> {}
                }
            } catch (e: Exception) {
                pendingPhoneLinkUser = null
                _authState.update { UiState.Error(e.message ?: "Phone verification failed") }
            }
        }
    }

    // ── Duplicate email recovery (FIX #5) ────────────────────────────────

    /** User dismissed duplicate email error and wants to sign in instead. */
    fun handleDuplicateEmailSignInInstead() {
        _duplicateEmailError.update { null }
        // Navigate back to user type so they can choose "I already have an account"
        _authScreen.update { AuthScreenState.MAIN }
    }

    /** User wants to try with a different Google account. */
    fun handleDuplicateEmailRetry() {
        _duplicateEmailError.update { null }
        _authState.update { UiState.Empty }
        // Don't automatically retry — let the user tap "Continue with Google" again
        // which will prompt them to select a different account
    }

    // ── Merge / duplicate detection (#4, #16, #17) ──────────────────────

    /** User confirmed merge — move all data from existing to new account. */
    fun confirmMerge() {
        val suggestion = _mergeSuggestion.value ?: return
        viewModelScope.launch(exceptionHandler) {
            _authState.update { UiState.Loading }
            try {
                val result = authRepository.mergeUsers(
                    fromId = suggestion.existingUser.id,
                    toId = suggestion.newUserId,
                )
                if (result is Resource.Success) {
                    AppLogger.d("MQ_AUTH", "confirmMerge: SUCCESS — merged ${suggestion.existingUser.id} → ${suggestion.newUserId}")
                    // Now create the user row for the new user (if profile pending)
                    suggestion.pendingUser?.let { user ->
                        handleGoogleSignInSuccess(user, suggestion.pendingProfile)
                    }
                } else if (result is Resource.Error) {
                    _authState.update { UiState.Error(result.message) }
                }
            } catch (e: Exception) {
                _authState.update { UiState.Error(e.message ?: "Merge failed") }
            } finally {
                _mergeSuggestion.update { null }
            }
        }
    }

    /** User declined merge — continue with separate account. */
    fun skipMerge() {
        val suggestion = _mergeSuggestion.value ?: return
        _mergeSuggestion.update { null }
        // Proceed with normal sign-in for the new user
        viewModelScope.launch(exceptionHandler) {
            suggestion.pendingUser?.let { user ->
                handleGoogleSignInSuccess(user, suggestion.pendingProfile)
            }
        }
    }

    // ── Sign out ─────────────────────────────────────────────────────────

    fun signOut() {
        viewModelScope.launch(exceptionHandler) {
            try { authRepository.signOut() } catch (_: Exception) {}
            sessionPrefs.clear()
        }
    }

    fun clearError() {
        _authState.update { UiState.Empty }
    }
}
