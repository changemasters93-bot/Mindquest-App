package com.android.mindquest.data.repository

import com.android.mindquest.core.constants.AppConstants
import com.android.mindquest.core.util.Resource
import com.android.mindquest.data.mock.MockDataSource
import com.android.mindquest.data.remote.ApiService
import com.android.mindquest.domain.model.User
import com.android.mindquest.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Phone
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class AuthRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val apiService: ApiService
) : AuthRepository {

    override suspend fun signInWithGoogle(): Resource<User> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Success(MockDataSource.mockUser())
            } else {
                supabaseClient.auth.signInWith(Google)
                val session = supabaseClient.auth.currentSessionOrNull()
                    ?: return Resource.Error("Google sign-in failed: no session returned")
                val userId = session.user?.id
                    ?: return Resource.Error("Google sign-in failed: no user ID")
                val profile = apiService.getProfile(userId)
                Resource.Success(
                    User(
                        id = userId,
                        displayName = profile.user.displayName,
                        avatarId = profile.user.avatarId,
                        gradeId = profile.user.gradeId,
                        gradeLabel = profile.user.gradeLabel,
                        authProvider = profile.user.authProvider,
                        isAnonymous = false,
                        countryName = profile.user.countryName,
                        cityName = profile.user.cityName,
                        schoolName = profile.user.schoolName
                    )
                )
            }
        } catch (e: Exception) {
            Resource.Error(
                message = e.message ?: "Google sign-in failed",
                throwable = e
            )
        }
    }

    override suspend fun signInAnonymously(): Resource<User> {
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
                supabaseClient.auth.signInAnonymously()
                val session = supabaseClient.auth.currentSessionOrNull()
                    ?: return Resource.Error("Anonymous sign-in failed: no session returned")
                val userId = session.user?.id
                    ?: return Resource.Error("Anonymous sign-in failed: no user ID")
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
            Resource.Error(
                message = e.message ?: "Anonymous sign-in failed",
                throwable = e
            )
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
            Resource.Error(
                message = e.message ?: "Failed to send OTP",
                throwable = e
            )
        }
    }

    override suspend fun verifyOtp(phoneNumber: String, otp: String): Resource<User> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Success(
                    MockDataSource.mockUser().copy(authProvider = "phone")
                )
            } else {
                supabaseClient.auth.verifyPhoneOtp(
                    type = io.github.jan.supabase.auth.OtpType.Phone.SMS,
                    phone = phoneNumber,
                    token = otp
                )
                val session = supabaseClient.auth.currentSessionOrNull()
                    ?: return Resource.Error("OTP verification failed: no session")
                val userId = session.user?.id
                    ?: return Resource.Error("OTP verification failed: no user ID")
                val profile = apiService.getProfile(userId)
                Resource.Success(
                    User(
                        id = userId,
                        displayName = profile.user.displayName,
                        avatarId = profile.user.avatarId,
                        gradeId = profile.user.gradeId,
                        gradeLabel = profile.user.gradeLabel,
                        authProvider = profile.user.authProvider,
                        isAnonymous = false,
                        countryName = profile.user.countryName,
                        cityName = profile.user.cityName,
                        schoolName = profile.user.schoolName
                    )
                )
            }
        } catch (e: Exception) {
            Resource.Error(
                message = e.message ?: "OTP verification failed",
                throwable = e
            )
        }
    }

    override suspend fun linkAccountWithGoogle(): Resource<Unit> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Success(Unit)
            } else {
                supabaseClient.auth.linkIdentity(Google)
                Resource.Success(Unit)
            }
        } catch (e: Exception) {
            Resource.Error(
                message = e.message ?: "Failed to link Google account",
                throwable = e
            )
        }
    }

    override suspend fun linkAccountWithPhone(phoneNumber: String): Resource<Unit> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Success(Unit)
            } else {
                supabaseClient.auth.updateUser {
                    this.phone = phoneNumber
                }
                Resource.Success(Unit)
            }
        } catch (e: Exception) {
            Resource.Error(
                message = e.message ?: "Failed to link phone number",
                throwable = e
            )
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
            Resource.Error(
                message = e.message ?: "Failed to create profile",
                throwable = e
            )
        }
    }

    override suspend fun updateProfile(userId: String, fields: Map<String, Any>): Resource<Unit> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Success(Unit)
            } else {
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
                Resource.Success(Unit)
            }
        } catch (e: Exception) {
            Resource.Error(
                message = e.message ?: "Failed to update profile",
                throwable = e
            )
        }
    }

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
                    countryName = profile.user.countryName,
                    cityName = profile.user.cityName,
                    schoolName = profile.user.schoolName
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun isAnonymous(): Boolean {
        return if (AppConstants.USE_MOCK_DATA) {
            false
        } else {
            val session = supabaseClient.auth.currentSessionOrNull()
            session?.user?.email.isNullOrBlank() && session?.user?.phone.isNullOrBlank()
        }
    }

    override suspend fun signOut(): Resource<Unit> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Success(Unit)
            } else {
                supabaseClient.auth.signOut()
                Resource.Success(Unit)
            }
        } catch (e: Exception) {
            Resource.Error(
                message = e.message ?: "Sign out failed",
                throwable = e
            )
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
                                countryName = profile.user.countryName,
                                cityName = profile.user.cityName,
                                schoolName = profile.user.schoolName
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }
                    else -> null
                }
            }
        }
    }
}
