package com.android.mindquest.core.update

/**
 * Describes how urgently the user needs to update.
 *
 * - [UP_TO_DATE] — app version meets or exceeds the latest published version.
 * - [SOFT_UPDATE] — a newer version exists; user may dismiss and continue.
 * - [HARD_UPDATE] — app version is below the minimum; user must update to proceed.
 */
enum class UpdateStatus { UP_TO_DATE, SOFT_UPDATE, HARD_UPDATE }

/**
 * Version thresholds fetched from Firebase Remote Config.
 *
 * @param minVersionCode  Minimum supported version code. Versions below this trigger a hard (forced) update.
 * @param latestVersionCode  Latest available version code. Versions below this (but >= [minVersionCode]) trigger a soft update.
 * @param updateMessage  User-facing message shown in the update dialog.
 * @param storeUrl  Platform-specific store URL to open when "Update" is tapped.
 */
data class AppUpdateConfig(
    val minVersionCode: Int,
    val latestVersionCode: Int,
    val updateMessage: String,
    val storeUrl: String,
)

/**
 * Evaluates the update status for the running app.
 */
fun AppUpdateConfig.evaluate(currentVersionCode: Int): UpdateStatus = when {
    currentVersionCode < minVersionCode -> UpdateStatus.HARD_UPDATE
    currentVersionCode < latestVersionCode -> UpdateStatus.SOFT_UPDATE
    else -> UpdateStatus.UP_TO_DATE
}
