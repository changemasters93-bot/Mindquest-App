package com.android.mindquest.data.repository

import com.android.mindquest.core.constants.AppConstants
import com.android.mindquest.core.util.AppLogger
import com.android.mindquest.core.util.ErrorMapper
import com.android.mindquest.core.util.Resource
import com.android.mindquest.core.util.withRetry
import com.android.mindquest.data.mapper.toDomain
import com.android.mindquest.data.mock.MockDataSource
import com.android.mindquest.data.remote.ApiService
import com.android.mindquest.domain.model.Chapter
import com.android.mindquest.domain.model.Module
import com.android.mindquest.domain.model.Quiz
import com.android.mindquest.domain.repository.ChapterRepository

class ChapterRepositoryImpl(
    private val apiService: ApiService
) : ChapterRepository {

    override suspend fun getModuleFull(
        moduleId: String,
        userId: String
    ): Resource<Pair<Module, List<Chapter>>> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Success(MockDataSource.mockModuleFull(moduleId))
            } else {
                val response = withRetry { apiService.getModuleFull(moduleId, userId) }
                val module = response.module.toDomain()
                val chapters = response.chapters.map { it.toDomain() }
                Resource.Success(module to chapters)
            }
        } catch (e: Exception) {
            AppLogger.e("ChapterRepo", "load module failed", e)
            Resource.Error(
                message = ErrorMapper.toUserMessage(e),
                throwable = e
            )
        }
    }

    override suspend fun getChapterQuizzes(
        chapterId: String,
        userId: String
    ): Resource<List<Quiz>> {
        return try {
            if (AppConstants.USE_MOCK_DATA) {
                Resource.Success(MockDataSource.mockChapterQuizzes(chapterId))
            } else {
                val response = withRetry { apiService.getChapterQuizzes(chapterId, userId) }
                Resource.Success(response.map { it.toDomain() })
            }
        } catch (e: Exception) {
            AppLogger.e("ChapterRepo", "load quizzes failed", e)
            Resource.Error(
                message = ErrorMapper.toUserMessage(e),
                throwable = e
            )
        }
    }
}
