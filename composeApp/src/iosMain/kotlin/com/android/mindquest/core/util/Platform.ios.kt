package com.android.mindquest.core.util

import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
actual val isDebugBuild: Boolean = Platform.isDebugBinary
