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
import tokyo.isseikuzumaki.unison.audio.createAudioRecorder
import tokyo.isseikuzumaki.unison.screens.fileselection.FileSelectionScreen
import tokyo.isseikuzumaki.unison.screens.review.RecordingReviewScreen
import tokyo.isseikuzumaki.unison.screens.session.DemoSessionViewModel
import tokyo.isseikuzumaki.unison.screens.shadowing.ShadowingScreen
import java.io.File

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

    // Helper function to get actual file name from URI using ContentResolver
    val getFileNameFromUri: (String) -> String? = { uriString ->
        try {
            val uri = Uri.parse(uriString)
            var fileName: String? = null

            // Query the content resolver for the display name
            context.contentResolver.query(
                uri,
                arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            }

            // Remove file extension and return
            fileName?.substringBeforeLast('.') ?: fileName
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Create recording output path generator
    val getRecordingOutputPath: () -> String = {
        val recordingsDir = File(context.filesDir, "recordings")
        recordingsDir.mkdirs()
        val timestamp = System.currentTimeMillis()
        File(recordingsDir, "recording_$timestamp.m4a").absolutePath
    }

    // Create AudioPlayer, AudioRecorder, and ViewModel with them
    val viewModel: DemoSessionViewModel = viewModel {
        DemoSessionViewModel(
            audioPlayer = createAudioPlayer(context),
            audioRecorder = createAudioRecorder(context),
            getRecordingOutputPath = getRecordingOutputPath
        )
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
                getFileNameFromUri = getFileNameFromUri,
                onNavigateBack = {
                    currentScreen = "fileSelection"
                },
                onRecordingComplete = { recordingData ->
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
