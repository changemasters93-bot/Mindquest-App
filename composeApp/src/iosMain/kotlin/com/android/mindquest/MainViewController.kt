package com.android.mindquest

import androidx.compose.ui.window.ComposeUIViewController
import com.android.mindquest.di.appModule
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController(
    configure = {
        startKoin {
            modules(appModule)
        }
    },
) {
    App()
}
