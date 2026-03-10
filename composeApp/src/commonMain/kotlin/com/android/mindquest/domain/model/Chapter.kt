package com.android.mindquest.domain.model

data class Chapter(
    val id: String,
    val title: String,
    val chapterNumber: Int,
    val quizCount: Int = 0,
    val progress: ChapterProgress? = null,
    val state: ChapterState = ChapterState.LOCKED,
    val quizzes: List<Quiz> = emptyList()
)

enum class ChapterState { COMPLETED, UNLOCKED, LOCKED }

data class ChapterProgress(
    val quizzesDone: Int,
    val totalQuizzes: Int,
    val bestScorePct: Int? = null,
    val isCompleted: Boolean = false
)
