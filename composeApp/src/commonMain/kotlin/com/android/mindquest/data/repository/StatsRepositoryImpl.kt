package com.android.mindquest.data.repository

import com.android.mindquest.core.constants.AppConstants
import com.android.mindquest.core.util.AppLogger
import com.android.mindquest.core.util.ErrorMapper
import com.android.mindquest.core.util.Resource
import com.android.mindquest.core.util.withRetry
import com.android.mindquest.data.mapper.toDomain
import com.android.mindquest.data.mock.MockDataSource
import com.android.mindquest.data.remote.ApiService
import com.android.mindquest.domain.model.StatsData
import com.android.mindquest.domain.repository.StatsRepository

class StatsRepositoryImpl(
    private val apiService: ApiService
) : StatsRepository {

    override suspend fun getUserStats(userId: String, period: String): Resource<StatsData> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Success(MockDataSource.mockStats())
            } else {
                val response = withRetry { apiService.getUserStats(userId, period) }
                Resource.Success(response.toDomain())
            }
        } catch (e: Exception) {
            AppLogger.e("StatsRepo", "load stats failed", e)
            Resource.Error(
                message = ErrorMapper.toUserMessage(e),
                throwable = e
            )
        }
    }
}
