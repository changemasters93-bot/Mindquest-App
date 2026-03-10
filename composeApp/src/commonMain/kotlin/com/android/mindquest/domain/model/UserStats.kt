package com.android.mindquest.domain.model

data class UserStats(
    val totalXp: Long,
    val level: Int,
    val streakCurrent: Int,
    val streakBest: Int,
    val quizzesCompleted: Int,
    val accuracyPct: Double = 0.0,
    val tournamentsPlayed: Int = 0,
    val bestTournamentRank: Int? = null,
    val iqScore: Int? = null,
)
