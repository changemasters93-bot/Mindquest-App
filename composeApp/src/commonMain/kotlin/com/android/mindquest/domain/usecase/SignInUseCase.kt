package com.android.mindquest.domain.usecase

import com.android.mindquest.core.util.Resource
import com.android.mindquest.domain.model.User
import com.android.mindquest.domain.repository.AuthRepository

class SignInUseCase(
    private val repository: AuthRepository
) {
    suspend fun withGoogle(): Resource<User> {
        return repository.signInWithGoogle()
    }

    suspend fun anonymously(): Resource<User> {
        return repository.signInAnonymously()
    }

    suspend fun withPhone(phoneNumber: String): Resource<Unit> {
        return repository.signInWithPhone(phoneNumber)
    }

    suspend fun verifyOtp(phoneNumber: String, otp: String): Resource<User> {
        return repository.verifyOtp(phoneNumber, otp)
    }

    suspend operator fun invoke(method: SignInMethod): Resource<User> {
        return when (method) {
            SignInMethod.Google -> repository.signInWithGoogle()
            SignInMethod.Anonymous -> repository.signInAnonymously()
        }
    }
}

enum class SignInMethod { Google, Anonymous }
