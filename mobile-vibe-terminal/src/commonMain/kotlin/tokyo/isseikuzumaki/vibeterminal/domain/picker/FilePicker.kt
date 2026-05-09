package tokyo.isseikuzumaki.vibeterminal.domain.picker

import java.io.File

/**
 * Interface for picking a file from the local device.
 * Implemented by platform-specific modules.
 */
interface FilePicker {
    /**
     * Opens the system file picker and returns the selected file.
     * Returns null if the user cancels or no file is selected.
     */
    suspend fun pickFile(): File?
}
