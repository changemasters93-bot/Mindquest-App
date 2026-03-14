package com.android.mindquest.domain.repository

import com.android.mindquest.core.util.Resource
import com.android.mindquest.domain.model.User
import kotlinx.coroutines.flow.Flow

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
    ): Resource<Unit>

    suspend fun getCurrentUser(): User?
    suspend fun isAnonymous(): Boolean
    suspend fun signOut(): Resource<Unit>
    fun observeAuthState(): Flow<User?>
}
