package com.android.mindquest.core.update

import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun getAppVersionCode(): Int {
    // CFBundleVersion is the build number (integer version code equivalent on iOS)
    val version = NSBundle.mainBundle.infoDictionary?.get("CFBundleVersion") as? String
    return version?.toIntOrNull() ?: 1
}

actual fun openStoreForUpdate(storeUrl: String) {
    val url = NSURL.URLWithString(storeUrl) ?: return
    UIApplication.sharedApplication.openURL(url)
}
