package tokyo.isseikuzumaki.vibeterminal.util

/**
 * JVM implementation of Logger using println
 */
actual object Logger {
    actual fun d(message: String) {
        println("[DEBUG] $message")
    }

    actual fun d(tag: String, message: String) {
        println("[DEBUG][$tag] $message")
    }

    actual fun e(message: String) {
        System.err.println("[ERROR] $message")
    }

    actual fun e(tag: String, message: String) {
        System.err.println("[ERROR][$tag] $message")
    }

    actual fun e(throwable: Throwable, message: String) {
        System.err.println("[ERROR] $message")
        throwable.printStackTrace()
    }

    actual fun w(message: String) {
        println("[WARN] $message")
    }

    actual fun w(tag: String, message: String) {
        println("[WARN][$tag] $message")
    }

    actual fun i(message: String) {
        println("[INFO] $message")
    }

    actual fun i(tag: String, message: String) {
        println("[INFO][$tag] $message")
    }
}
