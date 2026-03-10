package com.android.mindquest.domain.repository

import com.android.mindquest.core.util.Resource
import com.android.mindquest.domain.model.ProfileData

interface ProfileRepository {
    suspend fun getProfile(userId: String): Resource<ProfileData>
    suspend fun updateProfile(userId: String, fields: Map<String, Any>): Resource<Unit>
}
