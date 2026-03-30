package com.android.mindquest.core.update

import android.content.Intent
import android.net.Uri
import com.android.mindquest.BuildConfig
import org.koin.mp.KoinPlatformTools

actual fun getAppVersionCode(): Int = BuildConfig.VERSION_CODE

actual fun openStoreForUpdate(storeUrl: String) {
    val context = KoinPlatformTools.defaultContext().get().get<android.content.Context>()
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(storeUrl)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
