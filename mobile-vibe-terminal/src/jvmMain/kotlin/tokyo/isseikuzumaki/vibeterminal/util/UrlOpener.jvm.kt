package tokyo.isseikuzumaki.vibeterminal.util

import java.awt.Desktop
import java.net.URI

actual object UrlOpener {
    actual fun openUrl(url: String) {
        Desktop.getDesktop().browse(URI(url))
    }
}
