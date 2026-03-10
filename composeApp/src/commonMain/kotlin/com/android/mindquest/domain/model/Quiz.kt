package com.android.mindquest.domain.model

data class Quiz(
    val id: String,
    val title: String,
    val quizType: String, // practice / iq / tournament
    val questionCount: Int,
    val timeLimitSeconds: Int,
    val maxXp: Int,
    val difficulty: String? = null,
    val displayOrder: Int,
    val bestScore: Int? = null, // null = never attempted
    val attemptCount: Int = 0,
    val isLocked: Boolean = false,
    val questions: List<Question> = emptyList()
) {
    val state: QuizState get() = when {
        isLocked -> QuizState.LOCKED
        bestScore != null -> QuizState.COMPLETED
        else -> QuizState.UNLOCKED
    }
}

enum class QuizState { COMPLETED, UNLOCKED, LOCKED }
