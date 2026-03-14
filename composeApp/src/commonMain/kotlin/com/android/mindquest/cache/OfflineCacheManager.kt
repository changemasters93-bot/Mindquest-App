package com.android.mindquest.cache

import com.android.mindquest.core.util.AppLogger
import com.android.mindquest.domain.model.Chapter
import com.android.mindquest.domain.model.ChapterProgress
import com.android.mindquest.domain.model.ChapterState
import com.android.mindquest.domain.model.Module
import com.android.mindquest.domain.model.ModuleProgress
import com.android.mindquest.domain.model.Quiz
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class OfflineCacheManager(driverFactory: DatabaseDriverFactory) {

    private val database = MindquestDatabase(driverFactory.createDriver())
    private val queries = database.mindquestDatabaseQueries

    companion object {
        private const val TAG = "OfflineCacheManager"
        const val CACHE_TTL_MS = 30 * 60 * 1000L // 30 minutes
    }

    // ── Dashboard (JSON blob) ────────────────────────────────────────────

    suspend fun cacheDashboard(userId: String, jsonData: String) = withContext(Dispatchers.IO) {
        try {
            queries.insertDashboard(userId, jsonData, nowMs())
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to cache dashboard", e)
        }
    }

    suspend fun getCachedDashboard(userId: String): String? = withContext(Dispatchers.IO) {
        try {
            val row = queries.selectDashboard(userId).executeAsOneOrNull()
            if (row != null && !isExpired(row.cached_at)) row.json_data else null
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to read cached dashboard", e)
            null
        }
    }

    // ── Modules ──────────────────────────────────────────────────────────

    suspend fun cacheModules(modules: List<Module>) = withContext(Dispatchers.IO) {
        try {
            val now = nowMs()
            modules.forEach { m ->
                queries.insertModule(
                    id = m.id,
                    title = m.title,
                    subtitle = m.subtitle,
                    emoji = m.emoji,
                    accent_color = m.accentColor,
                    display_order = m.displayOrder.toLong(),
                    current_chapter_id = m.progress?.currentChapterId,
                    current_quiz_id = m.progress?.currentQuizId,
                    best_score_pct = m.progress?.bestScorePct?.toLong(),
                    is_completed = if (m.progress?.isCompleted == true) 1L else 0L,
                    completed_chapters = (m.progress?.completedChapters ?: 0).toLong(),
                    total_chapters = (m.progress?.totalChapters ?: 0).toLong(),
                    total_xp_earned = m.progress?.totalXpEarned ?: 0L,
                    cached_at = now
                )
            }
            AppLogger.d(TAG, "Cached ${modules.size} modules")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to cache modules", e)
        }
    }

    suspend fun getCachedModules(): List<Module>? = withContext(Dispatchers.IO) {
        try {
            val rows = queries.selectModules().executeAsList()
            if (rows.isEmpty()) return@withContext null
            if (isExpired(rows.first().cached_at)) return@withContext null

            rows.map { row ->
                Module(
                    id = row.id,
                    title = row.title,
                    subtitle = row.subtitle,
                    emoji = row.emoji,
                    accentColor = row.accent_color,
                    displayOrder = row.display_order.toInt(),
                    progress = ModuleProgress(
                        currentChapterId = row.current_chapter_id,
                        currentQuizId = row.current_quiz_id,
                        bestScorePct = row.best_score_pct?.toInt(),
                        isCompleted = row.is_completed == 1L,
                        completedChapters = row.completed_chapters.toInt(),
                        totalChapters = row.total_chapters.toInt(),
                        totalXpEarned = row.total_xp_earned,
                    )
                )
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to read cached modules", e)
            null
        }
    }

    // ── Chapters ─────────────────────────────────────────────────────────

    suspend fun cacheChapters(moduleId: String, chapters: List<Chapter>) = withContext(Dispatchers.IO) {
        try {
            val now = nowMs()
            chapters.forEach { c ->
                queries.insertChapter(
                    id = c.id,
                    module_id = moduleId,
                    title = c.title,
                    chapter_number = c.chapterNumber.toLong(),
                    quiz_count = c.quizCount.toLong(),
                    state = c.state.name,
                    quizzes_done = (c.progress?.quizzesDone ?: 0).toLong(),
                    total_quizzes = (c.progress?.totalQuizzes ?: 0).toLong(),
                    best_score_pct = c.progress?.bestScorePct?.toLong(),
                    is_completed = if (c.progress?.isCompleted == true) 1L else 0L,
                    cached_at = now
                )
            }
            AppLogger.d(TAG, "Cached ${chapters.size} chapters for module $moduleId")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to cache chapters", e)
        }
    }

    suspend fun getCachedChapters(moduleId: String): List<Chapter>? = withContext(Dispatchers.IO) {
        try {
            val rows = queries.selectChaptersByModule(moduleId).executeAsList()
            if (rows.isEmpty()) return@withContext null
            if (isExpired(rows.first().cached_at)) return@withContext null

            rows.map { row ->
                Chapter(
                    id = row.id,
                    title = row.title,
                    chapterNumber = row.chapter_number.toInt(),
                    quizCount = row.quiz_count.toInt(),
                    state = try {
                        ChapterState.valueOf(row.state)
                    } catch (_: Exception) {
                        ChapterState.LOCKED
                    },
                    progress = ChapterProgress(
                        quizzesDone = row.quizzes_done.toInt(),
                        totalQuizzes = row.total_quizzes.toInt(),
                        bestScorePct = row.best_score_pct?.toInt(),
                        isCompleted = row.is_completed == 1L
                    )
                )
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to read cached chapters", e)
            null
        }
    }

    // ── Quizzes ──────────────────────────────────────────────────────────

    suspend fun cacheQuizzes(chapterId: String, quizzes: List<Quiz>) = withContext(Dispatchers.IO) {
        try {
            val now = nowMs()
            quizzes.forEach { q ->
                queries.insertQuiz(
                    id = q.id,
                    chapter_id = chapterId,
                    title = q.title,
                    quiz_type = q.quizType,
                    question_count = q.questionCount.toLong(),
                    time_limit_seconds = q.timeLimitSeconds.toLong(),
                    max_xp = q.maxXp.toLong(),
                    difficulty = q.difficulty,
                    display_order = q.displayOrder.toLong(),
                    best_score = q.bestScore?.toLong(),
                    attempt_count = q.attemptCount.toLong(),
                    is_locked = if (q.isLocked) 1L else 0L,
                    cached_at = now
                )
            }
            AppLogger.d(TAG, "Cached ${quizzes.size} quizzes for chapter $chapterId")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to cache quizzes", e)
        }
    }

    suspend fun getCachedQuizzes(chapterId: String): List<Quiz>? = withContext(Dispatchers.IO) {
        try {
            val rows = queries.selectQuizzesByChapter(chapterId).executeAsList()
            if (rows.isEmpty()) return@withContext null
            if (isExpired(rows.first().cached_at)) return@withContext null

            rows.map { row ->
                Quiz(
                    id = row.id,
                    title = row.title,
                    quizType = row.quiz_type,
                    questionCount = row.question_count.toInt(),
                    timeLimitSeconds = row.time_limit_seconds.toInt(),
                    maxXp = row.max_xp.toInt(),
                    difficulty = row.difficulty,
                    displayOrder = row.display_order.toInt(),
                    bestScore = row.best_score?.toInt(),
                    attemptCount = row.attempt_count.toInt(),
                    isLocked = row.is_locked == 1L
                )
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to read cached quizzes", e)
            null
        }
    }

    // ── Stats (JSON blob) ────────────────────────────────────────────────

    suspend fun cacheStats(userId: String, period: String, jsonData: String) = withContext(Dispatchers.IO) {
        try {
            queries.insertStats(userId, period, jsonData, nowMs())
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to cache stats", e)
        }
    }

    suspend fun getCachedStats(userId: String, period: String): String? = withContext(Dispatchers.IO) {
        try {
            val row = queries.selectStats(userId, period).executeAsOneOrNull()
            if (row != null && !isExpired(row.cached_at)) row.json_data else null
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to read cached stats", e)
            null
        }
    }

    // ── Pending Operations Queue ─────────────────────────────────────────

    suspend fun enqueuePendingOperation(type: String, payload: String) = withContext(Dispatchers.IO) {
        try {
            queries.insertPendingOperation(type, payload, nowMs())
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to enqueue pending operation", e)
        }
    }

    suspend fun getPendingOperations() = withContext(Dispatchers.IO) {
        try {
            queries.selectPendingOperations().executeAsList()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to read pending operations", e)
            emptyList()
        }
    }

    suspend fun removePendingOperation(id: Long) = withContext(Dispatchers.IO) {
        try {
            queries.deletePendingOperation(id)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to remove pending operation $id", e)
        }
    }

    suspend fun incrementPendingRetryCount(id: Long) = withContext(Dispatchers.IO) {
        try {
            queries.updatePendingRetryCount(id)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to update retry count for $id", e)
        }
    }

    // ── Cache Maintenance ────────────────────────────────────────────────

    suspend fun clearExpiredCache() = withContext(Dispatchers.IO) {
        try {
            val cutoff = nowMs() - CACHE_TTL_MS
            queries.clearOldCache(cutoff)
            AppLogger.d(TAG, "Cleared expired cache entries")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to clear expired cache", e)
        }
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        try {
            queries.clearAllDashboard()
            queries.clearAllModules()
            queries.clearAllChapters()
            queries.clearAllQuizzes()
            queries.clearAllStats()
            queries.clearAllPendingOperations()
            AppLogger.d(TAG, "Cleared all cached data")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to clear all cache", e)
        }
    }

    // ── Utilities ────────────────────────────────────────────────────────

    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

    private fun isExpired(cachedAt: Long): Boolean {
        return (nowMs() - cachedAt) > CACHE_TTL_MS
    }
}
