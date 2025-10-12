package tokyo.isseikuzumaki.puzzroom.ui.molecules

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.ui.atoms.*
import tokyo.isseikuzumaki.puzzroom.ui.state.SaveState

/**
 * Save state indicator molecule
 * Displays the current save state with appropriate icon and message
 */
@Composable
fun SaveStateIndicator(
    saveState: SaveState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (saveState) {
        SaveState.Saved -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier.padding(horizontal = 8.dp)
            ) {
                AppIcon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Saved",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                HorizontalSpacer(width = 4.dp)
                AppText(
                    text = "Saved",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
        SaveState.Saving -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier.padding(horizontal = 8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                HorizontalSpacer(width = 4.dp)
                AppText(
                    text = "Saving...",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        is SaveState.Failed -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier.padding(horizontal = 8.dp)
            ) {
                AppIconButton(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Failed to save",
                    onClick = onRetry,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                HorizontalSpacer(width = 4.dp)
                AppText(
                    text = "Failed to save",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
