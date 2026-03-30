package com.android.mindquest.core.update

/** Returns the integer version code of the running app (e.g. `versionCode` on Android). */
expect fun getAppVersionCode(): Int

/** Opens the platform store listing so the user can update the app. */
expect fun openStoreForUpdate(storeUrl: String)
