package com.android.mindquest.core.util

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

enum class SnackbarType { SUCCESS, ERROR, INFO }

data class SnackbarMessage(
    val text: String,
    val type: SnackbarType = SnackbarType.INFO,
)

/**
 * App-wide snackbar manager. ViewModels post messages here;
 * the root Scaffold consumes them.
 *
 * Uses a Channel (buffered) so messages are never lost even if
 * the UI is paused (e.g. app backgrounded during OAuth flow).
 * Each message is delivered exactly once.
 */
class SnackbarManager {

    private val _messages = Channel<SnackbarMessage>(capacity = Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun showSuccess(text: String) {
        _messages.trySend(SnackbarMessage(text, SnackbarType.SUCCESS))
    }

    fun showError(text: String) {
        _messages.trySend(SnackbarMessage(text, SnackbarType.ERROR))
    }

    fun showInfo(text: String) {
        _messages.trySend(SnackbarMessage(text, SnackbarType.INFO))
    }
}
