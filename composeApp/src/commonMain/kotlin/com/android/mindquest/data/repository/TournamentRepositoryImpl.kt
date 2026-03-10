package com.android.mindquest.data.repository

import com.android.mindquest.core.constants.AppConstants
import com.android.mindquest.core.util.Resource
import com.android.mindquest.data.mapper.toDomain
import com.android.mindquest.data.mock.MockDataSource
import com.android.mindquest.data.remote.ApiService
import com.android.mindquest.domain.model.LeaderboardEntry
import com.android.mindquest.domain.model.QuizAnswer
import com.android.mindquest.domain.model.QuizResult
import com.android.mindquest.domain.model.Tournament
import com.android.mindquest.domain.model.TournamentEntry
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
                val response = apiService.getActiveTournament(userId, gradeId)
                Resource.Success(response?.toDomain())
            }
        } catch (e: Exception) {
            Resource.Error(
                message = e.message ?: "Failed to load tournament",
                throwable = e
            )
        }
    }

    override suspend fun startTournament(
        userId: String,
        tournamentId: String
    ): Resource<TournamentEntry> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Success(MockDataSource.mockTournamentEntry())
            } else {
                val response = apiService.startTournament(userId, tournamentId)
                Resource.Success(
                    TournamentEntry(
                        id = response.entryId,
                        tournamentId = tournamentId,
                        userId = userId,
                        status = com.android.mindquest.domain.model.TournamentEntryStatus.IN_PROGRESS,
                        timeRemainingSeconds = response.timeLimitSecs
                    )
                )
            }
        } catch (e: Exception) {
            Resource.Error(
                message = e.message ?: "Failed to start tournament",
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
            Resource.Error(
                message = e.message ?: "Failed to pause tournament",
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
            Resource.Error(
                message = e.message ?: "Failed to resume tournament",
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
            Resource.Error(
                message = e.message ?: "Failed to submit tournament",
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
            Resource.Error(
                message = e.message ?: "Failed to submit answer",
                throwable = e,
            )
        }
    }

    override fun observeTournamentLeaderboard(tournamentId: String): Flow<List<LeaderboardEntry>> {
        // For mock mode, emit a static leaderboard. In production, this would
        // subscribe to Supabase Realtime changes on the tournament_entries table.
        return if (AppConstants.USE_MOCK_DATA) {
            flowOf(MockDataSource.mockLeaderboard().rankedUsers)
        } else {
            // TODO: Implement Supabase Realtime subscription for live leaderboard
            flowOf(emptyList())
        }
    }
}
