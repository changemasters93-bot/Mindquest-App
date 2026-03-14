package com.android.mindquest.domain.usecase

import com.android.mindquest.core.prefs.SessionPrefs
import com.android.mindquest.core.util.AppLogger
import com.android.mindquest.core.util.Resource
import com.android.mindquest.domain.model.ChapterState
import com.android.mindquest.domain.model.DailyChallenge
import com.android.mindquest.domain.model.Module
import com.android.mindquest.domain.repository.ChapterRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Generates up to 3 daily challenges client-side.
 *
 * **Cache-first:** Challenges are generated once per day and cached locally.
 * Subsequent calls on the same day return the cached version. On a new day,
 * fresh challenges are fetched from module/chapter data and cached.
 *
 * Completion is tracked locally via [markDone] — no API re-fetch needed.
 */
class GenerateDailyChallengesUseCase(
    private val chapterRepository: ChapterRepository,
    private val sessionPrefs: SessionPrefs,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Return today's daily challenges. Uses cache if available, otherwise
     * generates from [modules] and caches the result.
     */
    suspend operator fun invoke(
        modules: List<Module>,
        userId: String,
    ): Resource<List<DailyChallenge>> {
        val today = todayDateString()

        // ── Cache hit: return stored challenges ───────────────────────
        if (sessionPrefs.dailyChallengeDate == today) {
            val cached = loadFromCache()
            if (cached != null) {
                AppLogger.d("MQ_AUTH", "DailyChallenges: cache HIT for $today (${cached.size} challenges)")
                return Resource.Success(cached)
            }
        }

        // ── Cache miss: generate fresh challenges ─────────────────────
        AppLogger.d("MQ_AUTH", "DailyChallenges: cache MISS — generating for $today")
        return try {
            val challenges = generateFromModules(modules, userId)
            saveToCache(today, challenges)
            AppLogger.d("MQ_AUTH", "DailyChallenges: generated & cached ${challenges.size} challenges")
            Resource.Success(challenges)
        } catch (e: Exception) {
            AppLogger.e("MQ_AUTH", "DailyChallenges: EXCEPTION", e)
            Resource.Error(message = "Failed to generate daily challenges", throwable = e)
        }
    }

    /**
     * Mark a specific challenge as done (locally). Called after quiz completion
     * from any entry point (daily mission or chapter listing).
     */
    fun markDone(quizId: String): List<DailyChallenge>? {
        val cached = loadFromCache() ?: return null
        val updated = cached.map { c ->
            if (c.quizId == quizId) c.copy(isDone = true) else c
        }
        saveToCache(sessionPrefs.dailyChallengeDate, updated)
        AppLogger.d("MQ_AUTH", "DailyChallenges: marked $quizId as done locally")
        return updated
    }

    // ── Internal helpers ──────────────────────────────────────────────

    private suspend fun generateFromModules(
        modules: List<Module>,
        userId: String,
    ): List<DailyChallenge> {
        val challenges = mutableListOf<DailyChallenge>()
        val candidates = modules.take(3)
        AppLogger.d("MQ_AUTH", "DailyChallenges: ${candidates.size} candidate modules")

        for (module in candidates) {
            try {
                val chapterId = resolveChapterId(module, userId) ?: continue
                AppLogger.d("MQ_AUTH", "DailyChallenges: ${module.title} → chapter $chapterId")

                val quizzesResult = chapterRepository.getChapterQuizzes(chapterId, userId)
                if (quizzesResult !is Resource.Success) continue

                val quizzes = quizzesResult.data
                // Priority: first unlocked & unattempted quiz
                val targetQuiz = quizzes
                    .sortedBy { it.displayOrder }
                    .firstOrNull { !it.isLocked && it.bestScore == null }
                // Fallback: first completed quiz (retry)
                    ?: quizzes.sortedBy { it.displayOrder }
                        .firstOrNull { it.bestScore != null }

                if (targetQuiz != null) {
                    challenges.add(
                        DailyChallenge(
                            quizId = targetQuiz.id,
                            title = targetQuiz.title,
                            description = "${module.emoji} ${module.title}",
                            questionCount = targetQuiz.questionCount,
                            timeInMinutes = (targetQuiz.timeLimitSeconds / 60).coerceAtLeast(1),
                            moduleId = module.id,
                            chapterId = chapterId,
                            isDone = targetQuiz.bestScore != null,
                            moduleColor = module.accentColor.removePrefix("#"),
                            moduleEmoji = module.emoji,
                        ),
                    )
                }
            } catch (e: Exception) {
                AppLogger.e("MQ_AUTH", "DailyChallenges: error for ${module.title}", e)
            }
        }
        return challenges
    }

    private suspend fun resolveChapterId(module: Module, userId: String): String? {
        module.progress?.currentChapterId?.let { return it }
        val result = chapterRepository.getModuleFull(module.id, userId)
        if (result !is Resource.Success) return null
        val (_, chapters) = result.data
        return chapters.firstOrNull { it.state == ChapterState.UNLOCKED }?.id
            ?: chapters.firstOrNull()?.id
    }

    // ── Cache serialization ──────────────────────────────────────────

    @Serializable
    private data class CachedChallenge(
        val quizId: String,
        val title: String,
        val description: String? = null,
        val questionCount: Int,
        val timeInMinutes: Int,
        val moduleId: String,
        val chapterId: String,
        val isDone: Boolean = false,
        val moduleColor: String = "",
        val moduleEmoji: String = "",
    )

    private fun loadFromCache(): List<DailyChallenge>? {
        val raw = sessionPrefs.dailyChallengesJson
        if (raw.isBlank()) return null
        return try {
            json.decodeFromString<List<CachedChallenge>>(raw).map { it.toDomain() }
        } catch (e: Exception) {
            AppLogger.e("MQ_AUTH", "DailyChallenges: cache parse error", e)
            null
        }
    }

    private fun saveToCache(date: String, challenges: List<DailyChallenge>) {
        sessionPrefs.dailyChallengeDate = date
        sessionPrefs.dailyChallengesJson = json.encodeToString(challenges.map { it.toCached() })
    }

    private fun DailyChallenge.toCached() = CachedChallenge(
        quizId = quizId, title = title, description = description,
        questionCount = questionCount, timeInMinutes = timeInMinutes,
        moduleId = moduleId, chapterId = chapterId, isDone = isDone,
        moduleColor = moduleColor, moduleEmoji = moduleEmoji,
    )

    private fun CachedChallenge.toDomain() = DailyChallenge(
        quizId = quizId, title = title, description = description,
        questionCount = questionCount, timeInMinutes = timeInMinutes,
        moduleId = moduleId, chapterId = chapterId, isDone = isDone,
        moduleColor = moduleColor, moduleEmoji = moduleEmoji,
    )

    private fun todayDateString(): String {
        val now = Clock.System.now()
        val local = now.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${local.year}-${local.monthNumber.toString().padStart(2, '0')}-${local.dayOfMonth.toString().padStart(2, '0')}"
    }
}
