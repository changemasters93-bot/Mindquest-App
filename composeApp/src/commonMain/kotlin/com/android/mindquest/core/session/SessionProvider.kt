package com.android.mindquest.core.session

import com.android.mindquest.core.util.AppLogger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth

/**
 * Provides the current Supabase user ID from the active session.
 * Injected into ViewModels that need to pass userId to API calls.
 */
class SessionProvider(private val supabaseClient: SupabaseClient) {
    val userId: String
        get() {
            val id = supabaseClient.auth.currentSessionOrNull()?.user?.id ?: ""
            AppLogger.d("MQ_AUTH", "SessionProvider.userId → '${if (id.isNotEmpty()) id.take(8) + "..." else "EMPTY"}'")
            return id
        }
}
