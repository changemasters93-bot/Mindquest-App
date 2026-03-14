package com.android.mindquest.core.session

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth

/**
 * Provides the current Supabase user ID from the active session.
 * Injected into ViewModels that need to pass userId to API calls.
 */
class SessionProvider(private val supabaseClient: SupabaseClient) {
    val userId: String
        get() = supabaseClient.auth.currentSessionOrNull()?.user?.id ?: ""
}
