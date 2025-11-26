package tokyo.isseikuzumaki.audioscriptplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import tokyo.isseikuzumaki.audioscriptplayer.ui.pages.AlignmentPlayerPage
import tokyo.isseikuzumaki.audioscriptplayer.ui.viewmodel.AndroidAlignmentViewModel
import tokyo.isseikuzumaki.shared.ui.theme.AppTheme

/**
 * Main activity for the Audio-Script Alignment Player.
 * 
 * This activity sets up the Android-specific ViewModel with:
 * - ExoPlayer for actual audio playback
 * - WhisperJniWrapper for speech recognition (when JNI is integrated)
 */
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                val viewModel: AndroidAlignmentViewModel = viewModel(
                    factory = AndroidAlignmentViewModel.Factory(
                        context = applicationContext,
                        // Set to true when whisper.cpp native library is integrated
                        useRealImplementations = false
                    )
                )
                AlignmentPlayerPage(viewModel = viewModel)
            }
        }
    }
}

@Preview
@Composable
fun AppPreview() {
    App()
}
