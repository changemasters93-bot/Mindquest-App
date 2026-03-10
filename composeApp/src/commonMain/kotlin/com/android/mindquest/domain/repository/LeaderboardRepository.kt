package com.android.mindquest.domain.repository

import com.android.mindquest.core.util.Resource
import com.android.mindquest.domain.model.LeaderboardData
import com.android.mindquest.domain.model.LeaderboardFilter

interface LeaderboardRepository {
    suspend fun getLeaderboard(
        userId: String,
        filter: LeaderboardFilter,
        filterId: String? = null,
        limit: Int = 100,
        offset: Int = 0
    ): Resource<LeaderboardData>
}
