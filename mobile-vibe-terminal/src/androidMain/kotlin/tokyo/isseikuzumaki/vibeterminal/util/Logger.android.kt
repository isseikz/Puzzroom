package tokyo.isseikuzumaki.vibeterminal.util

import timber.log.Timber

/**
 * Android implementation of Logger using Timber
 */
actual object Logger {
    actual fun d(message: String) {
        Timber.d(message)
    }

    actual fun d(tag: String, message: String) {
        Timber.tag(tag).d(message)
    }

    actual fun e(message: String) {
        Timber.e(message)
    }

    actual fun e(tag: String, message: String) {
        Timber.tag(tag).e(message)
    }

    actual fun e(throwable: Throwable, message: String) {
        Timber.e(throwable, message)
    }

    actual fun w(message: String) {
        Timber.w(message)
    }

    actual fun w(tag: String, message: String) {
        Timber.tag(tag).w(message)
    }

    actual fun i(message: String) {
        Timber.i(message)
    }

    actual fun i(tag: String, message: String) {
        Timber.tag(tag).i(message)
    }
}
