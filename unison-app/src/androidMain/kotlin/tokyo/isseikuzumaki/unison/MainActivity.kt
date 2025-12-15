package tokyo.isseikuzumaki.unison

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
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
    val viewModel: DemoSessionViewModel = viewModel()
    val shadowingData by viewModel.shadowingData.collectAsState()
    val recordingData by viewModel.recordingData.collectAsState()

    var currentScreen by remember { mutableStateOf("shadowing") }

    when (currentScreen) {
        "shadowing" -> {
            ShadowingScreen(
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
