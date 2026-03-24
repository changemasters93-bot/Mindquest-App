package com.android.mindquest.di

import com.android.mindquest.cache.DatabaseDriverFactory
import com.chuckerteam.chucker.api.ChuckerInterceptor
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { DatabaseDriverFactory(get()) }
    single<HttpClientEngine> {
        OkHttp.create {
            addInterceptor(
                ChuckerInterceptor.Builder(androidContext())
                    .maxContentLength(250_000L)
                    .alwaysReadResponseBody(true)
                    .build()
            )
        }
    }
}
