package com.android.mindquest.core.update

/**
 * Fetches app-update configuration from a remote source.
 *
 * Each platform provides its own implementation (e.g. Firebase Remote Config
 * on Android, Swift-bridged Remote Config on iOS).
 *
 * Returns `null` when the fetch fails (network error, timeout, etc.) so
 * the caller can gracefully skip the update check.
 */
interface AppUpdateChecker {
    suspend fun fetchUpdateConfig(): AppUpdateConfig?
}
