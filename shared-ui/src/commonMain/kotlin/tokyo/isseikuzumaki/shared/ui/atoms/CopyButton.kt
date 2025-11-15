package tokyo.isseikuzumaki.shared.ui.atoms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Copy button atom for copying content to clipboard
 * 
 * This is a simple icon button with a copy icon, following atomic design principles.
 * The actual clipboard interaction must be handled by the platform-specific code.
 */
@Composable
fun CopyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Copy"
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = contentDescription
        )
    }
}
