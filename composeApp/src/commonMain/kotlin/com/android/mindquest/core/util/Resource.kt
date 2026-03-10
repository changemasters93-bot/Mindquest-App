package com.android.mindquest.core.util

/**
 * Generic result wrapper used by repositories.
 *
 * Unlike [UiState], [Resource] is framework-agnostic and can be
 * returned from any suspend function or Flow. ViewModels typically
 * map a `Resource` into the richer `UiState` before exposing it
 * to the UI layer.
 *
 * ```
 * when (val result = repository.fetchQuizzes()) {
 *     is Resource.Success -> emit(UiState.Success(result.data))
 *     is Resource.Error   -> emit(UiState.Error(result.message))
 *     is Resource.Loading -> emit(UiState.Loading)
 * }
 * ```
 */
sealed class Resource<out T> {

    /** Operation completed successfully with [data]. */
    data class Success<T>(val data: T) : Resource<T>()

    /** Operation failed. [message] is human-readable; [throwable] is optional for logging. */
    data class Error(
        val message: String,
        val throwable: Throwable? = null,
    ) : Resource<Nothing>()

    /**
     * Operation is in progress.
     *
     * [data] may hold stale / cached data that can be displayed
     * while fresh data is being fetched.
     */
    data class Loading<T>(val data: T? = null) : Resource<T>()
}
