package com.android.mindquest.data.repository

import com.android.mindquest.core.constants.AppConstants
import com.android.mindquest.core.util.AppLogger
import com.android.mindquest.core.util.ErrorMapper
import com.android.mindquest.core.util.Resource
import com.android.mindquest.core.util.withRetry
import com.android.mindquest.data.mapper.toDomain
import com.android.mindquest.data.mock.MockDataSource
import com.android.mindquest.data.remote.ApiService
import com.android.mindquest.domain.model.LeaderboardEntry
import com.android.mindquest.domain.model.QuizAnswer
import com.android.mindquest.domain.model.QuizResult
import com.android.mindquest.domain.model.Quiz
import com.android.mindquest.domain.model.Tournament
import com.android.mindquest.domain.model.TournamentEntry
import com.android.mindquest.domain.model.TournamentEntryStatus
import com.android.mindquest.domain.model.TournamentStartResult
import com.android.mindquest.domain.repository.TournamentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

class TournamentRepositoryImpl(
    private val apiService: ApiService
) : TournamentRepository {

    override suspend fun getActiveTournament(
        userId: String,
        gradeId: String
    ): Resource<Tournament?> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                // Use scenario-aware mock data so lobby sees correct userEntryStatus
                Resource.Success(MockDataSource.mockDashboard().activeTournament)
            } else {
                val response = withRetry { apiService.getActiveTournament(userId, gradeId) }
                Resource.Success(response?.toDomain())
            }
        } catch (e: Exception) {
            AppLogger.e("TournamentRepo", "load tournament failed", e)
            Resource.Error(
                message = ErrorMapper.toUserMessage(e),
                throwable = e
            )
        }
    }

    override suspend fun getTournamentEntry(
        userId: String,
        tournamentId: String
    ): Resource<TournamentEntry?> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Success(MockDataSource.mockCompletedTournamentEntry())
            } else {
                val dto = withRetry { apiService.getTournamentEntry(userId, tournamentId) }
                Resource.Success(dto?.toDomain())
            }
        } catch (e: Exception) {
            AppLogger.e("TournamentRepo", "load tournament entry failed", e)
            Resource.Error(
                message = ErrorMapper.toUserMessage(e),
                throwable = e
            )
        }
    }

    override suspend fun startTournament(
        userId: String,
        tournamentId: String
    ): Resource<TournamentStartResult> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Success(
                    TournamentStartResult(
                        entry = MockDataSource.mockTournamentEntry(),
                        quiz = MockDataSource.mockTournamentQuiz(),
                    )
                )
            } else {
                val response = apiService.startTournament(userId, tournamentId)
                val entry = TournamentEntry(
                    id = response.entryId,
                    tournamentId = tournamentId,
                    userId = userId,
                    status = TournamentEntryStatus.IN_PROGRESS,
                    timeRemainingSeconds = response.timeLimitSecs
                )
                val questions = response.questions.map { it.toDomain() }
                val quiz = Quiz(
                    id = "tournament_$tournamentId",
                    title = "Tournament",
                    quizType = "tournament",
                    questionCount = questions.size,
                    timeLimitSeconds = response.timeLimitSecs,
                    maxXp = 0,
                    displayOrder = 0,
                    questions = questions,
                )
                Resource.Success(TournamentStartResult(entry = entry, quiz = quiz))
            }
        } catch (e: Exception) {
            AppLogger.e("TournamentRepo", "start tournament failed", e)
            Resource.Error(
                message = ErrorMapper.toUserMessage(e),
                throwable = e
            )
        }
    }

    override suspend fun pauseTournament(entryId: String): Resource<Int> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Success(420) // 7 minutes remaining
            } else {
                val response = apiService.pauseTournament(entryId)
                Resource.Success(response.timeRemainingSecs ?: 0)
            }
        } catch (e: Exception) {
            AppLogger.e("TournamentRepo", "pause tournament failed", e)
            Resource.Error(
                message = ErrorMapper.toUserMessage(e),
                throwable = e
            )
        }
    }

    override suspend fun resumeTournament(entryId: String): Resource<TournamentEntry> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Success(MockDataSource.mockTournamentEntry())
            } else {
                val response = apiService.resumeTournament(entryId)
                Resource.Success(response.toDomain())
            }
        } catch (e: Exception) {
            AppLogger.e("TournamentRepo", "resume tournament failed", e)
            Resource.Error(
                message = ErrorMapper.toUserMessage(e),
                throwable = e
            )
        }
    }

    override suspend fun submitTournament(
        entryId: String,
        answers: List<QuizAnswer>,
        timeTaken: Int
    ): Resource<QuizResult> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                val correctCount = answers.count { it.isCorrect }
                Resource.Success(MockDataSource.mockQuizResult(correctCount, answers.size))
            } else {
                val answersJson = buildJsonObject {
                    put("items", buildJsonArray {
                        answers.forEach { answer ->
                            add(buildJsonObject {
                                put("question_id", JsonPrimitive(answer.questionId))
                                put("selected", JsonPrimitive(answer.selected))
                                put("is_correct", JsonPrimitive(answer.isCorrect))
                                put("time_ms", JsonPrimitive(answer.timeMs))
                            })
                        }
                    })
                }
                val response = apiService.submitTournament(entryId, answersJson, timeTaken)
                Resource.Success(response.toDomain())
            }
        } catch (e: Exception) {
            AppLogger.e("TournamentRepo", "submit tournament failed", e)
            Resource.Error(
                message = ErrorMapper.toUserMessage(e),
                throwable = e
            )
        }
    }

    override suspend fun submitSingleAnswer(
        entryId: String,
        answer: QuizAnswer,
    ): Resource<Unit> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                // Mock: just acknowledge the answer
                Resource.Success(Unit)
            } else {
                val answerJson = buildJsonObject {
                    put("question_id", JsonPrimitive(answer.questionId))
                    put("selected", JsonPrimitive(answer.selected))
                    put("is_correct", JsonPrimitive(answer.isCorrect))
                    put("time_ms", JsonPrimitive(answer.timeMs))
                }
                apiService.submitTournamentAnswer(entryId, answerJson)
                Resource.Success(Unit)
            }
        } catch (e: Exception) {
            // Fire-and-forget: log but don't block the user
            AppLogger.e("TournamentRepo", "submit answer failed", e)
            Resource.Error(
                message = ErrorMapper.toUserMessage(e),
                throwable = e,
            )
        }
    }

    override fun observeTournamentLeaderboard(tournamentId: String): Flow<List<LeaderboardEntry>> {
        // For mock mode, emit a static leaderboard. In production, fetch from API.
        // Note: A full Supabase Realtime subscription could be added later for live updates.
        return if (AppConstants.USE_MOCK_DATA) {
            flowOf(MockDataSource.mockLeaderboard().rankedUsers)
        } else {
            kotlinx.coroutines.flow.flow {
                try {
                    val response = apiService.getTournamentLeaderboard(tournamentId, "", 50, 0)
                    emit(response.rankedUsers.map { it.toDomain() })
                } catch (e: Exception) {
                    AppLogger.e("TournamentRepo", "load tournament leaderboard failed", e)
                    emit(emptyList())
                }
            }
        }
    }
}
