package com.android.mindquest.core.analytics

/**
 * Delegates analytics calls to multiple [AnalyticsTracker] implementations.
 *
 * Enables the Open/Closed Principle: adding a new provider (Mixpanel,
 * Amplitude, etc.) requires only appending to the [trackers] list —
 * no existing code changes.
 */
class CompositeAnalyticsTracker(
    private val trackers: List<AnalyticsTracker>,
) : AnalyticsTracker {

    override fun logEvent(event: AnalyticsEvent) {
        trackers.forEach { it.logEvent(event) }
    }

    override fun logScreenView(screenName: String, screenClass: String?) {
        trackers.forEach { it.logScreenView(screenName, screenClass) }
    }

    override fun setUserId(userId: String?) {
        trackers.forEach { it.setUserId(userId) }
    }

    override fun setUserProperty(name: String, value: String?) {
        trackers.forEach { it.setUserProperty(name, value) }
    }

    override fun setEnabled(enabled: Boolean) {
        trackers.forEach { it.setEnabled(enabled) }
    }
}
