package tokyo.isseikuzumaki.unison.screens.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * Demo ViewModel for testing ShadowingScreen with dummy data
 * Simulates loading state and provides sample transcript
 */
class DemoSessionViewModel : ViewModel() {

    private val _shadowingData = MutableStateFlow<ShadowingData?>(null)
    val shadowingData: StateFlow<ShadowingData?> = _shadowingData.asStateFlow()

    init {
        loadDummyData()
    }

    private fun loadDummyData() {
        viewModelScope.launch {
            // Simulate loading delay
            delay(1500)

            // Provide dummy data
            _shadowingData.value = ShadowingData(
                pcmData = ByteArray(44100 * 2 * 30), // 30 seconds of dummy audio data
                transcript = """
                    Welcome to the Unison shadowing practice session.

                    This is a demonstration of the ShadowingScreen feature.

                    Shadowing is a language learning technique where you listen to audio and simultaneously repeat what you hear.

                    This screen allows you to:
                    - View the full transcript of the audio
                    - Play the original audio file
                    - Record your voice while practicing
                    - See real-time feedback during recording

                    The interface uses a warm color palette with visual hierarchy to make practice sessions comfortable and effective.

                    Try pressing the play button to start the audio, or the record button to practice speaking along with the transcript.

                    This demo shows how the ViewModel flow correctly propagates data to the UI components.
                """.trimIndent(),
                durationMs = 30000L, // 30 seconds
                fileName = "demo_shadowing_session"
            )
        }
    }

    fun playPreview() {
        // Dummy implementation - no actual audio playback in demo
    }

    fun stopPreview() {
        // Dummy implementation
    }

    fun startRecording() {
        // Dummy implementation - no actual recording in demo
    }

    fun stopRecording() {
        // Dummy implementation
    }
}
