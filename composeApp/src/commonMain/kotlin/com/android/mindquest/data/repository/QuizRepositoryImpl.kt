package com.android.mindquest.data.repository

import com.android.mindquest.core.constants.AppConstants
import com.android.mindquest.core.util.Resource
import com.android.mindquest.data.mapper.toDomain
import com.android.mindquest.data.mock.MockDataSource
import com.android.mindquest.data.remote.ApiService
import com.android.mindquest.domain.model.QuizResult
import com.android.mindquest.domain.model.QuizSubmitPayload
import com.android.mindquest.domain.repository.QuizRepository
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

class QuizRepositoryImpl(
    private val apiService: ApiService
) : QuizRepository {

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
                    put("user_id", JsonPrimitive(payload.userId))
                    put("quiz_id", JsonPrimitive(payload.quizId))
                    put("time_taken_secs", JsonPrimitive(payload.timeTakenSecs))
                    put("idempotency_key", JsonPrimitive(payload.idempotencyKey))
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
            Resource.Error(
                message = e.message ?: "Failed to submit quiz",
                throwable = e
            )
        }
    }
}
