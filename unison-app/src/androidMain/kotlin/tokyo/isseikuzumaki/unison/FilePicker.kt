package tokyo.isseikuzumaki.unison

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Handles file picking for audio and text files
 */
class FilePicker(private val activity: ComponentActivity) {

    private var onFileSelected: ((Uri?) -> Unit)? = null

    private val pickFileLauncher = activity.registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onFileSelected?.invoke(uri)
        onFileSelected = null
    }

    fun pickAudioFile(onSelected: (Uri?) -> Unit) {
        onFileSelected = onSelected
        pickFileLauncher.launch("audio/*")
    }

    fun pickTextFile(onSelected: (Uri?) -> Unit) {
        onFileSelected = onSelected
        pickFileLauncher.launch("text/*")
    }
}
