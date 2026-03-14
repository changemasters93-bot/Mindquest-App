package com.android.mindquest.core.util

/**
 * Simple cross-platform logger.
 *
 * Uses `println` which outputs to:
 * - Android → Logcat (System.out)
 * - iOS → Xcode console
 *
 * All Supabase / network errors are logged here so they appear
 * in debug output while the user sees only friendly messages.
 */
object AppLogger {

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        println("ERROR [$tag]: $message")
        throwable?.let {
            println("  ↳ ${it::class.simpleName}: ${it.message}")
        }
    }

    fun w(tag: String, message: String) {
        println("WARN [$tag]: $message")
    }

    fun d(tag: String, message: String) {
        println("DEBUG [$tag]: $message")
    }
}
