package com.android.mindquest.data.repository

import com.android.mindquest.core.constants.AppConstants
import com.android.mindquest.core.util.AppLogger
import com.android.mindquest.core.util.ErrorMapper
import com.android.mindquest.core.util.Resource
import com.android.mindquest.core.util.withRetry
import com.android.mindquest.data.mapper.toDomain
import com.android.mindquest.data.mock.MockDataSource
import com.android.mindquest.data.remote.ApiService
import com.android.mindquest.domain.model.Quiz
import com.android.mindquest.domain.model.QuizResult
import com.android.mindquest.domain.model.QuizSubmitPayload
import com.android.mindquest.domain.repository.QuizRepository
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

class QuizRepositoryImpl(
    private val apiService: ApiService
) : QuizRepository {

    override suspend fun getQuizWithQuestions(quizId: String, userId: String): Resource<Quiz> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Error("Not available in mock mode")
            } else {
                val dto = withRetry { apiService.getQuizWithQuestions(quizId, userId) }
                Resource.Success(dto.toDomain())
            }
        } catch (e: Exception) {
            AppLogger.e("QuizRepo", "load quiz failed", e)
            Resource.Error(
                message = ErrorMapper.toUserMessage(e),
                throwable = e
            )
        }
    }

    override suspend fun submitQuizAttempt(payload: QuizSubmitPayload): Resource<QuizResult> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                val correctCount = payload.answers.count { it.isCorrect }
                Resource.Success(
                    MockDataSource.mockQuizResult(
                        score = correctCount,
                        total = payload.answers.size
                    )
                )
            } else {
                val jsonPayload = buildJsonObject {
                    put("userId", JsonPrimitive(payload.userId))
                    put("quizId", JsonPrimitive(payload.quizId))
                    put("timeTakenSecs", JsonPrimitive(payload.timeTakenSecs))
                    put("idempotencyKey", JsonPrimitive(payload.idempotencyKey))
                    put("answers", buildJsonArray {
                        payload.answers.forEach { answer ->
                            add(buildJsonObject {
                                put("question_id", JsonPrimitive(answer.questionId))
                                put("selected", JsonPrimitive(answer.selected))
                                put("is_correct", JsonPrimitive(answer.isCorrect))
                                put("time_ms", JsonPrimitive(answer.timeMs))
                            })
                        }
                    })
                }
                val response = apiService.submitQuizAttempt(jsonPayload)
                Resource.Success(response.toDomain())
            }
        } catch (e: Exception) {
            AppLogger.e("QuizRepo", "submit quiz failed", e)
            Resource.Error(
                message = ErrorMapper.toUserMessage(e),
                throwable = e
            )
        }
    }
}
