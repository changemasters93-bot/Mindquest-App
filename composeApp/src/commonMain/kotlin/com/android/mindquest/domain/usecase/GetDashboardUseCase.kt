package com.android.mindquest.domain.usecase

import com.android.mindquest.core.util.Resource
import com.android.mindquest.domain.model.DashboardData
import com.android.mindquest.domain.repository.DashboardRepository

class GetDashboardUseCase(
    private val repository: DashboardRepository
) {
    suspend operator fun invoke(userId: String): Resource<DashboardData> {
        return repository.getDashboard(userId)
    }
}
