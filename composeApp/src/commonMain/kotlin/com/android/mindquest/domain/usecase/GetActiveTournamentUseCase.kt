package com.android.mindquest.domain.usecase

import com.android.mindquest.core.util.Resource
import com.android.mindquest.domain.model.Tournament
import com.android.mindquest.domain.repository.TournamentRepository

class GetActiveTournamentUseCase(
    private val repository: TournamentRepository
) {
    suspend operator fun invoke(userId: String, gradeId: String): Resource<Tournament?> {
        return repository.getActiveTournament(userId, gradeId)
    }
}
