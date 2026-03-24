package com.android.mindquest.cache

import android.content.Context

// SQLDelight disabled: v2.2.1 klibs require Kotlin 2.2.x ABI.
// Re-enable with full AndroidSqliteDriver when Kotlin is upgraded.
actual class DatabaseDriverFactory(private val context: Context)
