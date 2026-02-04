package tokyo.isseikuzumaki.vibeterminal.util

import android.content.Intent
import android.net.Uri
import tokyo.isseikuzumaki.vibeterminal.VibeTerminalApp

actual object UrlOpener {
    actual fun openUrl(url: String) {
        val context = VibeTerminalApp.applicationContext
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
