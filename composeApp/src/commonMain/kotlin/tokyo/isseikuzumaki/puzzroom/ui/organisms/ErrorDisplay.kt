package tokyo.isseikuzumaki.puzzroom.ui.organisms

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppButton
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppIcon
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppText
import tokyo.isseikuzumaki.puzzroom.ui.atoms.VerticalSpacer
import tokyo.isseikuzumaki.puzzroom.ui.state.UiError

/**
 * Error display organism
 */
@Composable
fun ErrorDisplay(
    error: UiError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppIcon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = when (error.severity) {
                UiError.Severity.Error -> MaterialTheme.colorScheme.error
                UiError.Severity.Warning -> Color(0xFFFF9800)
                UiError.Severity.Info -> MaterialTheme.colorScheme.primary
            }
        )
        VerticalSpacer(height = 16.dp)
        AppText(
            text = error.message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = when (error.severity) {
                UiError.Severity.Error -> MaterialTheme.colorScheme.error
                UiError.Severity.Warning -> Color(0xFFFF9800)
                UiError.Severity.Info -> MaterialTheme.colorScheme.primary
            }
        )
        VerticalSpacer(height = 24.dp)
        AppButton(
            text = "Retry",
            onClick = onRetry
        )
    }
}
