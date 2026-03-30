package com.android.mindquest.core.update

/**
 * iOS implementation of [AppUpdateChecker].
 *
 * Firebase Remote Config is accessed from Swift (AppDelegate) because
 * the Firebase iOS SDK is a native Swift package. The fetched values
 * are pushed into this object via [setConfig] before the Compose UI
 * loads, so [fetchUpdateConfig] simply returns the cached result.
 */
object IosAppUpdateChecker : AppUpdateChecker {

    private var cachedConfig: AppUpdateConfig? = null

    /**
     * Called from Swift after Firebase Remote Config fetch completes.
     * Must be invoked before the Compose UI starts.
     */
    fun setConfig(
        minVersionCode: Int,
        latestVersionCode: Int,
        updateMessage: String,
        storeUrl: String,
    ) {
        cachedConfig = AppUpdateConfig(
            minVersionCode = minVersionCode,
            latestVersionCode = latestVersionCode,
            updateMessage = updateMessage,
            storeUrl = storeUrl,
        )
    }

    override suspend fun fetchUpdateConfig(): AppUpdateConfig? = cachedConfig
}
