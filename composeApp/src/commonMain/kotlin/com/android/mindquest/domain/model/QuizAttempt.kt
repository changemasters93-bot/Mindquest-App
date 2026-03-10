package com.android.mindquest.domain.model

data class QuizAnswer(
    val questionId: String,
    val selected: String, // option id or answer
    val isCorrect: Boolean,
    val timeMs: Long
)

data class QuizSubmitPayload(
    val userId: String,
    val quizId: String,
    val answers: List<QuizAnswer>,
    val timeTakenSecs: Int,
    val idempotencyKey: String
)
