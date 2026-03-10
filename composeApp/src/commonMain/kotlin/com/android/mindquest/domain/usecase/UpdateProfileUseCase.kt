package com.android.mindquest.domain.usecase

import com.android.mindquest.core.util.Resource
import com.android.mindquest.domain.repository.ProfileRepository

class UpdateProfileUseCase(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(userId: String, fields: Map<String, Any>): Resource<Unit> {
        return repository.updateProfile(userId, fields)
    }
}
