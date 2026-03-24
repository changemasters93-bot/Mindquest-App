package com.android.mindquest.domain.repository

import com.android.mindquest.core.util.Resource
import com.android.mindquest.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Data about an existing user found during duplicate detection.
 */
data class ExistingUserInfo(
    val id: String,
    val displayName: String,
    val authProvider: String,
    val totalXp: Long = 0,
    val level: Int = 1,
)

interface AuthRepository {
    suspend fun signInWithGoogle(): Resource<User>
    suspend fun signInAnonymously(): Resource<User>
    suspend fun signInWithPhone(phoneNumber: String): Resource<Unit>
    suspend fun verifyOtp(phoneNumber: String, otp: String): Resource<User>
    suspend fun linkAccountWithGoogle(): Resource<Unit>
    suspend fun linkAccountWithPhone(phoneNumber: String): Resource<Unit>
    suspend fun createUserProfile(user: User): Resource<Unit>
    suspend fun updateProfile(userId: String, fields: Map<String, Any>): Resource<Unit>

    /**
     * Upserts a row in public.users after first sign-in.
     * Inserts if new user, updates if returning.
     */
    suspend fun upsertUserRow(
        userId: String,
        displayName: String,
        avatarId: Int,
        gradeId: String,
        authProvider: String,
        countryId: String? = null,
        cityId: String? = null,
        schoolName: String? = null,
        googleSub: String? = null,
        email: String? = null,
        phone: String? = null,
    ): Resource<Unit>

    suspend fun getCurrentUser(): User?
    suspend fun getCurrentUserEmail(): String?
    suspend fun isAnonymous(): Boolean
    suspend fun signOut(): Resource<Unit>
    fun observeAuthState(): Flow<User?>

    /**
     * Lightweight session observer — emits the Supabase user ID when
     * the session becomes authenticated, null otherwise.
     * Does NOT hit the profile API, so it works for brand-new users too.
     */
    fun observeSessionUserId(): Flow<String?>

    // ── Account linking & duplicate detection ────────────────────────────

    /** Find existing user by email or phone. Returns null if not found. */
    suspend fun findExistingUser(email: String? = null, phone: String? = null): ExistingUserInfo?

    /** Merge all data from one user to another. Deletes the source user. */
    suspend fun mergeUsers(fromId: String, toId: String): Resource<Unit>

    /** Initiate phone linking by sending OTP to the phone number. */
    suspend fun linkPhoneInitiate(phoneNumber: String): Resource<Unit>

    /** Verify OTP for phone linking and complete the link. */
    suspend fun linkPhoneVerify(phoneNumber: String, otp: String): Resource<Unit>
}
