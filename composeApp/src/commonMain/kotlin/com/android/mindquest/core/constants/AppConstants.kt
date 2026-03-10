package com.android.mindquest.core.constants

/**
 * Application-wide constants for Mindquest.
 *
 * All magic numbers and feature flags live here so they can be tuned
 * from a single location.
 */
object AppConstants {

    // ── Feature flags ──────────────────────────────────────────────────

    /** When `true` the app uses local mock data instead of hitting Supabase. */
    const val USE_MOCK_DATA: Boolean = true

    // ── Supabase ───────────────────────────────────────────────────────

    /** Supabase project URL. Replace with real value before release. */
    const val SUPABASE_URL: String = ""

    /** Supabase anonymous / public API key. Replace with real value before release. */
    const val SUPABASE_ANON_KEY: String = ""

    // ── Leaderboard ────────────────────────────────────────────────────

    /** Number of entries fetched per leaderboard page. */
    const val LEADERBOARD_PAGE_SIZE: Int = 100

    // ── Quiz XP rules ──────────────────────────────────────────────────

    /** If a quiz is completed within this time (ms) the player earns a speed bonus. */
    const val QUIZ_SPEED_BONUS_THRESHOLD_MS: Long = 15_000L

    /** Bonus XP awarded for a 100 % score. */
    const val PERFECT_SCORE_BONUS_XP: Int = 50

    /** Base XP granted just for finishing a quiz. */
    const val PARTICIPATION_XP: Int = 5

    /** Extra XP for the first quiz completed each day. */
    const val DAILY_FIRST_BONUS_XP: Int = 10

    /** Multiplier applied to XP when replaying an already-completed quiz. */
    const val REPLAY_XP_MULTIPLIER: Double = 0.5

    /** XP awarded for participating in a tournament round. */
    const val TOURNAMENT_PARTICIPATION_XP: Int = 30

    // ── Avatar ─────────────────────────────────────────────────────────

    /** Highest avatar ID available in the asset catalog. */
    const val MAX_AVATAR_ID: Int = 8

    // ── Content versioning ─────────────────────────────────────────────

    /** Key used in local storage to track the downloaded content version. */
    const val CONTENT_VERSION_KEY: String = "content_version"

    // ── UI helpers ─────────────────────────────────────────────────────

    /** Default debounce delay for search fields and rapid-tap guards (ms). */
    const val DEBOUNCE_DELAY_MS: Long = 300L

    // ── Star rating thresholds (percentage) ────────────────────────────

    /** Minimum score % required for 1 star. */
    const val STAR_1_THRESHOLD: Int = 40

    /** Minimum score % required for 2 stars. */
    const val STAR_2_THRESHOLD: Int = 60

    /** Minimum score % required for 3 stars. */
    const val STAR_3_THRESHOLD: Int = 75

    /** Minimum score % required for 4 stars (maximum). */
    const val STAR_4_THRESHOLD: Int = 90

    // ── Levelling ──────────────────────────────────────────────────────

    /**
     * Cumulative XP required to reach each level.
     *
     * Index 0 = Level 1 (0 XP), index 1 = Level 2 (100 XP), etc.
     */
    val LEVEL_XP_THRESHOLDS: List<Int> = listOf(
        0,          // Level 1
        100,        // Level 2
        250,        // Level 3
        500,        // Level 4
        1_000,      // Level 5
        2_000,      // Level 6
        4_000,      // Level 7
        7_000,      // Level 8
        12_000,     // Level 9
        20_000,     // Level 10
    )
}
