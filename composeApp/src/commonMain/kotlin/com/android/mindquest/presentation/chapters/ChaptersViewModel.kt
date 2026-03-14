package com.android.mindquest.presentation.chapters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mindquest.core.constants.AppConstants
import com.android.mindquest.core.util.AppLogger
import com.android.mindquest.core.util.Resource
import com.android.mindquest.core.util.UiState
import com.android.mindquest.domain.model.Chapter
import com.android.mindquest.domain.model.ChapterState
import com.android.mindquest.domain.model.Module
import com.android.mindquest.domain.model.Quiz
import com.android.mindquest.domain.usecase.GetChapterQuizzesUseCase
import com.android.mindquest.domain.usecase.GetModuleFullUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

class ChaptersViewModel(
    private val getModuleFull: GetModuleFullUseCase,
    private val getChapterQuizzes: GetChapterQuizzesUseCase,
) : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        AppLogger.e("ChaptersViewModel", "Unhandled coroutine exception", throwable as? Exception)
    }

    private val _moduleState =
        MutableStateFlow<UiState<Pair<Module, List<Chapter>>>>(UiState.Loading)
    val moduleState: StateFlow<UiState<Pair<Module, List<Chapter>>>> = _moduleState.asStateFlow()

    private val _chapterQuizzesState =
        MutableStateFlow<UiState<List<Quiz>>>(UiState.Loading)
    val chapterQuizzesState: StateFlow<UiState<List<Quiz>>> = _chapterQuizzesState.asStateFlow()

    private val _expandedChapterId = MutableStateFlow<String?>(null)
    val expandedChapterId: StateFlow<String?> = _expandedChapterId.asStateFlow()

    private var lastModuleId: String = ""
    private var lastUserId: String = ""

    fun loadModule(moduleId: String, userId: String) {
        lastModuleId = moduleId
        lastUserId = userId
        viewModelScope.launch(exceptionHandler) {
            _moduleState.value = UiState.Loading
            when (val result = getModuleFull(moduleId, userId)) {
                is Resource.Success -> {
                    _moduleState.value = UiState.Success(result.data)
                    // Auto-expand the chapter the user should continue playing.
                    // A chapter is "effectively completed" when its state is
                    // COMPLETED **or** progress shows all quizzes done (handles
                    // the case where the backend hasn't been updated to return
                    // the 'completed' state yet).
                    val chapters = result.data.second
                    val nextIncomplete = chapters.firstOrNull { ch ->
                        val effectivelyCompleted =
                            ch.state == ChapterState.COMPLETED ||
                                (ch.progress != null &&
                                    ch.progress.totalQuizzes > 0 &&
                                    ch.progress.quizzesDone >= ch.progress.totalQuizzes)
                        ch.state != ChapterState.LOCKED && !effectivelyCompleted
                    } ?: chapters.lastOrNull { ch ->
                        // All done → show the most recent non-locked chapter
                        ch.state != ChapterState.LOCKED
                    } ?: chapters.firstOrNull()
                    nextIncomplete?.let { expandChapter(it.id, userId) }
                }
                is Resource.Error -> _moduleState.value = UiState.Error(result.message)
                is Resource.Loading -> { /* no-op */ }
            }
        }
    }

    /** Reload module data (called when returning from quiz). */
    fun refreshModule() {
        if (lastModuleId.isNotEmpty()) {
            loadModule(lastModuleId, lastUserId)
        }
    }

    fun expandChapter(chapterId: String, userId: String) {
        if (_expandedChapterId.value == chapterId) {
            _expandedChapterId.value = null
            return
        }
        _expandedChapterId.value = chapterId
        viewModelScope.launch(exceptionHandler) {
            _chapterQuizzesState.value = UiState.Loading
            when (val result = getChapterQuizzes(chapterId, userId)) {
                is Resource.Success -> {
                    val quizzes = result.data
                    val processed = if (AppConstants.ENABLE_SEQUENTIAL_QUIZ_UNLOCK) {
                        applySequentialLock(quizzes)
                    } else quizzes
                    _chapterQuizzesState.value = UiState.Success(processed)
                }
                is Resource.Error -> _chapterQuizzesState.value = UiState.Error(result.message)
                is Resource.Loading -> { /* no-op */ }
            }
        }
    }

    /**
     * Applies sequential quiz locking: all completed quizzes stay unlocked,
     * the first incomplete quiz is unlocked, and all subsequent incomplete
     * quizzes are locked.
     */
    private fun applySequentialLock(quizzes: List<Quiz>): List<Quiz> {
        val sorted = quizzes.sortedBy { it.displayOrder }
        var foundFirstIncomplete = false
        return sorted.map { quiz ->
            if (quiz.bestScore != null) {
                // Already completed — keep unlocked
                quiz
            } else if (!foundFirstIncomplete) {
                // First incomplete quiz — unlock it
                foundFirstIncomplete = true
                quiz.copy(isLocked = false)
            } else {
                // Subsequent incomplete quizzes — lock them
                quiz.copy(isLocked = true)
            }
        }
    }
}
