package com.android.mindquest.core.prefs

import com.russhwolf.settings.Settings

/**
 * Lightweight key-value store for session flags.
 *
 * Uses `multiplatform-settings` (SharedPreferences on Android,
 * NSUserDefaults on iOS) so values survive process death.
 *
 * Currently stores only the "has the user completed login?" flag.
 * Expand with additional keys as app settings grow.
 */
class SessionPrefs(private val settings: Settings = Settings()) {

    /** `true` once the user completes any auth flow (Google, Phone, Anonymous). */
    var isLoggedIn: Boolean
        get() = settings.getBoolean(KEY_LOGGED_IN, false)
        set(value) { settings.putBoolean(KEY_LOGGED_IN, value) }

    // ── Daily Challenge Cache ─────────────────────────────────────────

    /** Date string (yyyy-MM-dd) when daily challenges were last generated. */
    var dailyChallengeDate: String
        get() = settings.getString(KEY_DAILY_CHALLENGE_DATE, "")
        set(value) { settings.putString(KEY_DAILY_CHALLENGE_DATE, value) }

    /** JSON-serialized daily challenges for today. */
    var dailyChallengesJson: String
        get() = settings.getString(KEY_DAILY_CHALLENGES_JSON, "")
        set(value) { settings.putString(KEY_DAILY_CHALLENGES_JSON, value) }

    // ── Generic key-value access ────────────────────────────────────

    /** Store an arbitrary string value by key. */
    fun putString(key: String, value: String) {
        settings.putString(key, value)
    }

    /** Retrieve a string value by key, or null if not present. */
    fun getString(key: String): String? {
        return settings.getStringOrNull(key)
    }

    /** Remove a single key from storage. */
    fun remove(key: String) {
        settings.remove(key)
    }

    /** Wipe all session flags (called on explicit sign-out). */
    fun clear() {
        settings.remove(KEY_LOGGED_IN)
        settings.remove(KEY_DAILY_CHALLENGE_DATE)
        settings.remove(KEY_DAILY_CHALLENGES_JSON)
    }

    private companion object {
        const val KEY_LOGGED_IN = "session_logged_in"
        const val KEY_DAILY_CHALLENGE_DATE = "daily_challenge_date"
        const val KEY_DAILY_CHALLENGES_JSON = "daily_challenges_json"
    }
}
