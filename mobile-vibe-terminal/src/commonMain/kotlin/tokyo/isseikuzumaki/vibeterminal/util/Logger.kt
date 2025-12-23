package tokyo.isseikuzumaki.vibeterminal.util

/**
 * Platform-agnostic logger interface
 */
expect object Logger {
    fun d(message: String)
    fun d(tag: String, message: String)
    fun e(message: String)
    fun e(tag: String, message: String)
    fun e(throwable: Throwable, message: String)
    fun w(message: String)
    fun w(tag: String, message: String)
    fun i(message: String)
    fun i(tag: String, message: String)
}
