package com.android.mindquest.core.update

import com.android.mindquest.core.config.RemoteConfigProvider
import com.android.mindquest.core.util.AppLogger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Android implementation of [AppUpdateChecker].
 *
 * Delegates all Firebase Remote Config access to [RemoteConfigProvider]
 * (single responsibility) and only owns JSON → [AppUpdateConfig] parsing.
 *
 * Remote Config key: `android_update_config` (JSON string).
 */
class FirebaseAppUpdateChecker(
    private val configProvider: RemoteConfigProvider,
) : AppUpdateChecker {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchUpdateConfig(): AppUpdateConfig? {
        return try {
            configProvider.init()
            val raw = configProvider.getString(CONFIG_KEY)
            parseConfig(raw).also {
                AppLogger.d(TAG, "Update config: min=${it.minVersionCode}, latest=${it.latestVersionCode}")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to read update config, skipping", e)
            null
        }
    }

    private fun parseConfig(raw: String): AppUpdateConfig {
        val obj = json.parseToJsonElement(raw).jsonObject
        return AppUpdateConfig(
            minVersionCode = obj["min_version_code"]!!.jsonPrimitive.int,
            latestVersionCode = obj["latest_version_code"]!!.jsonPrimitive.int,
            updateMessage = obj["update_message"]?.jsonPrimitive?.content
                ?: DEFAULT_MESSAGE,
            storeUrl = obj["store_url"]?.jsonPrimitive?.content
                ?: DEFAULT_STORE_URL,
        )
    }

    companion object {
        private const val TAG = "MQ_UPDATE"
        private const val CONFIG_KEY = "android_update_config"
        private const val DEFAULT_MESSAGE = "A new version of Mindquest is available!"
        private const val DEFAULT_STORE_URL = "https://play.google.com/store/apps/details?id=com.android.mindquest"
    }
}
