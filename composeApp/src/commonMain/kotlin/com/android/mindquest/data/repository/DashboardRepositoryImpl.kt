package com.android.mindquest.data.repository

import com.android.mindquest.core.constants.AppConstants
import com.android.mindquest.core.util.Resource
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
                val response = apiService.getUserDashboard(userId)
                Resource.Success(response.toDomain())
            }
        } catch (e: Exception) {
            Resource.Error(
                message = e.message ?: "Failed to load dashboard",
                throwable = e
            )
        }
    }

    override suspend fun getDailyChallenges(userId: String): Resource<List<DailyChallenge>> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Success(MockDataSource.mockDailyChallenges())
            } else {
                val response = apiService.getDailyChallenges(userId)
                Resource.Success(response.map { it.toDomain() })
            }
        } catch (e: Exception) {
            Resource.Error(
                message = e.message ?: "Failed to load daily challenges",
                throwable = e
            )
        }
    }
}
