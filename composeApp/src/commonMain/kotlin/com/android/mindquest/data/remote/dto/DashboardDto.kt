package com.android.mindquest.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DashboardResponseDto(
    val user: UserBasicDto,
    val stats: UserStatsDto,
    val modules: List<ModuleWithProgressDto>,
    @SerialName("active_tournament") val activeTournament: TournamentInfoDto? = null
)

@Serializable
data class UserBasicDto(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_id") val avatarId: Int,
    @SerialName("grade_id") val gradeId: String,
    @SerialName("auth_provider") val authProvider: String
)

@Serializable
data class UserStatsDto(
    @SerialName("total_xp") val totalXp: Long,
    val level: Int,
    @SerialName("streak_current") val streakCurrent: Int = 0,
    @SerialName("streak_best") val streakBest: Int = 0,
    @SerialName("quizzes_completed") val quizzesCompleted: Int = 0,
    @SerialName("accuracy_pct") val accuracyPct: Double = 0.0,
    @SerialName("tournaments_played") val tournamentsPlayed: Int = 0,
    @SerialName("best_tournament_rank") val bestTournamentRank: Int? = null,
    @SerialName("iq_best_score") val iqBestScore: Int? = null,
    @SerialName("last_iq_attempt_at") val lastIqAttemptAt: String? = null,
    @SerialName("iq_cooldown_hours") val iqCooldownHours: Int? = null,
    @SerialName("iq_quiz_id") val iqQuizId: String? = null
)

@Serializable
data class ModuleWithProgressDto(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val emoji: String,
    @SerialName("accent_color") val accentColor: String,
    @SerialName("sort_order") val sortOrder: Int,
    val progress: ModuleProgressDto? = null
)

@Serializable
data class ModuleProgressDto(
    @SerialName("current_chapter_id") val currentChapterId: String? = null,
    @SerialName("current_quiz_id") val currentQuizId: String? = null,
    @SerialName("best_score_pct") val bestScorePct: Int? = null,
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("completed_chapters") val completedChapters: Int = 0,
    @SerialName("total_chapters") val totalChapters: Int = 0,
    @SerialName("total_xp_earned") val totalXpEarned: Long = 0,
)

@Serializable
data class TournamentInfoDto(
    val id: String,
    val title: String,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
    val status: String,
    @SerialName("user_entry_status") val userEntryStatus: String? = null,
    @SerialName("question_count") val questionCount: Int = 0,
    @SerialName("time_limit_seconds") val timeLimitSeconds: Int = 0,
    @SerialName("participant_count") val participantCount: Int = 0
)
