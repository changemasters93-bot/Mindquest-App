package com.android.mindquest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.android.mindquest.core.theme.MindquestTheme
import com.android.mindquest.core.update.AppUpdateChecker
import com.android.mindquest.core.update.AppUpdateConfig
import com.android.mindquest.core.update.UpdateStatus
import com.android.mindquest.core.update.evaluate
import com.android.mindquest.core.update.getAppVersionCode
import com.android.mindquest.core.update.openStoreForUpdate
import com.android.mindquest.presentation.components.HardUpdateDialog
import com.android.mindquest.presentation.components.SoftUpdateDialog
import com.android.mindquest.presentation.navigation.MindquestNavGraph
import org.koin.compose.koinInject

/**
 * Application-level root composable.
 *
 * Wraps the entire UI in [MindquestTheme] and checks for app updates
 * on launch. Hard updates block the UI; soft updates show a dismissible
 * prompt once per session.
 */
@Composable
fun App() {
    MindquestTheme {
        val updateChecker = koinInject<AppUpdateChecker>()

        var updateStatus by remember { mutableStateOf(UpdateStatus.UP_TO_DATE) }
        var updateConfig by remember { mutableStateOf<AppUpdateConfig?>(null) }
        var softUpdateDismissed by remember { mutableStateOf(false) }

        // Check for updates once on launch
        LaunchedEffect(Unit) {
            val config = updateChecker.fetchUpdateConfig()
            if (config != null) {
                updateConfig = config
                updateStatus = config.evaluate(getAppVersionCode())
            }
        }

        // Always render the nav graph (hard update overlays on top)
        MindquestNavGraph()

        // Update dialogs
        when {
            updateStatus == UpdateStatus.HARD_UPDATE && updateConfig != null -> {
                HardUpdateDialog(
                    message = updateConfig!!.updateMessage,
                    onUpdate = { openStoreForUpdate(updateConfig!!.storeUrl) },
                )
            }
            updateStatus == UpdateStatus.SOFT_UPDATE && updateConfig != null && !softUpdateDismissed -> {
                SoftUpdateDialog(
                    message = updateConfig!!.updateMessage,
                    onUpdate = { openStoreForUpdate(updateConfig!!.storeUrl) },
                    onDismiss = { softUpdateDismissed = true },
                )
            }
        }
    }
}
