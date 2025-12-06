package tokyo.isseikuzumaki.unison.screens.recorder

import androidx.compose.runtime.*
import tokyo.isseikuzumaki.unison.LocalPermissionHandler

/**
 * Android implementation of permission state for record audio
 */
private class AndroidRecordAudioPermissionState(
    private val permissionHandler: tokyo.isseikuzumaki.unison.PermissionHandler
) : RecordAudioPermissionState {
    private var _isGranted by mutableStateOf(permissionHandler.hasRecordAudioPermission())

    override val isGranted: Boolean
        get() = _isGranted

    override fun launchPermissionRequest() {
        permissionHandler.requestRecordAudioPermission { granted ->
            _isGranted = granted
        }
    }
}

/**
 * Android-specific implementation of permission state
 */
@Composable
actual fun rememberRecordAudioPermissionState(): RecordAudioPermissionState {
    val permissionHandler = LocalPermissionHandler.current
    return remember {
        AndroidRecordAudioPermissionState(permissionHandler)
    }
}