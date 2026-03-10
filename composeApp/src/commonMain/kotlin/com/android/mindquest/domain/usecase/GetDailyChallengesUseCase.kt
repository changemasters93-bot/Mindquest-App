package com.android.mindquest.domain.usecase

import com.android.mindquest.core.util.Resource
import com.android.mindquest.domain.model.DailyChallenge
import com.android.mindquest.domain.repository.DashboardRepository

class GetDailyChallengesUseCase(
    private val repository: DashboardRepository
) {
    suspend operator fun invoke(userId: String): Resource<List<DailyChallenge>> {
        return repository.getDailyChallenges(userId)
    }
}
