package com.android.mindquest.domain.repository

import com.android.mindquest.core.util.Resource
import com.android.mindquest.domain.model.DailyChallenge
import com.android.mindquest.domain.model.DashboardData

interface DashboardRepository {
    suspend fun getDashboard(userId: String): Resource<DashboardData>
    suspend fun getDailyChallenges(userId: String): Resource<List<DailyChallenge>>
}
