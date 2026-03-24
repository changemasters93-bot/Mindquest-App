package com.android.mindquest.core.util

/**
 * Maps raw Supabase / network exceptions to user-friendly messages.
 *
 * The raw error details are logged separately via [AppLogger].
 * This mapper ensures the UI never shows technical SQL errors,
 * Ktor stack traces, or Supabase internal messages.
 */
object ErrorMapper {

    fun toUserMessage(e: Exception): String {
        val msg = e.message?.lowercase() ?: return DEFAULT

        return when {
            // ── Network / connectivity ────────────────────────────────
            msg.containsAny("network", "unreachable", "no address", "connection refused", "unable to resolve", "no route") ->
                "Unable to connect. Please check your internet and try again."

            msg.containsAny("timeout", "timed out") ->
                "Request timed out. Please try again."

            msg.containsAny("ssl", "certificate", "handshake") ->
                "Secure connection failed. Please try again."

            // ── Auth / OTP ────────────────────────────────────────────
            msg.containsAny("invalid otp", "otp expired", "token expired", "invalid token") ->
                "The code you entered is invalid or expired. Please try again."

            msg.containsAny("otp already sent", "rate limit", "too many requests") ->
                "A verification code was already sent. Please wait before requesting again."

            msg.containsAny("no session", "session expired", "not authenticated", "jwt expired") ->
                "Your session has expired. Please sign in again."

            msg.containsAny("no user", "user not found") ->
                "Sign-in failed. Please try again."

            msg.containsAny("email already", "already registered") ->
                "This account already exists. Please sign in instead."

            msg.containsAny("identity already exists", "identity is already linked") ->
                "This provider is already linked to your account."

            msg.containsAny("user already has", "cannot link") ->
                "Unable to link this account. It may already be linked to another user."

            // ── Tournament ────────────────────────────────────────────
            msg.containsAny("already started or completed", "already participated") ->
                "You've already participated in this tournament."

            msg.containsAny("tournament is full") ->
                "This tournament is full. Try the next one!"

            // ── Quiz / cooldown ───────────────────────────────────────
            msg.containsAny("cooldown", "quiz is locked", "try again later") ->
                "This quiz is on cooldown. Try again later."

            // ── Database constraints ──────────────────────────────────
            // Email uniqueness violation
            msg.containsAny("users_email_unique", "duplicate key value violates unique constraint \"users_email_unique\"") ->
                "This email is already used by another account. Please sign in instead."

            // Phone uniqueness violation
            msg.containsAny("users_phone_unique", "duplicate key value violates unique constraint \"users_phone_unique\"") ->
                "This phone number is already linked to another account."

            // Generic duplicate/unique constraint
            msg.containsAny("duplicate key", "unique constraint", "already exists") ->
                "This action has already been completed. Please try again."

            msg.containsAny("violates", "constraint", "foreign key") ->
                "Something went wrong. Please try again."

            // ── Permission / RLS ──────────────────────────────────────
            msg.containsAny("permission denied", "rls", "policy", "insufficient privilege") ->
                "You don't have permission to perform this action."

            // ── Default ───────────────────────────────────────────────
            else -> DEFAULT
        }
    }

    private const val DEFAULT = "Something went wrong. Please try again."

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it) }
}
