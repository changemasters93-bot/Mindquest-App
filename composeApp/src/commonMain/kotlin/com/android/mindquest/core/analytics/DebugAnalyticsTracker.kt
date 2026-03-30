package com.android.mindquest.core.analytics

import com.android.mindquest.core.util.AppLogger

/**
 * Logs all analytics events to the debug console via [AppLogger].
 *
 * [AppLogger] is gated by [isDebugBuild], so this tracker is
 * automatically a no-op in release builds — no conditional logic needed.
 */
class DebugAnalyticsTracker : AnalyticsTracker {

    override fun logEvent(event: AnalyticsEvent) {
        AppLogger.d(TAG, "EVENT: ${event.eventName} | ${event.params}")
    }

    override fun logScreenView(screenName: String, screenClass: String?) {
        AppLogger.d(TAG, "SCREEN: $screenName${screenClass?.let { " ($it)" } ?: ""}")
    }

    override fun setUserId(userId: String?) {
        AppLogger.d(TAG, "USER_ID: $userId")
    }

    override fun setUserProperty(name: String, value: String?) {
        AppLogger.d(TAG, "PROPERTY: $name=$value")
    }

    override fun setEnabled(enabled: Boolean) {
        AppLogger.d(TAG, "ENABLED: $enabled")
    }

    companion object {
        private const val TAG = "MQ_ANALYTICS"
    }
}
