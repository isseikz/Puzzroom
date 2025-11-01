package tokyo.isseikuzumaki.quickdeploy

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import tokyo.isseikuzumaki.puzzroom.shared.ui.theme.PuzzroomTheme

/**
 * Main entry point for Quick Deploy application.
 * 
 * Quick Deploy is a streamlined deployment tool for the Puzzroom ecosystem.
 */
@Composable
fun App() {
    PuzzroomTheme {
        Scaffold { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Quick Deploy",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
