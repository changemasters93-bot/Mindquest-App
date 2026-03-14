package com.android.mindquest.domain.model

data class DailyChallenge(
    val quizId: String,
    val title: String,
    val description: String? = null,
    val questionCount: Int,
    val timeInMinutes: Int,
    val moduleId: String,
    val chapterId: String,
    val isDone: Boolean = false,
    val moduleColor: String = "",
    val moduleEmoji: String = "",
)
