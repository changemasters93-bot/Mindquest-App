package com.android.mindquest.core.analytics

import com.android.mindquest.presentation.navigation.NavRoutes

/**
 * Maps raw navigation routes (which may contain argument values like
 * `chapters/abc/Math/🧮/FF0000`) to clean, consistent screen names
 * for analytics (e.g. `"chapters"`).
 *
 * Returns `null` for routes that should not be tracked.
 */
object ScreenNameMapper {

    fun fromRoute(route: String?): String? = when {
        route == null -> null
        route == NavRoutes.LOGIN_JOURNEY -> "login_journey"
        route == NavRoutes.AUTH -> "auth"
        route == NavRoutes.HOME -> "home"
        route == NavRoutes.LEADERBOARD -> "leaderboard"
        route == NavRoutes.STATS -> "stats"
        route == NavRoutes.PROFILE -> "profile"
        route.startsWith("chapters/") -> "chapters"
        route.startsWith("quiz_intro/") -> "quiz_intro"
        route.startsWith("quiz_play/") -> "quiz_play"
        route.startsWith("quiz_result/") -> "quiz_result"
        route == NavRoutes.QUIZ_REVIEW -> "quiz_review"
        route == NavRoutes.TOURNAMENT_LOBBY -> "tournament_lobby"
        route == NavRoutes.TOURNAMENT_PLAY -> "tournament_play"
        route == NavRoutes.TOURNAMENT_PAUSE -> "tournament_pause"
        route == NavRoutes.TOURNAMENT_RESULT -> "tournament_result"
        else -> null
    }
}
