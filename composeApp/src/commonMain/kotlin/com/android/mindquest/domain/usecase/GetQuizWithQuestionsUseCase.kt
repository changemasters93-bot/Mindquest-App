package com.android.mindquest.domain.usecase

import com.android.mindquest.core.util.Resource
import com.android.mindquest.domain.model.Quiz
import com.android.mindquest.domain.repository.QuizRepository

class GetQuizWithQuestionsUseCase(
    private val repository: QuizRepository
) {
    suspend operator fun invoke(quizId: String, userId: String): Resource<Quiz> {
        return repository.getQuizWithQuestions(quizId, userId)
    }
}
