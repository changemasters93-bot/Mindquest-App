package com.android.mindquest.core.analytics

/**
 * iOS implementation of [AnalyticsTracker].
 *
 * Firebase Analytics iOS SDK is Swift-only. This object receives calls
 * from shared Kotlin code and forwards them to a Swift-provided [Bridge].
 *
 * Setup: In `AppDelegate.swift`, call:
 * ```swift
 * IosAnalyticsTracker.shared.setBridge(bridge: AnalyticsBridge.shared)
 * ```
 *
 * Follows the same pattern as `IosAppUpdateChecker`.
 */
object IosAnalyticsTracker : AnalyticsTracker {

    /**
     * Bridge interface that Swift implements to forward calls
     * to the Firebase Analytics iOS SDK.
     */
    interface Bridge {
        fun logEvent(name: String, params: Map<String, Any>)
        fun logScreenView(screenName: String, screenClass: String?)
        fun setUserId(userId: String?)
        fun setUserProperty(name: String, value: String?)
        fun setEnabled(enabled: Boolean)
    }

    private var bridge: Bridge? = null

    /** Called from Swift after Firebase is configured. */
    fun setBridge(bridge: Bridge) {
        this.bridge = bridge
    }

    override fun logEvent(event: AnalyticsEvent) {
        bridge?.logEvent(event.eventName, event.params)
    }

    override fun logScreenView(screenName: String, screenClass: String?) {
        bridge?.logScreenView(screenName, screenClass)
    }

    override fun setUserId(userId: String?) {
        bridge?.setUserId(userId)
    }

    override fun setUserProperty(name: String, value: String?) {
        bridge?.setUserProperty(name, value)
    }

    override fun setEnabled(enabled: Boolean) {
        bridge?.setEnabled(enabled)
    }
}
