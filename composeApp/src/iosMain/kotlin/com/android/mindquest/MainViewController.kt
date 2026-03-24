package com.android.mindquest

import androidx.compose.ui.window.ComposeUIViewController
import com.android.mindquest.di.appModule
import com.android.mindquest.di.platformModule
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatformTools

fun MainViewController() = ComposeUIViewController(
    configure = {
        // Guard against double-init: on iOS the view controller may be
        // re-created (scene disconnect/reconnect, SwiftUI previews).
        if (KoinPlatformTools.defaultContext().getOrNull() == null) {
            startKoin {
                modules(platformModule(), appModule)
            }
        }
    },
) {
    App()
}
