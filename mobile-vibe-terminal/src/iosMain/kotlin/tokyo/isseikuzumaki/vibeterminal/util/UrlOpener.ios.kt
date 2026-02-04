package tokyo.isseikuzumaki.vibeterminal.util

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual object UrlOpener {
    actual fun openUrl(url: String) {
        val nsUrl = NSURL(string = url) ?: return
        UIApplication.sharedApplication.openURL(nsUrl)
    }
}
