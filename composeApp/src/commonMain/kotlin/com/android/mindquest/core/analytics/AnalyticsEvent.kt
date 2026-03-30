package com.android.mindquest.core.analytics

/**
 * Strongly-typed analytics events.
 *
 * Each event defines its own [eventName] (snake_case, ≤40 chars) and
 * [params] map so callers never deal with raw strings. Adding a new event
 * is a single data-class addition — no other files need changes.
 *
 * Event names follow Firebase best practices and are consistent across
 * Android and iOS for cross-platform funnel analysis.
 */
sealed interface AnalyticsEvent {
    val eventName: String
    val params: Map<String, Any> get() = emptyMap()

    // ── Auth ─────────────────────────────────────────────────────────

    data class SignUp(val method: String) : AnalyticsEvent {
        override val eventName = "sign_up"
        override val params = mapOf("method" to method)
    }

    data class Login(val method: String) : AnalyticsEvent {
        override val eventName = "login"
        override val params = mapOf("method" to method)
    }

    data object Logout : AnalyticsEvent {
        override val eventName = "logout"
    }

    data class AccountLinked(val provider: String) : AnalyticsEvent {
        override val eventName = "account_linked"
        override val params = mapOf("provider" to provider)
    }

    // ── Onboarding ───────────────────────────────────────────────────

    data class OnboardingStarted(val step: String) : AnalyticsEvent {
        override val eventName = "onboarding_started"
        override val params = mapOf("step" to step)
    }

    data class OnboardingCompleted(val step: String) : AnalyticsEvent {
        override val eventName = "onboarding_completed"
        override val params = mapOf("step" to step)
    }

    // ── Quiz ─────────────────────────────────────────────────────────

    data class QuizStarted(
        val quizId: String,
        val moduleTitle: String,
        val quizType: String,
    ) : AnalyticsEvent {
        override val eventName = "quiz_started"
        override val params = mapOf(
            "quiz_id" to quizId,
            "module_title" to moduleTitle,
            "quiz_type" to quizType,
        )
    }

    data class QuizCompleted(
        val quizId: String,
        val score: Int,
        val totalQuestions: Int,
        val quizType: String,
        val durationSeconds: Long,
    ) : AnalyticsEvent {
        override val eventName = "quiz_completed"
        override val params = mapOf(
            "quiz_id" to quizId,
            "score" to score,
            "total_questions" to totalQuestions,
            "quiz_type" to quizType,
            "duration_seconds" to durationSeconds,
        )
    }

    data class QuestionAnswered(
        val quizId: String,
        val questionIndex: Int,
        val isCorrect: Boolean,
    ) : AnalyticsEvent {
        override val eventName = "question_answered"
        override val params = mapOf(
            "quiz_id" to quizId,
            "question_index" to questionIndex,
            "is_correct" to isCorrect,
        )
    }

    // ── Tournament ───────────────────────────────────────────────────

    data class TournamentJoined(val tournamentId: String) : AnalyticsEvent {
        override val eventName = "tournament_joined"
        override val params = mapOf("tournament_id" to tournamentId)
    }

    data class TournamentCompleted(
        val tournamentId: String,
        val score: Int,
    ) : AnalyticsEvent {
        override val eventName = "tournament_completed"
        override val params = mapOf(
            "tournament_id" to tournamentId,
            "score" to score,
        )
    }

    // ── Daily Challenge ──────────────────────────────────────────────

    data class DailyChallengeStarted(val challengeId: String) : AnalyticsEvent {
        override val eventName = "daily_challenge_started"
        override val params = mapOf("challenge_id" to challengeId)
    }

    data class DailyChallengeDone(val challengeId: String) : AnalyticsEvent {
        override val eventName = "daily_challenge_done"
        override val params = mapOf("challenge_id" to challengeId)
    }

    // ── Profile ──────────────────────────────────────────────────────

    data class ProfileUpdated(val fieldsChanged: List<String>) : AnalyticsEvent {
        override val eventName = "profile_updated"
        override val params = mapOf("fields_changed" to fieldsChanged.joinToString(","))
    }

    // ── Navigation / UI ──────────────────────────────────────────────

    data class ModuleOpened(
        val moduleId: String,
        val moduleTitle: String,
    ) : AnalyticsEvent {
        override val eventName = "module_opened"
        override val params = mapOf(
            "module_id" to moduleId,
            "module_title" to moduleTitle,
        )
    }

    data class ButtonClicked(
        val buttonName: String,
        val screenName: String,
    ) : AnalyticsEvent {
        override val eventName = "button_clicked"
        override val params = mapOf(
            "button_name" to buttonName,
            "screen_name" to screenName,
        )
    }

    // ── IQ Test ──────────────────────────────────────────────────────

    data class IqTestCompleted(val iqScore: Int) : AnalyticsEvent {
        override val eventName = "iq_test_completed"
        override val params = mapOf("iq_score" to iqScore)
    }
}
