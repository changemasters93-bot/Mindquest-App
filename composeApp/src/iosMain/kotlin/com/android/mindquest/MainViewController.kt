package com.android.mindquest

import androidx.compose.ui.window.ComposeUIViewController
import com.android.mindquest.di.appModule
import com.android.mindquest.di.platformModule
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController(
    configure = {
        startKoin {
            modules(platformModule(), appModule)
        }
    },
) {
    App()
}
