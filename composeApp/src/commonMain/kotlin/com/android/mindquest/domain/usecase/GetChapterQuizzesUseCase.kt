package com.android.mindquest.domain.usecase

import com.android.mindquest.core.util.Resource
import com.android.mindquest.domain.model.Quiz
import com.android.mindquest.domain.repository.ChapterRepository

class GetChapterQuizzesUseCase(
    private val repository: ChapterRepository
) {
    suspend operator fun invoke(chapterId: String, userId: String): Resource<List<Quiz>> {
        return repository.getChapterQuizzes(chapterId, userId)
    }
}
