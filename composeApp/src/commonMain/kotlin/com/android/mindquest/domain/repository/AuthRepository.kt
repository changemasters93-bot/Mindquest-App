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
    suspend fun getCurrentUser(): User?
    suspend fun isAnonymous(): Boolean
    suspend fun signOut(): Resource<Unit>
    fun observeAuthState(): Flow<User?>
}
