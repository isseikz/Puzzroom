package tokyo.isseikuzumaki.vibeterminal.util

actual object Logger {
    actual fun d(message: String) = println("[DEBUG] $message")
    actual fun d(tag: String, message: String) = println("[DEBUG][$tag] $message")
    actual fun e(message: String) = println("[ERROR] $message")
    actual fun e(tag: String, message: String) = println("[ERROR][$tag] $message")
    actual fun e(throwable: Throwable, message: String) {
        println("[ERROR] $message: ${throwable.message}")
    }
    actual fun w(message: String) = println("[WARN] $message")
    actual fun w(tag: String, message: String) = println("[WARN][$tag] $message")
    actual fun i(message: String) = println("[INFO] $message")
    actual fun i(tag: String, message: String) = println("[INFO][$tag] $message")
}
