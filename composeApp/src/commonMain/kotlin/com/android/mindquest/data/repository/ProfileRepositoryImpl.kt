package com.android.mindquest.data.repository

import com.android.mindquest.core.constants.AppConstants
import com.android.mindquest.core.util.AppLogger
import com.android.mindquest.core.util.ErrorMapper
import com.android.mindquest.core.util.Resource
import com.android.mindquest.core.util.withRetry
import com.android.mindquest.data.mapper.toDomain
import com.android.mindquest.data.mock.MockDataSource
import com.android.mindquest.data.remote.ApiService
import com.android.mindquest.domain.model.ProfileData
import com.android.mindquest.domain.repository.ProfileRepository
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class ProfileRepositoryImpl(
    private val apiService: ApiService
) : ProfileRepository {

    override suspend fun getProfile(userId: String): Resource<ProfileData> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Success(MockDataSource.mockProfile())
            } else {
                val response = withRetry { apiService.getProfile(userId) }
                Resource.Success(response.toDomain())
            }
        } catch (e: Exception) {
            AppLogger.e("ProfileRepo", "load profile failed", e)
            Resource.Error(
                message = ErrorMapper.toUserMessage(e),
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
            AppLogger.e("ProfileRepo", "update profile failed", e)
            Resource.Error(
                message = ErrorMapper.toUserMessage(e),
                throwable = e
            )
        }
    }
}
