package com.android.mindquest.domain.usecase

import com.android.mindquest.core.util.Resource
import com.android.mindquest.domain.model.QuizAnswer
import com.android.mindquest.domain.model.QuizResult
import com.android.mindquest.domain.repository.TournamentRepository

class SubmitTournamentUseCase(
    private val repository: TournamentRepository
) {
    suspend operator fun invoke(
        entryId: String,
        answers: List<QuizAnswer>,
        timeTaken: Int
    ): Resource<QuizResult> {
        return repository.submitTournament(entryId, answers, timeTaken)
    }
}
