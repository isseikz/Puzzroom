package tokyo.isseikuzumaki.unison.screens.recorder

import androidx.compose.runtime.*
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryRecord
import platform.AVFAudio.setActive

/**
 * iOS implementation of permission state for record audio
 */
private class IosRecordAudioPermissionState : RecordAudioPermissionState {
    private var _isGranted by mutableStateOf(checkPermission())

    override val isGranted: Boolean
        get() = _isGranted

    override fun launchPermissionRequest() {
        AVAudioSession.sharedInstance().requestRecordPermission { granted ->
            _isGranted = granted
        }
    }

    private fun checkPermission(): Boolean {
        // On iOS, we need to request permission first
        // The permission status will be updated via the callback
        return try {
            AVAudioSession.sharedInstance().setCategory(
                AVAudioSessionCategoryRecord,
                error = null
            )
            AVAudioSession.sharedInstance().setActive(true, null)
            true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * iOS-specific implementation of permission state
 */
@Composable
actual fun rememberRecordAudioPermissionState(): RecordAudioPermissionState {
    return remember {
        IosRecordAudioPermissionState()
    }
}