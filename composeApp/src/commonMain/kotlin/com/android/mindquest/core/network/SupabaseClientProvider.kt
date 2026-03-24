package com.android.mindquest.core.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import kotlinx.serialization.json.Json

/**
 * Singleton provider for the Supabase client.
 *
 * Call [createClient] once during app initialization (typically from
 * the DI graph via Koin `single {}`) and retain the returned
 * [SupabaseClient] for the lifetime of the process.
 *
 * Thread-safety: Koin's `single {}` guarantees the factory is invoked
 * at most once, so double-checked locking is unnecessary.
 * Previous approach using SynchronizedObject() inheritance caused
 * SIGABRT on Kotlin/Native (iOS) with certain atomicfu versions.
 */
object SupabaseClientProvider {

    private var instance: SupabaseClient? = null

    fun createClient(
        url: String,
        key: String,
        engine: HttpClientEngine? = null,
    ): SupabaseClient {
        // Fast path: return cached instance
        instance?.let { return it }
        // Build and cache (Koin single{} ensures this is called once)
        return buildClient(url, key, engine).also { instance = it }
    }

    @OptIn(SupabaseInternal::class)
    private fun buildClient(url: String, key: String, engine: HttpClientEngine?): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = key,
        ) {
            // ── Custom HTTP engine (e.g. OkHttp + Chucker on Android) ─
            if (engine != null) {
                httpEngine = engine
            }

            // ── Authentication ─────────────────────────────────────────
            // Configure the custom deep-link scheme so that OAuth
            // redirects land back in the app (mindquest://callback).
            install(Auth) {
                scheme = "mindquest"
                host = "callback"
            }

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
