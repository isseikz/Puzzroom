package tokyo.isseikuzumaki.vibeterminal.util

import android.content.ClipData
import android.content.ClipboardManager as AndroidClipboardManager
import android.content.Context
import tokyo.isseikuzumaki.vibeterminal.VibeTerminalApp

/**
 * Android用クリップボードマネージャー実装
 */
actual object ClipboardManager {
    actual fun copyToClipboard(text: String) {
        val context = VibeTerminalApp.applicationContext
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as AndroidClipboardManager
        val clip = ClipData.newPlainText("Terminal Text", text)
        clipboard.setPrimaryClip(clip)
    }
}
