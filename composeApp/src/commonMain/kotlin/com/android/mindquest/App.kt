package com.android.mindquest

import androidx.compose.runtime.Composable
import com.android.mindquest.core.theme.MindquestTheme
import com.android.mindquest.presentation.navigation.MindquestNavGraph

/**
 * Application-level root composable.
 *
 * Wraps the entire UI in [MindquestTheme] and renders the navigation
 * graph. This function is called from the platform-specific entry points
 * (Android `MainActivity` and iOS `MainViewController`).
 */
@Composable
fun App() {
    MindquestTheme {
        MindquestNavGraph()
    }
}
