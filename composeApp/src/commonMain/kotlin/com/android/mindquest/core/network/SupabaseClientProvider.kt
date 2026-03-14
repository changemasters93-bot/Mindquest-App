package com.android.mindquest.core.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.ktor.client.plugins.HttpTimeout
import kotlinx.atomicfu.locks.synchronized
import kotlinx.serialization.json.Json

/**
 * Singleton provider for the Supabase client.
 *
 * Call [createClient] once during app initialization (typically from
 * the DI graph) and retain the returned [SupabaseClient] for the
 * lifetime of the process.
 *
 * Thread-safe: uses @Volatile + synchronized for double-checked locking.
 */
object SupabaseClientProvider {

    @Volatile
    private var instance: SupabaseClient? = null

    fun createClient(url: String, key: String): SupabaseClient {
        return instance ?: synchronized(this) {
            instance ?: buildClient(url, key).also { instance = it }
        }
    }

    @OptIn(SupabaseInternal::class)
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

            // ── HTTP timeouts ──────────────────────────────────────────
            // Prevents indefinite hangs on slow/unresponsive connections.
            httpConfig {
                install(HttpTimeout) {
                    requestTimeoutMillis = 30_000   // 30s overall request
                    connectTimeoutMillis = 10_000   // 10s to establish connection
                    socketTimeoutMillis  = 30_000   // 30s socket inactivity
                }
            }

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
