package com.android.mindquest.core.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Android implementation of [AnalyticsTracker] backed by Firebase Analytics.
 *
 * Converts [AnalyticsEvent.params] (multiplatform `Map<String, Any>`) to
 * an Android [Bundle] before forwarding to the Firebase SDK.
 */
class FirebaseAnalyticsTracker(
    context: Context,
) : AnalyticsTracker {

    private val firebase = FirebaseAnalytics.getInstance(context)

    override fun logEvent(event: AnalyticsEvent) {
        firebase.logEvent(event.eventName, event.params.toBundle())
    }

    override fun logScreenView(screenName: String, screenClass: String?) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            screenClass?.let { putString(FirebaseAnalytics.Param.SCREEN_CLASS, it) }
        }
        firebase.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    override fun setUserId(userId: String?) {
        firebase.setUserId(userId)
    }

    override fun setUserProperty(name: String, value: String?) {
        firebase.setUserProperty(name, value)
    }

    override fun setEnabled(enabled: Boolean) {
        firebase.setAnalyticsCollectionEnabled(enabled)
    }

    private fun Map<String, Any>.toBundle(): Bundle = Bundle().apply {
        forEach { (key, value) ->
            when (value) {
                is String -> putString(key, value)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Double -> putDouble(key, value)
                is Boolean -> putBoolean(key, value)
                else -> putString(key, value.toString())
            }
        }
    }
}
