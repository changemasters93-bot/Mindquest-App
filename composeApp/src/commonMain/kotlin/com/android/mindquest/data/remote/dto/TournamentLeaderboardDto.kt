package com.android.mindquest.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response DTO for `get_tournament_leaderboard` RPC.
 *
 * This is distinct from [LeaderboardResponseDto] (used by `get_leaderboard`)
 * because the SQL returns different keys and field shapes:
 *  - `my_entry` instead of `user_rank`
 *  - `ranked_users` entries have `score` instead of `total_xp`
 *  - Includes `total_participants`
 */
@Serializable
data class TournamentLeaderboardResponseDto(
    @SerialName("ranked_users") val rankedUsers: List<TournamentLeaderboardEntryDto>,
    @SerialName("my_entry") val myEntry: TournamentMyEntryDto? = null,
    @SerialName("total_participants") val totalParticipants: Int = 0
)

@Serializable
data class TournamentLeaderboardEntryDto(
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_id") val avatarId: Int,
    val score: Int,
    @SerialName("time_taken_seconds") val timeTakenSeconds: Int? = null,
    @SerialName("entry_status") val entryStatus: String? = null,
    val rank: Int
)

@Serializable
data class TournamentMyEntryDto(
    val score: Int? = null,
    @SerialName("time_taken_seconds") val timeTakenSeconds: Int? = null,
    val status: String? = null,
    val rank: Int? = null,
    @SerialName("my_rank") val myRank: Int? = null
)
