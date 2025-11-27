package tokyo.isseikuzumaki.audioscriptplayer

import androidx.compose.runtime.Composable
import tokyo.isseikuzumaki.audioscriptplayer.ui.pages.AlignmentPlayerPage
import tokyo.isseikuzumaki.shared.ui.theme.AppTheme

/**
 * Main entry point for the Audio-Script Alignment Player application.
 *
 * This is a prototype app that demonstrates:
 * - Audio-script alignment using Whisper timestamps
 * - Synchronized text highlighting during playback
 * - Touch-to-seek on words
 */
@Composable
fun App() {
    AppTheme {
        AlignmentPlayerPage()
    }
}
