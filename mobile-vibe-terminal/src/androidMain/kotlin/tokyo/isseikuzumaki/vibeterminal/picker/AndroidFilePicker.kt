package tokyo.isseikuzumaki.vibeterminal.picker

import kotlinx.coroutines.CompletableDeferred
import tokyo.isseikuzumaki.vibeterminal.domain.picker.FilePicker
import java.io.File

/**
 * Android implementation of FilePicker.
 * Communicates with MainActivity to use ActivityResultLauncher.
 */
class AndroidFilePicker : FilePicker {
    private var deferred: CompletableDeferred<File?>? = null
    private var onTrigger: (() -> Unit)? = null

    /**
     * Sets the callback to trigger the system picker.
     */
    fun setOnTrigger(onTrigger: () -> Unit) {
        this.onTrigger = onTrigger
    }

    /**
     * Called by MainActivity when a file is selected or the picker is dismissed.
     */
    fun onFilePicked(file: File?) {
        deferred?.complete(file)
        deferred = null
    }

    override suspend fun pickFile(): File? {
        deferred = CompletableDeferred<File?>()
        onTrigger?.invoke()
        return deferred?.await()
    }
}
