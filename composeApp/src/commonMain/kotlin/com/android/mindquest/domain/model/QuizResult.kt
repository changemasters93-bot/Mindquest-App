package com.android.mindquest.domain.model

data class QuizResult(
    val status: String, // success / duplicate
    val attemptId: String? = null,
    val score: Int,
    val totalQuestions: Int,
    val xpEarned: Int,
    val totalXp: Long,
    val level: Int,
    val levelChanged: Boolean,
    val rankGlobal: Int,
    val isReplay: Boolean,
    val nextQuizId: String? = null,
    val iqScore: Int? = null
) {
    val accuracyPct: Int get() = if (totalQuestions > 0) (score * 100) / totalQuestions else 0
    val starRating: Int get() = when {
        accuracyPct >= 90 -> 5
        accuracyPct >= 75 -> 4
        accuracyPct >= 60 -> 3
        accuracyPct >= 40 -> 2
        else -> 1
    }
}
