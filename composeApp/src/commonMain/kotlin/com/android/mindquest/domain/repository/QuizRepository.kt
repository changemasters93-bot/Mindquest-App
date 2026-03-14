package com.android.mindquest.domain.repository

import com.android.mindquest.core.util.Resource
import com.android.mindquest.domain.model.Quiz
import com.android.mindquest.domain.model.QuizResult
import com.android.mindquest.domain.model.QuizSubmitPayload

interface QuizRepository {
    suspend fun getQuizWithQuestions(quizId: String, userId: String): Resource<Quiz>
    suspend fun submitQuizAttempt(payload: QuizSubmitPayload): Resource<QuizResult>
}
