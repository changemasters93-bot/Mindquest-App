package com.android.mindquest.core.util

/**
 * Cross-platform logger — **disabled in release builds**.
 *
 * Uses `println` which outputs to Logcat (Android) / Xcode console (iOS).
 * All calls are no-ops when [isDebugBuild] is false, preventing
 * accidental information leaks in production.
 */
object AppLogger {

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (!isDebugBuild) return
        println("ERROR [$tag]: $message")
        throwable?.let {
            println("  ↳ ${it::class.simpleName}: ${it.message}")
        }
    }

    fun w(tag: String, message: String) {
        if (!isDebugBuild) return
        println("WARN [$tag]: $message")
    }

    fun d(tag: String, message: String) {
        if (!isDebugBuild) return
        println("DEBUG [$tag]: $message")
    }
}
