package com.android.mindquest.data.repository

import com.android.mindquest.core.constants.AppConstants
import com.android.mindquest.core.util.AppLogger
import com.android.mindquest.core.util.ErrorMapper
import com.android.mindquest.core.util.Resource
import com.android.mindquest.core.util.withRetry
import com.android.mindquest.data.mapper.toDomain
import com.android.mindquest.data.mock.MockDataSource
import com.android.mindquest.data.remote.ApiService
import com.android.mindquest.domain.model.LeaderboardData
import com.android.mindquest.domain.model.LeaderboardFilter
import com.android.mindquest.domain.repository.LeaderboardRepository

class LeaderboardRepositoryImpl(
    private val apiService: ApiService
) : LeaderboardRepository {

    override suspend fun getLeaderboard(
        userId: String,
        filter: LeaderboardFilter,
        filterId: String?,
        limit: Int,
        offset: Int
    ): Resource<LeaderboardData> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Success(MockDataSource.mockLeaderboard())
            } else {
                val filterStr = filter.name.lowercase()
                val response = withRetry { apiService.getLeaderboard(userId, filterStr, filterId, limit, offset) }
                Resource.Success(response.toDomain())
            }
        } catch (e: Exception) {
            AppLogger.e("LeaderboardRepo", "load leaderboard failed", e)
            Resource.Error(
                message = ErrorMapper.toUserMessage(e),
                throwable = e
            )
        }
    }
}
