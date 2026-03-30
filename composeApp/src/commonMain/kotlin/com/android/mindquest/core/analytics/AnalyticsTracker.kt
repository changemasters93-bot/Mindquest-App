package com.android.mindquest.core.analytics

/**
 * Platform-agnostic analytics abstraction.
 *
 * All analytics consumers depend on this interface (DIP), never on a
 * concrete SDK. Platform modules supply the actual implementation
 * (Firebase on Android, bridged Firebase on iOS).
 */
interface AnalyticsTracker {

    /** Log a strongly-typed analytics event. */
    fun logEvent(event: AnalyticsEvent)

    /** Log a screen view (auto-called by MindquestNavGraph on route changes). */
    fun logScreenView(screenName: String, screenClass: String? = null)

    /** Set the current user ID for all subsequent events. Pass `null` on sign-out. */
    fun setUserId(userId: String?)

    /** Set a custom user property (e.g. grade, country). */
    fun setUserProperty(name: String, value: String?)

    /** Enable or disable analytics collection (GDPR / consent). */
    fun setEnabled(enabled: Boolean)
}
