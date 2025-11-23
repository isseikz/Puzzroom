package tokyo.isseikuzumaki.unison.screens.library

import androidx.compose.runtime.Composable
import tokyo.isseikuzumaki.unison.LocalFilePicker
import tokyo.isseikuzumaki.unison.LocalPermissionHandler
import org.koin.compose.viewmodel.koinViewModel

/**
 * Android-specific wrapper for LibraryScreen that integrates file picker and permissions
 */
@Composable
actual fun LibraryScreenPlatform(
    onNavigateToSession: (String) -> Unit,
    viewModel: LibraryViewModel
) {
    val filePicker = LocalFilePicker.current
    val permissionHandler = LocalPermissionHandler.current

    LibraryScreen(
        onFileSelected = {
            // Check and request permission if needed, then open file picker
            if (permissionHandler.hasRecordAudioPermission()) {
                filePicker.pickAudioFile { uri ->
                    uri?.let { viewModel.onFileSelected(it.toString()) }
                }
            } else {
                permissionHandler.requestRecordAudioPermission { granted ->
                    if (granted) {
                        filePicker.pickAudioFile { uri ->
                            uri?.let { viewModel.onFileSelected(it.toString()) }
                        }
                    }
                }
            }
        },
        onNavigateToSession = onNavigateToSession,
        viewModel = viewModel
    )
}
