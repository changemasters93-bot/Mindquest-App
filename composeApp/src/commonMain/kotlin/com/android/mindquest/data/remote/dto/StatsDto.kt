package com.android.mindquest.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StatsResponseDto(
    val stats: UserStatsDto,
    @SerialName("accuracy_pct") val accuracyPct: Double = 0.0,
    @SerialName("daily_activity") val dailyActivity: List<DailyActivityDto> = emptyList(),
    @SerialName("subject_performance") val subjectPerformance: List<SubjectPerformanceDto> = emptyList()
)

@Serializable
data class DailyActivityDto(
    val date: String,
    val quizzes: Int = 0,
    val xp: Int = 0,
    @SerialName("time_spent_minutes") val timeSpentMinutes: Int = 0
)

@Serializable
data class SubjectPerformanceDto(
    @SerialName("module_id") val moduleId: String,
    val title: String,
    val emoji: String,
    @SerialName("best_score_pct") val bestScorePct: Int = 0,
    @SerialName("accuracy_pct") val accuracyPct: Int = 0,
    @SerialName("chapters_completed") val chaptersCompleted: Int = 0,
    @SerialName("total_chapters") val totalChapters: Int = 0
)
