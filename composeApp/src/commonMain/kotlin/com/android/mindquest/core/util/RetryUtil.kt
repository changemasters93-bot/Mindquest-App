package com.android.mindquest.core.util

import kotlinx.coroutines.delay

/**
 * Executes [block] with exponential backoff retry on failure.
 *
 * Retries up to [maxAttempts] times. The delay between retries doubles
 * each time, starting from [initialDelayMs] and capped at [maxDelayMs].
 *
 * Only retries on exceptions that are likely transient (network errors,
 * timeouts, server errors). Non-retryable exceptions are thrown immediately.
 *
 * Usage:
 * ```kotlin
 * val result = withRetry { apiService.getUserDashboard(userId) }
 * ```
 */
suspend fun <T> withRetry(
    maxAttempts: Int = 3,
    initialDelayMs: Long = 500L,
    maxDelayMs: Long = 5_000L,
    shouldRetry: (Exception) -> Boolean = ::isRetryable,
    block: suspend () -> T,
): T {
    var currentDelay = initialDelayMs
    repeat(maxAttempts - 1) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            if (!shouldRetry(e)) throw e
            AppLogger.w(
                "RetryUtil",
                "Attempt ${attempt + 1}/$maxAttempts failed: ${e.message}. Retrying in ${currentDelay}ms..."
            )
            delay(currentDelay)
            currentDelay = (currentDelay * 2).coerceAtMost(maxDelayMs)
        }
    }
    // Final attempt — let any exception propagate
    return block()
}

/**
 * Returns true if the exception is likely transient and worth retrying.
 */
private fun isRetryable(e: Exception): Boolean {
    val msg = e.message?.lowercase() ?: return true
    return when {
        // Network issues — always retry
        msg.containsAny("timeout", "timed out", "connection", "unreachable", "no address", "no route") -> true
        // Server errors (5xx) — retry
        msg.containsAny("500", "502", "503", "504", "internal server error", "service unavailable") -> true
        // Rate limiting — retry with backoff
        msg.containsAny("429", "too many requests", "rate limit") -> true
        // Auth/permission/constraint errors — do NOT retry
        msg.containsAny("permission", "rls", "jwt", "session", "duplicate", "unique", "constraint", "cooldown") -> false
        // Default: retry on unknown errors (conservative approach)
        else -> true
    }
}

private fun String.containsAny(vararg keywords: String): Boolean =
    keywords.any { this.contains(it) }
