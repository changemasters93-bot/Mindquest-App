package com.android.mindquest.domain.usecase

import com.android.mindquest.core.util.Resource
import com.android.mindquest.domain.model.ProfileData
import com.android.mindquest.domain.repository.ProfileRepository

class GetProfileUseCase(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(userId: String): Resource<ProfileData> {
        return repository.getProfile(userId)
    }
}
