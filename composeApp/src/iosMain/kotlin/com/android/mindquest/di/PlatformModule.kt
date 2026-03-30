package com.android.mindquest.di

import com.android.mindquest.cache.DatabaseDriverFactory
import com.android.mindquest.core.update.AppUpdateChecker
import com.android.mindquest.core.update.IosAppUpdateChecker
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { DatabaseDriverFactory() }
    single<AppUpdateChecker> { IosAppUpdateChecker }
}
