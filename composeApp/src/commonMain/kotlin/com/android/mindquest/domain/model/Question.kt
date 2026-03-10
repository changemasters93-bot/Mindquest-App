package com.android.mindquest.domain.model

data class Question(
    val id: String,
    val questionType: QuestionType,
    val title: String,
    val prompt: String? = null,
    val explanation: String,
    val difficulty: String? = null,
    val timeLimitSeconds: Int? = null,
    val allowMultiple: Boolean = false,
    val promptConfig: Map<String, Any>? = null,
    val metadata: Map<String, Any>? = null,
    val mediaUrl: String? = null,
    val options: List<QuestionOption> = emptyList(),
    val matchPairs: List<MatchPair>? = null
)

enum class QuestionType {
    MULTIPLE_CHOICE, TRUE_FALSE, ORDERING, MATCH, FILL_BLANK,
    SELECT_WORD, MATRIX, GRID_PATTERN, STATEMENT_REASON,
    TABLE_DATA, MEMORY, VISUAL_SINGLE_CHOICE;

    companion object {
        fun fromString(value: String): QuestionType = when (value.lowercase()) {
            "multiple_choice" -> MULTIPLE_CHOICE
            "true_false" -> TRUE_FALSE
            "ordering" -> ORDERING
            "match" -> MATCH
            "fill_blank" -> FILL_BLANK
            "select_word" -> SELECT_WORD
            "matrix" -> MATRIX
            "grid_pattern" -> GRID_PATTERN
            "statement_reason" -> STATEMENT_REASON
            "table_data" -> TABLE_DATA
            "memory" -> MEMORY
            "visual_single_choice" -> VISUAL_SINGLE_CHOICE
            else -> MULTIPLE_CHOICE
        }
    }
}

data class QuestionOption(
    val id: String,
    val label: String,
    val isCorrect: Boolean,
    val displayOrder: Int,
    val correctPosition: Int? = null,
    val mediaUrl: String? = null,
    val visualLabel: String? = null,
    val metadata: Map<String, Any>? = null
)

data class MatchPair(
    val id: String,
    val leftText: String,
    val rightText: String,
    val displayOrder: Int = 0
)
