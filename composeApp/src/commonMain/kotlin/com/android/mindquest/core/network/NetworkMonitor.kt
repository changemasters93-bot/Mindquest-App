package com.android.mindquest.core.network

import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-agnostic connectivity monitor.
 *
 * Each target (Android / iOS) supplies an `actual` implementation that
 * observes the device's network state and pushes updates into [isOnline].
 *
 * Usage:
 * ```
 * val monitor: NetworkMonitor = …          // inject via DI
 * monitor.isOnline.collect { online ->
 *     if (!online) showOfflineBanner()
 * }
 * ```
 */
expect class NetworkMonitor {

    /**
     * Emits `true` when the device has a usable network connection,
     * `false` otherwise. The initial value should reflect the current
     * connectivity state at the time of creation.
     */
    val isOnline: StateFlow<Boolean>
}
