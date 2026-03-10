package com.android.mindquest.core.util

/**
 * Represents the state of a UI-bound data request.
 *
 * ViewModels expose a `StateFlow<UiState<T>>` so that screens can
 * render loading indicators, error banners, empty states and offline
 * notices without ad-hoc boolean juggling.
 *
 * ```
 * when (val state = uiState.collectAsState().value) {
 *     is UiState.Loading  -> LoadingSpinner()
 *     is UiState.Success  -> ContentList(state.data)
 *     is UiState.Error    -> ErrorBanner(state.message)
 *     is UiState.Empty    -> EmptyPlaceholder()
 *     is UiState.Offline  -> OfflineBanner()
 * }
 * ```
 */
sealed class UiState<out T> {

    /** A request is in flight; show a loading indicator. */
    data object Loading : UiState<Nothing>()

    /** The request completed successfully with [data]. */
    data class Success<T>(val data: T) : UiState<T>()

    /** The request failed. [message] is user-facing; [throwable] is for logging. */
    data class Error(
        val message: String,
        val throwable: Throwable? = null,
    ) : UiState<Nothing>()

    /** The request succeeded but returned no data. */
    data object Empty : UiState<Nothing>()

    /** The device is offline and no cached data is available. */
    data object Offline : UiState<Nothing>()
}
