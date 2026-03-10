package com.android.mindquest.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DailyChallengeDto(
    @SerialName("quiz_id") val quizId: String,
    val title: String,
    val description: String? = null,
    @SerialName("question_count") val questionCount: Int,
    @SerialName("time_in_minutes") val timeInMinutes: Int,
    @SerialName("module_id") val moduleId: String,
    @SerialName("chapter_id") val chapterId: String,
    @SerialName("is_done") val isDone: Boolean = false
)
