package tokyo.isseikuzumaki.audioscriptplayer.ui.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import tokyo.isseikuzumaki.audioscriptplayer.data.AlignmentState
import tokyo.isseikuzumaki.audioscriptplayer.ui.organisms.PlayerControls
import tokyo.isseikuzumaki.audioscriptplayer.ui.organisms.PlayerHeader
import tokyo.isseikuzumaki.audioscriptplayer.ui.organisms.ScriptView
import tokyo.isseikuzumaki.audioscriptplayer.ui.state.PlayerEvent
import tokyo.isseikuzumaki.audioscriptplayer.ui.viewmodel.AlignmentViewModel

/**
 * Main page for the Audio-Script Alignment Player.
 * Composes header, script view, and player controls.
 */
@Composable
fun AlignmentPlayerPage(
    viewModel: AlignmentViewModel = viewModel { AlignmentViewModel() },
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show error messages
    LaunchedEffect(uiState.modelError, uiState.alignmentError) {
        val error = uiState.modelError ?: uiState.alignmentError
        if (error != null) {
            snackbarHostState.showSnackbar(error)
        }
    }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header with model status and process button
            PlayerHeader(
                modelState = uiState.modelState,
                alignmentState = uiState.alignmentState,
                onLoadModel = { viewModel.onEvent(PlayerEvent.LoadModel) },
                onProcess = { viewModel.onEvent(PlayerEvent.ProcessAlignment) },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Script view (main content area)
            ScriptView(
                alignedWords = uiState.alignedWords,
                currentWordIndex = uiState.currentWordIndex,
                onWordClick = { index -> 
                    viewModel.onEvent(PlayerEvent.WordClicked(index))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            
            // Player controls
            PlayerControls(
                isPlaying = uiState.isPlaying,
                currentPositionMs = uiState.currentPositionMs,
                durationMs = uiState.durationMs,
                enabled = uiState.alignmentState == AlignmentState.Completed,
                onPlayPause = { viewModel.onEvent(PlayerEvent.PlayPause) },
                onSeek = { position -> viewModel.onEvent(PlayerEvent.Seek(position)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
