package com.android.mindquest.domain.usecase

import com.android.mindquest.core.util.Resource
import com.android.mindquest.domain.model.Chapter
import com.android.mindquest.domain.model.Module
import com.android.mindquest.domain.repository.ChapterRepository

class GetModuleFullUseCase(
    private val repository: ChapterRepository
) {
    suspend operator fun invoke(moduleId: String, userId: String): Resource<Pair<Module, List<Chapter>>> {
        return repository.getModuleFull(moduleId, userId)
    }
}
