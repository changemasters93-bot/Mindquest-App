package com.android.mindquest.cache

import com.android.mindquest.domain.model.Chapter
import com.android.mindquest.domain.model.Module
import com.android.mindquest.domain.model.Quiz

/**
 * Offline cache backed by SQLDelight on Android; no-op on iOS until
 * Kotlin is upgraded to 2.2.x for native ABI compatibility.
 */
expect class OfflineCacheManager(driverFactory: DatabaseDriverFactory) {
    suspend fun cacheDashboard(userId: String, jsonData: String)
    suspend fun getCachedDashboard(userId: String): String?
    suspend fun cacheModules(modules: List<Module>)
    suspend fun getCachedModules(): List<Module>?
    suspend fun cacheChapters(moduleId: String, chapters: List<Chapter>)
    suspend fun getCachedChapters(moduleId: String): List<Chapter>?
    suspend fun cacheQuizzes(chapterId: String, quizzes: List<Quiz>)
    suspend fun getCachedQuizzes(chapterId: String): List<Quiz>?
    suspend fun cacheStats(userId: String, period: String, jsonData: String)
    suspend fun getCachedStats(userId: String, period: String): String?
    suspend fun clearAll()
}
