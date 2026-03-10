package com.android.mindquest.presentation.chapters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.launch

class ChaptersViewModel(
    private val getModuleFull: GetModuleFullUseCase,
    private val getChapterQuizzes: GetChapterQuizzesUseCase,
) : ViewModel() {

    private val _moduleState =
        MutableStateFlow<UiState<Pair<Module, List<Chapter>>>>(UiState.Loading)
    val moduleState: StateFlow<UiState<Pair<Module, List<Chapter>>>> = _moduleState.asStateFlow()

    private val _chapterQuizzesState =
        MutableStateFlow<UiState<List<Quiz>>>(UiState.Loading)
    val chapterQuizzesState: StateFlow<UiState<List<Quiz>>> = _chapterQuizzesState.asStateFlow()

    private val _expandedChapterId = MutableStateFlow<String?>(null)
    val expandedChapterId: StateFlow<String?> = _expandedChapterId.asStateFlow()

    fun loadModule(moduleId: String, userId: String) {
        viewModelScope.launch {
            _moduleState.value = UiState.Loading
            when (val result = getModuleFull(moduleId, userId)) {
                is Resource.Success -> {
                    _moduleState.value = UiState.Success(result.data)
                    // Auto-expand first unlocked chapter
                    val firstUnlocked = result.data.second.firstOrNull {
                        it.state == ChapterState.UNLOCKED
                    } ?: result.data.second.firstOrNull()
                    firstUnlocked?.let { expandChapter(it.id, userId) }
                }
                is Resource.Error -> _moduleState.value = UiState.Error(result.message)
                is Resource.Loading -> { /* no-op */ }
            }
        }
    }

    fun expandChapter(chapterId: String, userId: String) {
        if (_expandedChapterId.value == chapterId) {
            _expandedChapterId.value = null
            return
        }
        _expandedChapterId.value = chapterId
        viewModelScope.launch {
            _chapterQuizzesState.value = UiState.Loading
            when (val result = getChapterQuizzes(chapterId, userId)) {
                is Resource.Success -> _chapterQuizzesState.value = UiState.Success(result.data)
                is Resource.Error -> _chapterQuizzesState.value = UiState.Error(result.message)
                is Resource.Loading -> { /* no-op */ }
            }
        }
    }
}
