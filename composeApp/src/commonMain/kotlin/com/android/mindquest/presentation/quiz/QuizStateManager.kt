package com.android.mindquest.presentation.quiz

import com.android.mindquest.core.prefs.SessionPrefs
import com.android.mindquest.core.util.AppLogger
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persists critical quiz state to survive Android process death.
 *
 * On process death, QuizSessionHolder (an in-memory object) loses all data.
 * This manager saves/restores the minimum state needed to resume or
 * re-submit a quiz:
 * - quizId, userId, config type
 * - answers collected so far
 * - current question index
 * - time remaining
 * - start timestamp
 *
 * Flow:
 * 1. [saveState] called after each answer and on pause
 * 2. On app restart, [restoreState] checks for pending quiz
 * 3. If found, user is prompted to resume or discard
 * 4. [clearState] called when quiz completes or is discarded
 */
class QuizStateManager(
    private val sessionPrefs: SessionPrefs,
) {
    companion object {
        private const val KEY_QUIZ_STATE = "pending_quiz_state"
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }

    fun saveState(state: PersistableQuizState) {
        try {
            val encoded = json.encodeToString(state)
            sessionPrefs.putString(KEY_QUIZ_STATE, encoded)
        } catch (e: Exception) {
            AppLogger.e("QuizStateManager", "Failed to save quiz state", e)
        }
    }

    fun restoreState(): PersistableQuizState? {
        return try {
            val encoded = sessionPrefs.getString(KEY_QUIZ_STATE) ?: return null
            json.decodeFromString<PersistableQuizState>(encoded)
        } catch (e: Exception) {
            AppLogger.e("QuizStateManager", "Failed to restore quiz state", e)
            clearState()
            null
        }
    }

    fun hasPendingState(): Boolean {
        return sessionPrefs.getString(KEY_QUIZ_STATE) != null
    }

    fun clearState() {
        sessionPrefs.remove(KEY_QUIZ_STATE)
    }
}

@Serializable
data class PersistableQuizState(
    val quizId: String,
    val userId: String,
    val behaviorType: String, // "MODULE", "IQ_TEST", "TOURNAMENT"
    val currentIndex: Int,
    val totalQuestions: Int,
    val timeRemainingSeconds: Int,
    val startTimeMs: Long,
    val answers: List<PersistableAnswer>,
    val tournamentEntryId: String? = null,
    val moduleColor: String = "",
    val moduleEmoji: String = "",
    val moduleTitle: String = "",
)

@Serializable
data class PersistableAnswer(
    val questionId: String,
    val selected: String,
    val isCorrect: Boolean,
    val timeMs: Long,
)
