package com.android.mindquest.data.repository

import com.android.mindquest.core.constants.AppConstants
import com.android.mindquest.core.util.AppLogger
import com.android.mindquest.core.util.ErrorMapper
import com.android.mindquest.core.util.Resource
import com.android.mindquest.data.mock.MockDataSource
import com.android.mindquest.data.remote.ApiService
import com.android.mindquest.domain.model.User
import com.android.mindquest.domain.repository.AuthRepository
import com.android.mindquest.domain.repository.ExistingUserInfo
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Phone
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.int
import kotlinx.serialization.json.put

class AuthRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val apiService: ApiService
) : AuthRepository {

    // ── Sign-in methods ──────────────────────────────────────────────────

    override suspend fun signInWithGoogle(): Resource<User> {
        AppLogger.d("MQ_AUTH", "AuthRepo.signInWithGoogle() called, USE_MOCK_DATA=${AppConstants.USE_MOCK_DATA}")
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Success(MockDataSource.mockUser())
            } else {
                // Log the Auth config to verify redirect URL is set
                val authConfig = supabaseClient.auth.config
                AppLogger.d("MQ_AUTH", "AuthRepo: Auth config — scheme='${authConfig.scheme}', host='${authConfig.host}'")
                AppLogger.d("MQ_AUTH", "AuthRepo: Expected redirect URL = ${authConfig.scheme}://${authConfig.host}")
                AppLogger.d("MQ_AUTH", "AuthRepo: calling supabaseClient.auth.signInWith(Google)...")
                supabaseClient.auth.signInWith(Google)
                AppLogger.d("MQ_AUTH", "AuthRepo: signInWith(Google) returned — checking session...")
                val session = supabaseClient.auth.currentSessionOrNull()
                AppLogger.d("MQ_AUTH", "AuthRepo: session=${if (session != null) "PRESENT" else "NULL"}")
                if (session == null) return Resource.Error("Sign-in failed. Please try again.")
                val userId = session.user?.id
                    ?: return Resource.Error("Sign-in failed. Please try again.")

                // Try to fetch existing profile; null for first-time users
                val profile = try { apiService.getProfile(userId) } catch (_: Exception) { null }

                val googleSub = session.user?.identities
                    ?.find { it.provider == "google" }?.identityId

                // Extract Google profile info from session metadata
                val metadata = session.user?.userMetadata
                val googleName = metadata?.get("full_name")?.toString()?.trim('"')
                    ?: metadata?.get("name")?.toString()?.trim('"')
                    ?: ""

                // CRITICAL FIX: Add retry logic for email extraction
                // The session may not be immediately updated with email after OAuth
                // Retry up to 3 times with 500ms delay between attempts
                var googleEmail = session.user?.email ?: ""
                if (googleEmail.isBlank()) {
                    AppLogger.d("MQ_AUTH", "AuthRepo: Email blank on first attempt, retrying (up to 3 attempts)...")
                    val maxRetries = 3
                    val delayMs = 500L
                    for (attempt in 1..maxRetries) {
                        delay(delayMs)
                        val retrySession = supabaseClient.auth.currentSessionOrNull()
                        googleEmail = retrySession?.user?.email ?: ""
                        if (googleEmail.isNotBlank()) {
                            AppLogger.d("MQ_AUTH", "AuthRepo: Email extracted on retry attempt $attempt")
                            break
                        } else {
                            AppLogger.d("MQ_AUTH", "AuthRepo: Email still blank on retry attempt $attempt")
                        }
                    }
                }

                val googleAvatarUrl = metadata?.get("avatar_url")?.toString()?.trim('"')
                    ?: metadata?.get("picture")?.toString()?.trim('"')
                AppLogger.d("MQ_AUTH", "AuthRepo: Google metadata — name='$googleName', email='${if (googleEmail.isNotBlank()) googleEmail.length.toString() + " chars" else "BLANK"}', avatarUrl=${googleAvatarUrl != null}, googleSub=$googleSub")

                Resource.Success(
                    if (profile != null) {
                        User(
                            id = userId,
                            displayName = profile.user.displayName,
                            avatarId = profile.user.avatarId,
                            gradeId = profile.user.gradeId,
                            gradeLabel = profile.user.gradeLabel,
                            authProvider = profile.user.authProvider,
                            isAnonymous = false,
                            email = profile.user.email ?: googleEmail,
                            phone = profile.user.phone,
                            countryId = profile.user.countryId,
                            cityId = profile.user.cityId,
                            countryName = profile.user.countryName,
                            cityName = profile.user.cityName,
                            schoolName = profile.user.schoolName
                        )
                    } else {
                        // New user — use Google name as fallback for display name.
                        // Profile will be created via upsertUserRow() after onboarding.
                        User(
                            id = userId,
                            displayName = googleName,
                            avatarId = 1,
                            gradeId = "",
                            authProvider = "google",
                            isAnonymous = false,
                            email = googleEmail,
                        )
                    }
                )
            }
        } catch (e: Exception) {
            AppLogger.e("AuthRepo", "Google sign-in failed", e)
            Resource.Error(message = ErrorMapper.toUserMessage(e), throwable = e)
        }
    }

    override suspend fun signInAnonymously(): Resource<User> {
        AppLogger.d("MQ_AUTH", "AuthRepo.signInAnonymously() called, USE_MOCK_DATA=${AppConstants.USE_MOCK_DATA}")
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Success(
                    MockDataSource.mockUser().copy(
                        id = "anon-mock-001",
                        displayName = "Guest Player",
                        authProvider = "anonymous",
                        isAnonymous = true
                    )
                )
            } else {
                AppLogger.d("MQ_AUTH", "AuthRepo: calling supabaseClient.auth.signInAnonymously()...")
                supabaseClient.auth.signInAnonymously()
                AppLogger.d("MQ_AUTH", "AuthRepo: signInAnonymously returned, getting session...")
                val session = supabaseClient.auth.currentSessionOrNull()
                if (session == null) {
                    AppLogger.e("MQ_AUTH", "AuthRepo: session is NULL after signInAnonymously!")
                    return Resource.Error("Sign-in failed. Please try again.")
                }
                val userId = session.user?.id
                if (userId == null) {
                    AppLogger.e("MQ_AUTH", "AuthRepo: userId is NULL from session!")
                    return Resource.Error("Sign-in failed. Please try again.")
                }
                AppLogger.d("MQ_AUTH", "AuthRepo: signInAnonymously SUCCESS, userId=$userId")
                Resource.Success(
                    User(
                        id = userId,
                        displayName = "Guest Player",
                        avatarId = 1,
                        gradeId = "",
                        authProvider = "anonymous",
                        isAnonymous = true
                    )
                )
            }
        } catch (e: Exception) {
            AppLogger.e("AuthRepo", "Anonymous sign-in failed", e)
            Resource.Error(message = ErrorMapper.toUserMessage(e), throwable = e)
        }
    }

    override suspend fun signInWithPhone(phoneNumber: String): Resource<Unit> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Success(Unit)
            } else {
                supabaseClient.auth.signInWith(Phone) {
                    this.phone = phoneNumber
                }
                Resource.Success(Unit)
            }
        } catch (e: Exception) {
            AppLogger.e("AuthRepo", "Send OTP failed", e)
            Resource.Error(message = ErrorMapper.toUserMessage(e), throwable = e)
        }
    }

    override suspend fun verifyOtp(phoneNumber: String, otp: String): Resource<User> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Success(
                    MockDataSource.mockUser().copy(authProvider = "phone", phone = phoneNumber)
                )
            } else {
                supabaseClient.auth.verifyPhoneOtp(
                    type = io.github.jan.supabase.auth.OtpType.Phone.SMS,
                    phone = phoneNumber,
                    token = otp
                )
                val session = supabaseClient.auth.currentSessionOrNull()
                    ?: return Resource.Error("Verification failed. Please try again.")
                val userId = session.user?.id
                    ?: return Resource.Error("Verification failed. Please try again.")

                // Try to fetch existing profile; null for first-time users
                val profile = try { apiService.getProfile(userId) } catch (_: Exception) { null }

                Resource.Success(
                    if (profile != null) {
                        User(
                            id = userId,
                            displayName = profile.user.displayName,
                            avatarId = profile.user.avatarId,
                            gradeId = profile.user.gradeId,
                            gradeLabel = profile.user.gradeLabel,
                            authProvider = profile.user.authProvider,
                            isAnonymous = false,
                            email = profile.user.email,
                            phone = profile.user.phone ?: phoneNumber,
                            countryId = profile.user.countryId,
                            cityId = profile.user.cityId,
                            countryName = profile.user.countryName,
                            cityName = profile.user.cityName,
                            schoolName = profile.user.schoolName
                        )
                    } else {
                        User(
                            id = userId,
                            displayName = "",
                            avatarId = 1,
                            gradeId = "",
                            authProvider = "phone",
                            isAnonymous = false,
                            phone = phoneNumber
                        )
                    }
                )
            }
        } catch (e: Exception) {
            AppLogger.e("AuthRepo", "OTP verification failed", e)
            Resource.Error(message = ErrorMapper.toUserMessage(e), throwable = e)
        }
    }

    // ── User row creation (guide §2.2) ───────────────────────────────────

    override suspend fun upsertUserRow(
        userId: String,
        displayName: String,
        avatarId: Int,
        gradeId: String,
        authProvider: String,
        countryId: String?,
        cityId: String?,
        schoolName: String?,
        googleSub: String?,
        email: String?,
        phone: String?,
    ): Resource<Unit> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Success(Unit)
            } else {
                if (userId.isBlank()) {
                    AppLogger.e("AuthRepo", "Upsert skipped — userId is blank")
                    return Resource.Error("Sign-in failed. Please try again.")
                }
                val data = buildJsonObject {
                    put("id", userId)
                    put("display_name", displayName.ifBlank { "Player" })
                    put("avatar_id", avatarId)
                    put("grade_id", gradeId)
                    put("auth_provider", authProvider)
                    countryId?.takeIf { it.isNotBlank() }?.let { put("country_id", it) }
                    cityId?.takeIf { it.isNotBlank() }?.let { put("city_id", it) }
                    schoolName?.takeIf { it.isNotBlank() }?.let { put("school_name", it) }
                    googleSub?.let { put("google_sub", it) }
                    email?.takeIf { it.isNotBlank() }?.let { put("email", it) }
                    phone?.takeIf { it.isNotBlank() }?.let { put("phone", it) }
                }
                AppLogger.d("MQ_DB", "upsertUserRow: preparing data for userId=$userId, authProvider=$authProvider, email=$email")
                AppLogger.d("MQ_AUTH", "AuthRepo: upsertUser data=$data")
                apiService.upsertUser(data)
                AppLogger.d("MQ_DB", "upsertUserRow: SUCCESS - user row created/updated")
                AppLogger.d("MQ_AUTH", "AuthRepo: upsertUser SUCCESS")
                Resource.Success(Unit)
            }
        } catch (e: Exception) {
            AppLogger.e("AuthRepo", "Upsert user row failed", e)
            Resource.Error(message = ErrorMapper.toUserMessage(e), throwable = e)
        }
    }

    override suspend fun createUserProfile(user: User): Resource<Unit> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Success(Unit)
            } else {
                val fields = buildJsonObject {
                    put("display_name", JsonPrimitive(user.displayName))
                    put("avatar_id", JsonPrimitive(user.avatarId))
                    put("grade_id", JsonPrimitive(user.gradeId))
                }
                apiService.updateProfile(user.id, fields)
                Resource.Success(Unit)
            }
        } catch (e: Exception) {
            AppLogger.e("AuthRepo", "Create profile failed", e)
            Resource.Error(message = ErrorMapper.toUserMessage(e), throwable = e)
        }
    }

    // ── Account linking ──────────────────────────────────────────────────

    /**
     * Determines the auth_provider value based on current linked identities.
     * Returns "google_and_phone" if both providers are linked, otherwise returns the single provider.
     */
    private suspend fun determineAuthProvider(): String {
        return try {
            val session = supabaseClient.auth.currentSessionOrNull() ?: return "anonymous"
            val identities = session.user?.identities ?: return "anonymous"

            val hasGoogle = identities.any { it.provider == "google" }
            val hasPhone = !session.user?.phone.isNullOrBlank()

            when {
                hasGoogle && hasPhone -> "google_and_phone"
                hasGoogle -> "google"
                hasPhone -> "phone"
                else -> "anonymous"
            }
        } catch (e: Exception) {
            AppLogger.e("MQ_DB", "determineAuthProvider: failed", e)
            "anonymous"
        }
    }

    override suspend fun linkAccountWithGoogle(): Resource<Unit> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                AppLogger.d("MQ_DB", "linkAccountWithGoogle: MOCK MODE")
                Resource.Success(Unit)
            } else {
                // WORKAROUND: Supabase's linkIdentity(Google) causes a 500 on Google's OAuth page.
                // We use signInWith(Google) to create a Google auth entry, then the ViewModel
                // calls merge_anonymous_to_google RPC to transfer all data to the Google user.
                AppLogger.d("MQ_DB", "linkAccountWithGoogle: using signInWith(Google) workaround")
                supabaseClient.auth.signInWith(Google)
                AppLogger.d("MQ_DB", "linkAccountWithGoogle: signInWith returned (browser opened)")
                Resource.Success(Unit)
            }
        } catch (e: Exception) {
            AppLogger.e("MQ_DB", "linkAccountWithGoogle: FAILED", e)
            Resource.Error(message = ErrorMapper.toUserMessage(e), throwable = e)
        }
    }

    override suspend fun mergeAnonymousToGoogle(anonId: String, googleId: String, email: String) {
        AppLogger.d("MQ_DB", "mergeAnonymousToGoogle: anon=$anonId → google=$googleId")
        apiService.mergeAnonymousToGoogle(anonId, googleId, email)
        AppLogger.d("MQ_DB", "mergeAnonymousToGoogle: SUCCESS")
    }

    override suspend fun linkAccountWithPhone(phoneNumber: String): Resource<Unit> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                AppLogger.d("MQ_DB", "linkAccountWithPhone: MOCK MODE")
                Resource.Success(Unit)
            } else {
                AppLogger.d("MQ_DB", "linkAccountWithPhone: updating auth user with phone=$phoneNumber")
                supabaseClient.auth.updateUser {
                    this.phone = phoneNumber
                }

                // Update database with phone and auth_provider
                val session = supabaseClient.auth.currentSessionOrNull()
                val userId = session?.user?.id
                if (userId != null) {
                    val newAuthProvider = determineAuthProvider()
                    AppLogger.d("MQ_DB", "linkAccountWithPhone: updating database for userId=$userId, phone=$phoneNumber, auth_provider=$newAuthProvider")
                    apiService.updateProfile(userId, buildJsonObject {
                        put("phone", JsonPrimitive(phoneNumber))
                        put("auth_provider", JsonPrimitive(newAuthProvider))
                    })
                    AppLogger.d("MQ_DB", "linkAccountWithPhone: database updated")
                }

                AppLogger.d("MQ_DB", "linkAccountWithPhone: SUCCESS")
                Resource.Success(Unit)
            }
        } catch (e: Exception) {
            AppLogger.e("MQ_DB", "linkAccountWithPhone: FAILED for phone=$phoneNumber", e)
            AppLogger.e("AuthRepo", "Link phone number failed", e)
            Resource.Error(message = ErrorMapper.toUserMessage(e), throwable = e)
        }
    }

    // ── Profile update ───────────────────────────────────────────────────

    override suspend fun updateProfile(userId: String, fields: Map<String, Any>): Resource<Unit> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                AppLogger.d("MQ_DB", "updateProfile: MOCK MODE for userId=$userId")
                Resource.Success(Unit)
            } else {
                AppLogger.d("MQ_DB", "updateProfile: updating userId=$userId with fields=${fields.keys}")
                val jsonFields = buildJsonObject {
                    fields.forEach { (key, value) ->
                        when (value) {
                            is String -> put(key, JsonPrimitive(value))
                            is Int -> put(key, JsonPrimitive(value))
                            is Long -> put(key, JsonPrimitive(value))
                            is Boolean -> put(key, JsonPrimitive(value))
                            is Double -> put(key, JsonPrimitive(value))
                            else -> put(key, JsonPrimitive(value.toString()))
                        }
                    }
                }
                apiService.updateProfile(userId, jsonFields)
                AppLogger.d("MQ_DB", "updateProfile: SUCCESS for userId=$userId")
                Resource.Success(Unit)
            }
        } catch (e: Exception) {
            AppLogger.e("MQ_DB", "updateProfile: FAILED for userId=$userId", e)
            AppLogger.e("AuthRepo", "Update profile failed", e)
            Resource.Error(message = ErrorMapper.toUserMessage(e), throwable = e)
        }
    }

    // ── Session queries ──────────────────────────────────────────────────

    override suspend fun getCurrentUser(): User? {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                MockDataSource.mockUser()
            } else {
                val session = supabaseClient.auth.currentSessionOrNull() ?: return null
                val userId = session.user?.id ?: return null
                val profile = apiService.getProfile(userId)
                User(
                    id = userId,
                    displayName = profile.user.displayName,
                    avatarId = profile.user.avatarId,
                    gradeId = profile.user.gradeId,
                    gradeLabel = profile.user.gradeLabel,
                    authProvider = profile.user.authProvider,
                    isAnonymous = profile.user.authProvider == "anonymous",
                    email = profile.user.email,
                    phone = profile.user.phone,
                    countryId = profile.user.countryId,
                    cityId = profile.user.cityId,
                    countryName = profile.user.countryName,
                    cityName = profile.user.cityName,
                    schoolName = profile.user.schoolName
                )
            }
        } catch (e: Exception) {
            AppLogger.e("AuthRepo", "Get current user failed", e)
            null
        }
    }

    override suspend fun getCurrentUserEmail(): String? {
        return if (AppConstants.USE_MOCK_DATA) {
            null
        } else {
            val email = supabaseClient.auth.currentSessionOrNull()?.user?.email
            AppLogger.d("MQ_DB", "getCurrentUserEmail: retrieved email=${if (email != null) "***" else "NULL"}")
            email
        }
    }

    override suspend fun isAnonymous(): Boolean {
        return if (AppConstants.USE_MOCK_DATA) {
            false
        } else {
            try {
                val session = supabaseClient.auth.currentSessionOrNull()
                val userId = session?.user?.id ?: return true

                // Check both session state and profile auth_provider
                val sessionIsAnon = session.user?.email.isNullOrBlank() && session.user?.phone.isNullOrBlank()

                // Also check the profile auth_provider for confirmation
                val profile = try { apiService.getProfile(userId) } catch (_: Exception) { null }
                val profileIsAnon = profile?.user?.authProvider == "anonymous"

                val result = sessionIsAnon && profileIsAnon
                AppLogger.d("MQ_DB", "isAnonymous: sessionIsAnon=$sessionIsAnon, profileIsAnon=$profileIsAnon, result=$result")
                result
            } catch (e: Exception) {
                AppLogger.e("MQ_DB", "isAnonymous: check failed", e)
                false
            }
        }
    }

    override suspend fun signOut(): Resource<Unit> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                AppLogger.d("MQ_DB", "signOut: MOCK MODE")
                Resource.Success(Unit)
            } else {
                AppLogger.d("MQ_DB", "signOut: calling supabaseClient.auth.signOut()...")
                supabaseClient.auth.signOut()
                AppLogger.d("MQ_DB", "signOut: SUCCESS")
                Resource.Success(Unit)
            }
        } catch (e: Exception) {
            AppLogger.e("MQ_DB", "signOut: FAILED", e)
            AppLogger.e("AuthRepo", "Sign out failed", e)
            Resource.Error(message = ErrorMapper.toUserMessage(e), throwable = e)
        }
    }

    override fun observeAuthState(): Flow<User?> {
        return if (AppConstants.USE_MOCK_DATA) {
            flow { emit(MockDataSource.mockUser()) }
        } else {
            supabaseClient.auth.sessionStatus.map { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val userId = status.session.user?.id ?: return@map null
                        try {
                            val profile = apiService.getProfile(userId)
                            User(
                                id = userId,
                                displayName = profile.user.displayName,
                                avatarId = profile.user.avatarId,
                                gradeId = profile.user.gradeId,
                                gradeLabel = profile.user.gradeLabel,
                                authProvider = profile.user.authProvider,
                                isAnonymous = profile.user.authProvider == "anonymous",
                                email = profile.user.email,
                                phone = profile.user.phone,
                                countryId = profile.user.countryId,
                                cityId = profile.user.cityId,
                                countryName = profile.user.countryName,
                                cityName = profile.user.cityName,
                                schoolName = profile.user.schoolName
                            )
                        } catch (e: Exception) {
                            AppLogger.e("AuthRepo", "Observe auth - get profile failed", e)
                            null
                        }
                    }
                    else -> null
                }
            }
        }
    }

    // ── Lightweight session observer ─────────────────────────────────────

    override fun observeSessionUserId(): Flow<String?> {
        return if (AppConstants.USE_MOCK_DATA) {
            flow { emit(null) }
        } else {
            supabaseClient.auth.sessionStatus.map { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val userId = status.session.user?.id
                        AppLogger.d("MQ_AUTH", "observeSessionUserId: Authenticated, userId=$userId")
                        userId
                    }
                    else -> {
                        AppLogger.d("MQ_AUTH", "observeSessionUserId: status=${status::class.simpleName}")
                        null
                    }
                }
            }
        }
    }

    // ── Account linking & duplicate detection ─────────────────────────────

    override suspend fun findExistingUser(email: String?, phone: String?): ExistingUserInfo? {
        return try {
            AppLogger.d("MQ_DB", "findExistingUser: searching by email=${email != null}, phone=${phone != null}")
            val json = when {
                !email.isNullOrBlank() -> apiService.findUserByEmail(email)
                !phone.isNullOrBlank() -> apiService.findUserByPhone(phone)
                else -> null
            } ?: return null.also { AppLogger.d("MQ_DB", "findExistingUser: NO MATCH FOUND") }

            AppLogger.d("MQ_DB", "findExistingUser: MATCH FOUND - user exists")
            AppLogger.d("MQ_AUTH", "findExistingUser: found=$json")
            ExistingUserInfo(
                id = json["id"]?.jsonPrimitive?.content ?: return null,
                displayName = json["display_name"]?.jsonPrimitive?.content ?: "",
                authProvider = json["auth_provider"]?.jsonPrimitive?.content ?: "",
                totalXp = json["total_xp"]?.jsonPrimitive?.long ?: 0L,
                level = json["level"]?.jsonPrimitive?.int ?: 1,
            )
        } catch (e: Exception) {
            AppLogger.e("MQ_DB", "findExistingUser: FAILED", e)
            AppLogger.e("MQ_AUTH", "findExistingUser failed", e)
            null
        }
    }

    override suspend fun mergeUsers(fromId: String, toId: String): Resource<Unit> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                AppLogger.d("MQ_DB", "mergeUsers: MOCK MODE")
                return Resource.Success(Unit)
            }
            AppLogger.d("MQ_DB", "mergeUsers: STARTING merge from=$fromId → to=$toId")
            apiService.mergeUsers(fromId, toId)
            AppLogger.d("MQ_DB", "mergeUsers: SUCCESS - all data migrated")
            AppLogger.d("MQ_AUTH", "mergeUsers: from=$fromId → to=$toId")
            Resource.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("MQ_DB", "mergeUsers: FAILED from=$fromId to=$toId", e)
            AppLogger.e("MQ_AUTH", "mergeUsers failed", e)
            Resource.Error(message = ErrorMapper.toUserMessage(e), throwable = e)
        }
    }

    override suspend fun linkPhoneInitiate(phoneNumber: String): Resource<Unit> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                AppLogger.d("MQ_DB", "linkPhoneInitiate: MOCK MODE")
                return Resource.Success(Unit)
            }
            AppLogger.d("MQ_DB", "linkPhoneInitiate: STARTING OTP send to phone=$phoneNumber")
            AppLogger.d("MQ_AUTH", "linkPhoneInitiate: sending OTP to $phoneNumber")
            supabaseClient.auth.signInWith(Phone) {
                this.phone = phoneNumber
            }
            AppLogger.d("MQ_DB", "linkPhoneInitiate: SUCCESS - OTP sent to $phoneNumber")
            AppLogger.d("MQ_AUTH", "linkPhoneInitiate: OTP sent")
            Resource.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("MQ_DB", "linkPhoneInitiate: FAILED for phone=$phoneNumber", e)
            AppLogger.e("MQ_AUTH", "linkPhoneInitiate failed", e)
            Resource.Error(message = ErrorMapper.toUserMessage(e), throwable = e)
        }
    }

    override suspend fun linkPhoneVerify(phoneNumber: String, otp: String): Resource<Unit> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                AppLogger.d("MQ_DB", "linkPhoneVerify: MOCK MODE")
                return Resource.Success(Unit)
            }
            AppLogger.d("MQ_DB", "linkPhoneVerify: STARTING OTP verification for phone=$phoneNumber")
            AppLogger.d("MQ_AUTH", "linkPhoneVerify: verifying OTP for $phoneNumber")
            supabaseClient.auth.verifyPhoneOtp(
                type = io.github.jan.supabase.auth.OtpType.Phone.SMS,
                phone = phoneNumber,
                token = otp,
            )
            AppLogger.d("MQ_DB", "linkPhoneVerify: OTP verified successfully")
            // Now link the phone to the current user
            AppLogger.d("MQ_DB", "linkPhoneVerify: updating auth user with phone=$phoneNumber")
            supabaseClient.auth.updateUser {
                this.phone = phoneNumber
            }

            // Update database with phone and auth_provider
            val session = supabaseClient.auth.currentSessionOrNull()
            val userId = session?.user?.id
            if (userId != null) {
                val newAuthProvider = determineAuthProvider()
                AppLogger.d("MQ_DB", "linkPhoneVerify: updating database for userId=$userId, phone=$phoneNumber, auth_provider=$newAuthProvider")
                apiService.updateProfile(userId, buildJsonObject {
                    put("phone", JsonPrimitive(phoneNumber))
                    put("auth_provider", JsonPrimitive(newAuthProvider))
                })
                AppLogger.d("MQ_DB", "linkPhoneVerify: database updated")
            }

            AppLogger.d("MQ_DB", "linkPhoneVerify: SUCCESS - phone linked to account")
            AppLogger.d("MQ_AUTH", "linkPhoneVerify: phone linked successfully")
            Resource.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e("MQ_DB", "linkPhoneVerify: FAILED for phone=$phoneNumber", e)
            AppLogger.e("MQ_AUTH", "linkPhoneVerify failed", e)
            Resource.Error(message = ErrorMapper.toUserMessage(e), throwable = e)
        }
    }
}
