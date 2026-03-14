package com.android.mindquest

import android.app.Application
import com.android.mindquest.core.network.NetworkMonitor
import com.android.mindquest.di.appModule
import com.android.mindquest.di.platformModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MindquestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MindquestApplication)
            modules(platformModule(), appModule)
        }
        // Initialize platform-specific network monitor
        val networkMonitor = NetworkMonitor()
        networkMonitor.initialize(this)
    }
}
