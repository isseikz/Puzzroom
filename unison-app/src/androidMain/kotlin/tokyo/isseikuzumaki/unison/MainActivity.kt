package tokyo.isseikuzumaki.unison

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tokyo.isseikuzumaki.unison.audio.createAudioPlayer
import tokyo.isseikuzumaki.unison.screens.fileselection.FileSelectionScreen
import tokyo.isseikuzumaki.unison.screens.review.RecordingReviewScreen
import tokyo.isseikuzumaki.unison.screens.session.DemoSessionViewModel
import tokyo.isseikuzumaki.unison.screens.shadowing.ShadowingScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UnisonApp()
        }
    }
}

@Composable
fun UnisonApp() {
    val context = LocalContext.current

    // Create AudioPlayer and ViewModel with it
    val viewModel: DemoSessionViewModel = viewModel {
        DemoSessionViewModel(createAudioPlayer(context))
    }
    val shadowingData by viewModel.shadowingData.collectAsState()
    val recordingData by viewModel.recordingData.collectAsState()

    var currentScreen by remember { mutableStateOf("fileSelection") }
    var selectedAudioUri by remember { mutableStateOf<String?>(null) }
    var selectedTranscriptionUri by remember { mutableStateOf<String?>(null) }

    // Helper function to read transcription file content
    val loadTranscriptionContent: suspend (String) -> String = { uriString ->
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            } ?: ""
        }
    }

    when (currentScreen) {
        "fileSelection" -> {
            FileSelectionScreen(
                onNavigateBack = {
                    // Optional: Handle back navigation (e.g., exit app)
                },
                onFilesSelected = { audioUri, transcriptionUri ->
                    selectedAudioUri = audioUri
                    selectedTranscriptionUri = transcriptionUri
                    currentScreen = "shadowing"
                }
            )
        }
        "shadowing" -> {
            ShadowingScreen(
                viewModel = viewModel,
                uri = selectedAudioUri,
                transcriptionUri = selectedTranscriptionUri,
                loadTranscription = loadTranscriptionContent,
                onNavigateBack = {
                    currentScreen = "fileSelection"
                },
                onRecordingComplete = {
                    currentScreen = "review"
                }
            )
        }
        "review" -> {
            recordingData?.let { recording ->
                shadowingData?.let { data ->
                    RecordingReviewScreen(
                        recordingDurationMs = recording.durationMs,
                        originalDurationMs = data.durationMs,
                        originalTranscript = data.transcript,
                        onNavigateBack = {
                            currentScreen = "shadowing"
                        },
                        onAcceptRecording = {
                            // TODO: Save or process the recording
                            viewModel.clearRecording()
                            currentScreen = "shadowing"
                        },
                        onRetryRecording = {
                            viewModel.clearRecording()
                            currentScreen = "shadowing"
                        }
                    )
                }
            }
        }
    }
}
