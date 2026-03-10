package com.android.mindquest.domain.model

data class Module(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val emoji: String,
    val accentColor: String,
    val displayOrder: Int,
    val progress: ModuleProgress? = null
)

data class ModuleProgress(
    val currentChapterId: String? = null,
    val currentQuizId: String? = null,
    val bestScorePct: Int? = null,
    val isCompleted: Boolean = false
)
