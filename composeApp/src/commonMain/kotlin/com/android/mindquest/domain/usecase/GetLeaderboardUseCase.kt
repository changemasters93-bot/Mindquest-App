package com.android.mindquest.domain.usecase

import com.android.mindquest.core.util.Resource
import com.android.mindquest.domain.model.LeaderboardData
import com.android.mindquest.domain.model.LeaderboardFilter
import com.android.mindquest.domain.repository.LeaderboardRepository

class GetLeaderboardUseCase(
    private val repository: LeaderboardRepository
) {
    suspend operator fun invoke(
        userId: String,
        filter: LeaderboardFilter,
        filterId: String? = null,
        limit: Int = 100,
        offset: Int = 0
    ): Resource<LeaderboardData> {
        return repository.getLeaderboard(userId, filter, filterId, limit, offset)
    }
}
