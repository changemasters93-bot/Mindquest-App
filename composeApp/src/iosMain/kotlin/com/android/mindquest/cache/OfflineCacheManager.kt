package com.android.mindquest.cache

import com.android.mindquest.domain.model.Chapter
import com.android.mindquest.domain.model.Module
import com.android.mindquest.domain.model.Quiz

/**
 * No-op iOS implementation: SQLDelight native driver excluded due to
 * Kotlin ABI mismatch (project uses 2.1.0, SQLDelight 2.2.1 klibs
 * require 2.2.x). Offline cache is not wired to the main app flow yet.
 */
actual class OfflineCacheManager actual constructor(driverFactory: DatabaseDriverFactory) {
    actual suspend fun cacheDashboard(userId: String, jsonData: String) {}
    actual suspend fun getCachedDashboard(userId: String): String? = null
    actual suspend fun cacheModules(modules: List<Module>) {}
    actual suspend fun getCachedModules(): List<Module>? = null
    actual suspend fun cacheChapters(moduleId: String, chapters: List<Chapter>) {}
    actual suspend fun getCachedChapters(moduleId: String): List<Chapter>? = null
    actual suspend fun cacheQuizzes(chapterId: String, quizzes: List<Quiz>) {}
    actual suspend fun getCachedQuizzes(chapterId: String): List<Quiz>? = null
    actual suspend fun cacheStats(userId: String, period: String, jsonData: String) {}
    actual suspend fun getCachedStats(userId: String, period: String): String? = null
    actual suspend fun clearAll() {}
}
