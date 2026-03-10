package com.android.mindquest.domain.model

data class ProfileData(
    val user: User,
    val stats: UserStats,
    val completedChapters: List<CompletedChapter> = emptyList(),
    val tournamentResults: List<TournamentResult> = emptyList()
)

data class CompletedChapter(
    val chapterId: String,
    val chapterTitle: String,
    val moduleTitle: String,
    val moduleEmoji: String,
    val completedAt: String? = null
)

data class TournamentResult(
    val tournamentId: String,
    val title: String,
    val score: Int,
    val totalQuestions: Int,
    val rank: Int,
    val participantCount: Int,
    val certificateUrl: String? = null,
    val date: String? = null
)
