package com.android.mindquest.domain.repository

import com.android.mindquest.core.util.Resource
import com.android.mindquest.domain.model.StatsData

interface StatsRepository {
    suspend fun getUserStats(userId: String, period: String): Resource<StatsData>
}
