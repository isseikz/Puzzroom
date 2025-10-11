package tokyo.isseikuzumaki.puzzroom.ui.molecules

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppButton
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppText
import tokyo.isseikuzumaki.puzzroom.ui.state.UiError

/**
 * Error message display molecule
 * エラーメッセージを表示する分子コンポーネント
 */
@Composable
fun ErrorMessage(
    error: UiError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppText(
            text = "⚠️",
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        AppText(
            text = error.message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = when (error.severity) {
                UiError.Severity.Error -> MaterialTheme.colorScheme.error
                UiError.Severity.Warning -> MaterialTheme.colorScheme.tertiary
                UiError.Severity.Info -> MaterialTheme.colorScheme.primary
            }
        )
        Spacer(modifier = Modifier.height(24.dp))
        AppButton(
            text = "リトライ",
            onClick = onRetry
        )
    }
}
