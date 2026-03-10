package com.android.mindquest.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ModuleFullResponseDto(
    val module: ModuleDto,
    val chapters: List<ChapterDto>
)

@Serializable
data class ModuleDto(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val emoji: String,
    @SerialName("accent_color") val accentColor: String,
    @SerialName("display_order") val displayOrder: Int
)

@Serializable
data class ChapterDto(
    val id: String,
    val title: String,
    @SerialName("chapter_number") val chapterNumber: Int,
    @SerialName("quiz_count") val quizCount: Int = 0,
    val progress: ChapterProgressDto? = null,
    val state: String = "locked"
)

@Serializable
data class ChapterProgressDto(
    @SerialName("quizzes_done") val quizzesDone: Int = 0,
    @SerialName("total_quizzes") val totalQuizzes: Int = 0,
    @SerialName("best_score_pct") val bestScorePct: Int? = null,
    @SerialName("is_completed") val isCompleted: Boolean = false
)

@Serializable
data class QuizDto(
    val id: String,
    val title: String,
    @SerialName("quiz_type") val quizType: String,
    @SerialName("question_count") val questionCount: Int,
    @SerialName("time_limit_secs") val timeLimitSecs: Int,
    @SerialName("max_xp") val maxXp: Int,
    val difficulty: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("best_score") val bestScore: Int? = null,
    @SerialName("attempt_count") val attemptCount: Int = 0,
    val questions: List<QuestionDto> = emptyList()
)

@Serializable
data class QuestionDto(
    val id: String,
    @SerialName("question_type") val questionType: String,
    val title: String,
    val prompt: String? = null,
    val explanation: String = "",
    val difficulty: String? = null,
    @SerialName("time_limit_secs") val timeLimitSecs: Int? = null,
    @SerialName("allow_multiple") val allowMultiple: Boolean = false,
    @SerialName("prompt_config") val promptConfig: JsonObject? = null,
    val metadata: JsonObject? = null,
    @SerialName("media_url") val mediaUrl: String? = null,
    val options: List<OptionDto> = emptyList(),
    @SerialName("match_pairs") val matchPairs: List<MatchPairDto>? = null
)

@Serializable
data class OptionDto(
    val id: String,
    val label: String,
    @SerialName("is_correct") val isCorrect: Boolean = false,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("correct_position") val correctPosition: Int? = null,
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("visual_label") val visualLabel: String? = null
)

@Serializable
data class MatchPairDto(
    val id: String,
    @SerialName("left_text") val leftText: String,
    @SerialName("right_text") val rightText: String,
    @SerialName("sort_order") val sortOrder: Int = 0
)
