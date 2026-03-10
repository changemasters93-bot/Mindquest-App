package com.android.mindquest.domain.model

data class LeaderboardEntry(
    val userId: String,
    val displayName: String,
    val avatarId: Int,
    val totalXp: Long,
    val rank: Int,
    val trend: String = "0" // "+3", "-1", "0"
)

data class UserRank(
    val rankGlobal: Int,
    val rankCountry: Int? = null,
    val rankCity: Int? = null,
    val xpGapToNext: Long? = null
)

data class LeaderboardData(
    val rankedUsers: List<LeaderboardEntry>,
    val userRank: UserRank
)

enum class LeaderboardFilter { GLOBAL, COUNTRY, CITY, SCHOOL }
