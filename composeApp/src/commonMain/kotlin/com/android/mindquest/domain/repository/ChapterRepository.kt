package com.android.mindquest.domain.repository

import com.android.mindquest.core.util.Resource
import com.android.mindquest.domain.model.Chapter
import com.android.mindquest.domain.model.Module
import com.android.mindquest.domain.model.Quiz

interface ChapterRepository {
    suspend fun getModuleFull(moduleId: String, userId: String): Resource<Pair<Module, List<Chapter>>>
    suspend fun getChapterQuizzes(chapterId: String, userId: String): Resource<List<Quiz>>
}
