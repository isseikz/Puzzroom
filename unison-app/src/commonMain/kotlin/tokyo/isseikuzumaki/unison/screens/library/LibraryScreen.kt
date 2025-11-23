package tokyo.isseikuzumaki.unison.screens.library

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

/**
 * Platform-specific library screen wrapper
 */
@Composable
expect fun LibraryScreenPlatform(
    onNavigateToSession: (String) -> Unit,
    viewModel: LibraryViewModel = koinViewModel()
)

/**
 * Library Screen - Entry point of the app
 * Allows users to select an audio file to start a shadowing session
 */
@Composable
internal fun LibraryScreen(
    onFileSelected: () -> Unit,
    onNavigateToSession: (String) -> Unit,
    viewModel: LibraryViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle navigation when file is validated
    LaunchedEffect(uiState) {
        if (uiState is LibraryUiState.Valid) {
            val uri = (uiState as LibraryUiState.Valid).uri
            onNavigateToSession(uri)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Unison - Shadowing Sync Player") }
            )
        },
        snackbarHost = {
            if (uiState is LibraryUiState.Error) {
                Snackbar(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text((uiState as LibraryUiState.Error).message)
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Unison",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Eliminate lag in shadowing practice by syncing your voice with the original audio",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onFileSelected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = uiState !is LibraryUiState.Validating
                ) {
                    if (uiState is LibraryUiState.Validating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Select Audio File")
                    }
                }

                Text(
                    text = "Supported formats: MP3, AAC, WAV, and more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
