package com.android.mindquest.domain.usecase

import com.android.mindquest.core.util.Resource
import com.android.mindquest.domain.model.QuizResult
import com.android.mindquest.domain.model.QuizSubmitPayload
import com.android.mindquest.domain.repository.QuizRepository

class SubmitQuizAttemptUseCase(
    private val repository: QuizRepository
) {
    suspend operator fun invoke(payload: QuizSubmitPayload): Resource<QuizResult> {
        return repository.submitQuizAttempt(payload)
    }
}
