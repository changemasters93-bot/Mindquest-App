package com.android.mindquest.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardResponseDto(
    @SerialName("ranked_users") val rankedUsers: List<LeaderboardEntryDto>,
    @SerialName("user_rank") val userRank: UserRankDto
)

@Serializable
data class LeaderboardEntryDto(
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_id") val avatarId: Int,
    @SerialName("total_xp") val totalXp: Long,
    val rank: Int
)

@Serializable
data class UserRankDto(
    @SerialName("rank_global") val rankGlobal: Int,
    @SerialName("rank_country") val rankCountry: Int? = null,
    @SerialName("rank_city") val rankCity: Int? = null,
    @SerialName("xp_gap") val xpGap: Long? = null
)
