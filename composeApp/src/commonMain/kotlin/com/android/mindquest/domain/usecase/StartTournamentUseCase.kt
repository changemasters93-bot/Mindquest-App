package com.android.mindquest.domain.usecase

import com.android.mindquest.core.util.Resource
import com.android.mindquest.domain.model.TournamentEntry
import com.android.mindquest.domain.repository.TournamentRepository

class StartTournamentUseCase(
    private val repository: TournamentRepository
) {
    suspend operator fun invoke(userId: String, tournamentId: String): Resource<TournamentEntry> {
        return repository.startTournament(userId, tournamentId)
    }
}
