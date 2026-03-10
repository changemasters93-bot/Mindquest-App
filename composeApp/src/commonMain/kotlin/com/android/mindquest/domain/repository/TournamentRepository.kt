package com.android.mindquest.domain.repository

import com.android.mindquest.core.util.Resource
import com.android.mindquest.domain.model.LeaderboardEntry
import com.android.mindquest.domain.model.QuizAnswer
import com.android.mindquest.domain.model.QuizResult
import com.android.mindquest.domain.model.Tournament
import com.android.mindquest.domain.model.TournamentEntry
import kotlinx.coroutines.flow.Flow

interface TournamentRepository {
    suspend fun getActiveTournament(userId: String, gradeId: String): Resource<Tournament?>
    suspend fun startTournament(userId: String, tournamentId: String): Resource<TournamentEntry>
    suspend fun pauseTournament(entryId: String): Resource<Int> // returns timeRemaining
    suspend fun resumeTournament(entryId: String): Resource<TournamentEntry>
    suspend fun submitTournament(entryId: String, answers: List<QuizAnswer>, timeTaken: Int): Resource<QuizResult>
    /** Submit a single answer in background for per-question tournament mode */
    suspend fun submitSingleAnswer(entryId: String, answer: QuizAnswer): Resource<Unit>
    fun observeTournamentLeaderboard(tournamentId: String): Flow<List<LeaderboardEntry>>
}
