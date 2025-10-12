package tokyo.isseikuzumaki.puzzroom.ui.component

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
import tokyo.isseikuzumaki.puzzroom.ui.state.SaveState

/**
 * 保存状態インジケーター
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
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Saved",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
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
                Spacer(modifier = Modifier.width(4.dp))
                Text(
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
                IconButton(
                    onClick = onRetry,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Failed to save",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Failed to save",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
