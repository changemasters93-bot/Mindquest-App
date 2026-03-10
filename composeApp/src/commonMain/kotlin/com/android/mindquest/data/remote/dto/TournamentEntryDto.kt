package com.android.mindquest.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class TournamentStartResponseDto(
    @SerialName("entry_id") val entryId: String,
    val questions: List<QuestionDto>,
    @SerialName("time_limit_secs") val timeLimitSecs: Int
)

@Serializable
data class TournamentEntryDto(
    val id: String,
    @SerialName("tournament_id") val tournamentId: String,
    @SerialName("user_id") val userId: String,
    val status: String,
    val score: Int = 0,
    @SerialName("time_taken_seconds") val timeTakenSeconds: Int? = null,
    val rank: Int? = null,
    @SerialName("time_remaining_secs") val timeRemainingSecs: Int? = null,
    @SerialName("answers_so_far") val answersSoFar: List<JsonObject>? = null
)
