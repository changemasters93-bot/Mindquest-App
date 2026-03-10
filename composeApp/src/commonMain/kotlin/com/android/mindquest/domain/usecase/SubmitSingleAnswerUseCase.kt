package com.android.mindquest.domain.usecase

import com.android.mindquest.core.util.Resource
import com.android.mindquest.domain.model.QuizAnswer
import com.android.mindquest.domain.repository.TournamentRepository

/**
 * Submits a single answer for tournament per-question mode.
 * Called non-blocking in the background after each answer confirmation.
 *
 * This is a fire-and-forget operation — network failures are silently
 * handled. The batch submission at quiz end serves as the authoritative
 * record via [SubmitTournamentUseCase].
 */
class SubmitSingleAnswerUseCase(
    private val repository: TournamentRepository,
) {
    suspend operator fun invoke(
        entryId: String,
        answer: QuizAnswer,
    ): Resource<Unit> {
        return repository.submitSingleAnswer(entryId, answer)
    }
}
