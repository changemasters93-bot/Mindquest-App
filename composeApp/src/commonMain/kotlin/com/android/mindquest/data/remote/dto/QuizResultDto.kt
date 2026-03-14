package com.android.mindquest.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuizResultDto(
    val status: String,
    @SerialName("attempt_id") val attemptId: String? = null,
    val score: Int = 0,
    @SerialName("total_questions") val totalQuestions: Int = 0,
    @SerialName("xp_earned") val xpEarned: Int = 0,
    @SerialName("total_xp") val totalXp: Long = 0,
    val level: Int = 1,
    @SerialName("level_changed") val levelChanged: Boolean = false,
    @SerialName("rank_global") val rankGlobal: Int = 0,
    @SerialName("is_replay") val isReplay: Boolean = false,
    @SerialName("next_quiz_id") val nextQuizId: String? = null,
    @SerialName("iq_score") val iqScore: Int? = null
)
