package com.android.mindquest.data.repository

import com.android.mindquest.core.constants.AppConstants
import com.android.mindquest.core.util.AppLogger
import com.android.mindquest.core.util.ErrorMapper
import com.android.mindquest.core.util.Resource
import com.android.mindquest.core.util.withRetry
import com.android.mindquest.data.mapper.toDomain
import com.android.mindquest.data.mock.MockDataSource
import com.android.mindquest.data.remote.ApiService
import com.android.mindquest.domain.model.DailyChallenge
import com.android.mindquest.domain.model.DashboardData
import com.android.mindquest.domain.repository.DashboardRepository

class DashboardRepositoryImpl(
    private val apiService: ApiService
) : DashboardRepository {

    override suspend fun getDashboard(userId: String): Resource<DashboardData> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Success(MockDataSource.mockDashboard())
            } else {
                val response = withRetry { apiService.getUserDashboard(userId) }
                Resource.Success(response.toDomain())
            }
        } catch (e: Exception) {
            AppLogger.e("DashboardRepo", "load dashboard failed", e)
            Resource.Error(
                message = ErrorMapper.toUserMessage(e),
                throwable = e
            )
        }
    }

    override suspend fun getDailyChallenges(userId: String): Resource<List<DailyChallenge>> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Success(MockDataSource.mockDailyChallenges())
            } else {
                val response = withRetry { apiService.getDailyChallenges(userId) }
                Resource.Success(response.map { it.toDomain() })
            }
        } catch (e: Exception) {
            AppLogger.e("DashboardRepo", "load daily challenges failed", e)
            Resource.Error(
                message = ErrorMapper.toUserMessage(e),
                throwable = e
            )
        }
    }
}
