package com.android.mindquest.domain.model

/**
 * Behavioral category of a quiz session.
 * Each value maps to a specific combination of feedback, submission,
 * and UI behavior flags via [QuizConfig] factory methods.
 *
 * Adding a new quiz type = adding a new enum value + a new factory method.
 */
enum class QuizBehavior {
    /** Module practice quiz or daily mission — full feedback + nudges */
    MODULE,
    /** IQ assessment — no feedback, timed, cooldown-based */
    IQ_TEST,
    /** Live tournament — no feedback, per-question submission */
    TOURNAMENT,
}

/**
 * Controls how quiz answers are submitted to the backend.
 */
enum class SubmissionMode {
    /** All answers collected and submitted together when quiz finishes */
    BATCH_AT_END,
    /** Each answer submitted immediately in background (non-blocking).
     *  A final batch is also sent at end as authoritative record. */
    PER_QUESTION_NON_BLOCKING,
}

/**
 * Controls timer granularity (extensible for future per-question timers).
 */
enum class TimerScope {
    /** One countdown timer for the entire quiz */
    QUIZ_LEVEL,
    /** Separate countdown per question (future extension) */
    PER_QUESTION,
}

/**
 * Configuration-driven data class that controls all behavioral differences
 * in the unified quiz play system. UI reads these flags to conditionally
 * render feedback, explanations, nudges, and action buttons.
 *
 * Use the companion factory methods to create pre-configured instances:
 * - [QuizConfig.module] for module quiz / daily mission
 * - [QuizConfig.iqTest] for IQ assessment
 * - [QuizConfig.tournament] for live tournament
 *
 * Supabase mapping: quiz_type column maps to [QuizBehavior].
 */
data class QuizConfig(
    val behavior: QuizBehavior,

    // ── Feedback ─────────────────────────────────────────────────
    /** Show correct/incorrect highlighting after confirming answer */
    val showAnswerFeedback: Boolean,
    /** Show the explanation card after confirming answer */
    val showExplanation: Boolean,

    // ── Submission ───────────────────────────────────────────────
    /** How answers are submitted to the backend */
    val submissionMode: SubmissionMode,

    // ── Timer ────────────────────────────────────────────────────
    /** Timer scope (quiz-level vs per-question) */
    val timerScope: TimerScope,

    // ── Navigation & Flow ────────────────────────────────────────
    /** Auto-advance to next question after confirm (null = manual Next button) */
    val autoAdvanceDelayMs: Long?,
    /** Allow pause/resume during quiz */
    val allowPause: Boolean,

    // ── Feedback Flash ──────────────────────────────────────────
    /**
     * Brief correct/incorrect flash after confirming, then auto-advance.
     * Useful for IQ / Tournament modes that normally hide feedback —
     * this shows green/red highlighting for a short period before
     * automatically moving to the next question.
     * null = no flash (use [showAnswerFeedback] + manual Next instead).
     */
    val feedbackFlashDurationMs: Long? = null,

    // ── Nudge / Hint System ──────────────────────────────────────
    /** Enable local nudge/hint system for wrong answers */
    val enableNudges: Boolean,

    // ── UI Configuration ─────────────────────────────────────────
    /** Top bar label text (e.g., "Quiz", "IQ Test", "Tournament") */
    val topBarLabel: String,
    /** Accent color hex override (null = use module color from session) */
    val accentColorHex: String?,
    /** Show "Review Answers" button on result screen */
    val showReviewButton: Boolean,
    /** Show "Try Again" button on result screen */
    val showRetryButton: Boolean,

    // ── Tournament-Specific ──────────────────────────────────────
    /** Tournament entry ID for per-question submission (null for non-tournament) */
    val tournamentEntryId: String? = null,
    /** Tournament ID for context (null for non-tournament) */
    val tournamentId: String? = null,
) {
    companion object {
        /**
         * Module practice quiz or daily mission.
         * Shows full feedback, explanations, nudge hints.
         * Batch submission at end. No pause.
         */
        fun module(
            moduleColor: String = "",
            moduleEmoji: String = "",
            moduleTitle: String = "Quiz",
        ) = QuizConfig(
            behavior = QuizBehavior.MODULE,
            showAnswerFeedback = true,
            showExplanation = true,
            submissionMode = SubmissionMode.BATCH_AT_END,
            timerScope = TimerScope.QUIZ_LEVEL,
            autoAdvanceDelayMs = null,
            allowPause = false,
            enableNudges = true,
            topBarLabel = moduleTitle.ifEmpty { "Quiz" },
            accentColorHex = moduleColor.ifEmpty { null },
            showReviewButton = true,
            showRetryButton = true,
        )

        /**
         * IQ assessment mode.
         * No feedback at all — just records the answer and auto-advances.
         * No explanations, no nudges.
         * Batch submission at end. No pause. No retry.
         */
        fun iqTest() = QuizConfig(
            behavior = QuizBehavior.IQ_TEST,
            showAnswerFeedback = false,
            showExplanation = false,
            submissionMode = SubmissionMode.BATCH_AT_END,
            timerScope = TimerScope.QUIZ_LEVEL,
            autoAdvanceDelayMs = 300L,
            allowPause = false,
            enableNudges = false,
            topBarLabel = "IQ Test",
            accentColorHex = "10B981",
            showReviewButton = false,
            showRetryButton = false,
        )

        /**
         * Live tournament mode.
         * No feedback at all — just records the answer and auto-advances.
         * Per-question non-blocking submission to backend.
         * Pause is configurable. No review/retry.
         */
        fun tournament(
            tournamentId: String,
            entryId: String,
            allowPause: Boolean = false,
        ) = QuizConfig(
            behavior = QuizBehavior.TOURNAMENT,
            showAnswerFeedback = false,
            showExplanation = false,
            submissionMode = SubmissionMode.PER_QUESTION_NON_BLOCKING,
            timerScope = TimerScope.QUIZ_LEVEL,
            autoAdvanceDelayMs = 300L,
            allowPause = allowPause,
            enableNudges = false,
            topBarLabel = "Tournament",
            accentColorHex = "4F46E5",
            showReviewButton = false,
            showRetryButton = false,
            tournamentEntryId = entryId,
            tournamentId = tournamentId,
        )
    }
}

/**
 * Nudge hint shown after a wrong answer in MODULE mode.
 * Encourages the user and optionally provides elimination hints.
 */
data class NudgeState(
    /** Display message for the nudge */
    val message: String,
    /** Type of hint provided */
    val hintType: NudgeHintType,
)

/**
 * Categories of nudge hints, ordered by helpfulness.
 */
enum class NudgeHintType {
    /** Generic encouragement: "Keep trying!" */
    ENCOURAGEMENT,
    /** Eliminates one wrong option: "Hint: It's not option X" */
    ELIMINATION_HINT,
    /** Points to the topic area: "Think about [topic]" */
    CATEGORY_HINT,
}
