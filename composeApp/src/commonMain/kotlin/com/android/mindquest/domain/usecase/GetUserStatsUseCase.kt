package com.android.mindquest.domain.usecase

import com.android.mindquest.core.util.Resource
import com.android.mindquest.domain.model.StatsData
import com.android.mindquest.domain.repository.StatsRepository

class GetUserStatsUseCase(
    private val repository: StatsRepository
) {
    suspend operator fun invoke(userId: String, period: String): Resource<StatsData> {
        return repository.getUserStats(userId, period)
    }
}
