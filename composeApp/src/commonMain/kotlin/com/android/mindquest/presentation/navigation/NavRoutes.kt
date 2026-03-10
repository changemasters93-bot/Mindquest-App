package com.android.mindquest.presentation.navigation

/**
 * Type-safe navigation route definitions for Mindquest.
 *
 * Each constant defines a route template. Routes that accept arguments use
 * `{argName}` placeholders which are replaced at navigation time via the
 * companion helper functions.
 */
object NavRoutes {

    // ── Auth flow ───────────────────────────────────────────────────────
    const val AUTH = "auth"
    const val LOGIN_JOURNEY = "login_journey"

    // ── Main tabs (shown in bottom nav) ─────────────────────────────────
    const val HOME = "home"
    const val LEADERBOARD = "leaderboard"
    const val STATS = "stats"
    const val PROFILE = "profile"

    // ── Chapters ────────────────────────────────────────────────────────
    const val CHAPTERS = "chapters/{moduleId}/{moduleTitle}/{moduleEmoji}/{moduleColor}"

    // ── Quiz flow ───────────────────────────────────────────────────────
    const val QUIZ_INTRO = "quiz_intro/{moduleColor}"
    const val QUIZ_PLAY = "quiz_play/{moduleColor}"
    const val QUIZ_RESULT = "quiz_result/{moduleColor}"
    const val QUIZ_REVIEW = "quiz_review"

    // ── Tournament flow ─────────────────────────────────────────────────
    const val TOURNAMENT_LOBBY = "tournament_lobby"
    const val TOURNAMENT_PLAY = "tournament_play"
    const val TOURNAMENT_PAUSE = "tournament_pause"
    const val TOURNAMENT_RESULT = "tournament_result"

    // ── Argument keys ───────────────────────────────────────────────────
    const val ARG_MODULE_ID = "moduleId"
    const val ARG_MODULE_TITLE = "moduleTitle"
    const val ARG_MODULE_EMOJI = "moduleEmoji"
    const val ARG_MODULE_COLOR = "moduleColor"

    // ── Bottom-nav routes set (used to decide when to show the bar) ────
    val BOTTOM_NAV_ROUTES = setOf(HOME, LEADERBOARD, STATS, PROFILE)

    // ── Helper functions to build routes with arguments ─────────────────

    fun chapters(
        moduleId: String,
        title: String,
        emoji: String,
        color: String,
    ): String = "chapters/$moduleId/$title/$emoji/$color"

    fun quizIntro(moduleColor: String): String = "quiz_intro/$moduleColor"

    fun quizPlay(moduleColor: String): String = "quiz_play/$moduleColor"

    fun quizResult(moduleColor: String): String = "quiz_result/$moduleColor"
}
