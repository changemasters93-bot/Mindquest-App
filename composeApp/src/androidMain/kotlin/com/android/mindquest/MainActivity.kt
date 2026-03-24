package com.android.mindquest

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import org.koin.mp.KoinPlatformTools

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App()
        }
        // Handle the deep link that launched the app (cold start)
        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle the deep link when the app is already running (warm start)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent) {
        val data = intent.data ?: return
        Log.d("MQ_AUTH", "MainActivity.handleDeepLink: uri=$data")
        try {
            val supabaseClient = KoinPlatformTools.defaultContext().get().get<SupabaseClient>()
            supabaseClient.handleDeeplinks(intent)
            Log.d("MQ_AUTH", "MainActivity.handleDeepLink: passed to Supabase successfully")
        } catch (e: Exception) {
            Log.e("MQ_AUTH", "MainActivity.handleDeepLink: FAILED", e)
        }
    }
}
