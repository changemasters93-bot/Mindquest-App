package com.android.mindquest.core.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import kotlinx.serialization.json.Json

/**
 * Singleton provider for the Supabase client.
 *
 * Call [createClient] once during app initialization (typically from
 * the DI graph) and retain the returned [SupabaseClient] for the
 * lifetime of the process.
 */
object SupabaseClientProvider {

    private var instance: SupabaseClient? = null

    fun createClient(url: String, key: String): SupabaseClient {
        return instance ?: buildClient(url, key).also { instance = it }
    }

    private fun buildClient(url: String, key: String): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = key,
        ) {
            // ── Authentication ─────────────────────────────────────────
            install(Auth)

            // ── Database (PostgREST) ───────────────────────────────────
            install(Postgrest)

            // ── Realtime subscriptions ─────────────────────────────────
            install(Realtime)

            // ── JSON configuration ─────────────────────────────────────
            defaultSerializer = io.github.jan.supabase.serializer.KotlinXSerializer(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                    prettyPrint = false
                    coerceInputValues = true
                }
            )
        }
    }
}
