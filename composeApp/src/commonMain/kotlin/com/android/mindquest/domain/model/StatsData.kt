package com.android.mindquest.domain.model

data class StatsData(
    val stats: UserStats,
    val accuracyPct: Double,
    val dailyActivity: List<DailyActivity>,
    val subjectPerformance: List<SubjectPerformance>
)

data class DailyActivity(
    val date: String,
    val quizzes: Int,
    val xp: Int,
    val minutesSpent: Int = 0
)

data class SubjectPerformance(
    val moduleId: String,
    val title: String,
    val emoji: String,
    val bestScorePct: Int,
    val accuracyPct: Int,
    val chaptersCompleted: Int = 0,
    val totalChapters: Int = 0
)
