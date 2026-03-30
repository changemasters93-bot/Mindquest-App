package com.android.mindquest.core.config

import com.android.mindquest.R
import com.android.mindquest.core.util.AppLogger
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.tasks.await

/**
 * Centralized Firebase Remote Config provider for Android.
 *
 * Owns initialization, default loading, and fetch lifecycle.
 * All remote-config consumers read values through this provider,
 * making it the single place to add new keys in the future.
 *
 * Usage:
 * ```
 * val provider = RemoteConfigProvider()
 * provider.init()           // loads defaults + fetches
 * provider.getString("key") // read any key
 * ```
 */
class RemoteConfigProvider {

    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    private var initialized = false

    /**
     * Initialize Remote Config: apply settings, load XML defaults, fetch & activate.
     * Safe to call multiple times — subsequent calls are no-ops.
     */
    suspend fun init() {
        if (initialized) return

        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(FETCH_INTERVAL_SECONDS)
            .build()
        remoteConfig.setConfigSettingsAsync(settings).await()
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults).await()

        try {
            remoteConfig.fetchAndActivate().await()
            AppLogger.d(TAG, "Remote Config fetched & activated")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Remote Config fetch failed, using defaults", e)
            // Defaults from XML are still available — callers won't break.
        }

        initialized = true
    }

    /** Read a string value (returns default if key is missing or not yet fetched). */
    fun getString(key: String): String = remoteConfig.getString(key)

    /** Read a long value. */
    fun getLong(key: String): Long = remoteConfig.getLong(key)

    /** Read a boolean value. */
    fun getBoolean(key: String): Boolean = remoteConfig.getBoolean(key)

    companion object {
        private const val TAG = "MQ_REMOTE_CONFIG"
        private const val FETCH_INTERVAL_SECONDS = 3600L // 1 hour
    }
}
