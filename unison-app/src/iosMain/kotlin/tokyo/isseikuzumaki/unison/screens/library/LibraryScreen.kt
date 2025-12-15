package tokyo.isseikuzumaki.unison.screens.library

import androidx.compose.runtime.Composable

@Composable
actual fun LibraryScreenPlatform(
    onNavigateToSession: (String, String) -> Unit,
    viewModel: LibraryViewModel
) {
    LibraryScreen(
        onAudioFileSelected = { /* TODO: Implement audio file picker for iOS */ },
        onTranscriptionFileSelected = { /* TODO: Implement transcription file picker for iOS */ },
        onStartSession = { viewModel.onStartSession() },
        onNavigateToSession = onNavigateToSession,
        viewModel = viewModel
    )
}
