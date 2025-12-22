package tokyo.isseikuzumaki.unison.screens.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import tokyo.isseikuzumaki.unison.audio.AudioPlayer
import tokyo.isseikuzumaki.unison.audio.AudioRecorder

/**
 * Data class combining transcript metadata for shadowing
 * Note: Audio data is not stored in state to avoid memory issues
 */
data class ShadowingData(
    val transcript: String,
    val durationMs: Long,
    val fileName: String
)

/**
 * Data class for recording information
 */
data class RecordingData(
    val durationMs: Long,
    val audioFilePath: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Demo ViewModel for testing ShadowingScreen with dummy data
 * Simulates loading state and provides sample transcript
 */
class DemoSessionViewModel(
    private val audioPlayer: AudioPlayer? = null,
    private val audioRecorder: AudioRecorder? = null,
    private val getRecordingOutputPath: (() -> String)? = null
) : ViewModel() {

    private val _shadowingData = MutableStateFlow<ShadowingData?>(null)
    val shadowingData: StateFlow<ShadowingData?> = _shadowingData.asStateFlow()

    private val _recordingData = MutableStateFlow<RecordingData?>(null)
    val recordingData: StateFlow<RecordingData?> = _recordingData.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var recordingStartTime: Long = 0
    private var currentAudioUri: String? = null
    private var positionTrackingJob: Job? = null
    private var currentRecordingPath: String? = null

    init {
        loadDummyData()

        // Set up audio player completion listener
        audioPlayer?.setOnCompletionListener {
            _isPlaying.value = false
            stopPositionTracking()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPositionTracking()
        audioPlayer?.release()
        audioRecorder?.release()
    }

    /**
     * Start tracking playback position in real-time
     */
    private fun startPositionTracking() {
        stopPositionTracking()
        positionTrackingJob = viewModelScope.launch {
            while (isActive && audioPlayer?.isPlaying() == true) {
                _currentPosition.value = audioPlayer?.getCurrentPosition() ?: 0L
                delay(100) // Update every 100ms
            }
        }
    }

    /**
     * Stop tracking playback position
     */
    private fun stopPositionTracking() {
        positionTrackingJob?.cancel()
        positionTrackingJob = null
    }

    /**
     * Load shadowing data from provided URIs
     * @param audioUri URI of the audio file
     * @param transcriptionUri URI of the transcription file
     * @param loadTranscription Function to load transcription content from URI
     */
    fun loadShadowingData(
        audioUri: String?,
        transcriptionUri: String?,
        loadTranscription: suspend (String) -> String
    ) {
        viewModelScope.launch {
            if (audioUri != null && transcriptionUri != null) {
                try {
                    currentAudioUri = audioUri
                    val transcriptionContent = loadTranscription(transcriptionUri)
                    val fileName = audioUri.substringAfterLast("/")

                    // Load the audio file and get its duration
                    val duration = if (audioPlayer != null) {
                        audioPlayer.load(audioUri)
                    } else {
                        0L
                    }

                    _shadowingData.value = ShadowingData(
                        transcript = transcriptionContent,
                        durationMs = duration,
                        fileName = fileName
                    )
                } catch (e: Exception) {
                    // Handle error - could add error state to ViewModel
                    e.printStackTrace()
                }
            } else {
                // No URIs provided, load dummy data
                loadDummyData()
            }
        }
    }

    private fun loadDummyData() {
        viewModelScope.launch {
            // Simulate loading delay
            delay(1500)

            // Provide dummy data (audio not stored in state)
            _shadowingData.value = ShadowingData(
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
                fileName = "Demo Shadowing Session"
            )
        }
    }

    fun playPreview() {
        audioPlayer?.play()
        _isPlaying.value = true
        startPositionTracking()
    }

    fun stopPreview() {
        audioPlayer?.pause()
        _isPlaying.value = false
        stopPositionTracking()
    }

    fun seekTo(positionMs: Long) {
        audioPlayer?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun startRecording() {
        if (audioRecorder != null && getRecordingOutputPath != null) {
            // Real recording implementation
            recordingStartTime = System.currentTimeMillis()
            currentRecordingPath = getRecordingOutputPath()
            val success = audioRecorder.startRecording(currentRecordingPath!!)
            if (!success) {
                println("Failed to start recording")
                currentRecordingPath = null
            }
        } else {
            // Fallback to dummy implementation for demo mode
            recordingStartTime = System.currentTimeMillis()
        }
    }

    fun stopRecording() {
        if (audioRecorder != null && currentRecordingPath != null) {
            // Real recording implementation
            val filePath = audioRecorder.stopRecording()
            val duration = System.currentTimeMillis() - recordingStartTime

            if (filePath != null) {
                _recordingData.value = RecordingData(
                    durationMs = duration,
                    audioFilePath = filePath
                )
            } else {
                println("Failed to stop recording or save file")
            }
            currentRecordingPath = null
        } else {
            // Fallback to dummy implementation for demo mode
            val duration = System.currentTimeMillis() - recordingStartTime
            _recordingData.value = RecordingData(
                durationMs = duration,
                audioFilePath = "" // Empty path for demo mode
            )
        }
    }

    fun clearRecording() {
        _recordingData.value = null
    }
}
