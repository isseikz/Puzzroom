package tokyo.isseikuzumaki.puzzroom.ui.molecules

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppCaptionText
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppIcon
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppIconButton
import tokyo.isseikuzumaki.puzzroom.ui.state.SaveState

/**
 * Save state indicator molecule
 * 保存状態を表示する分子コンポーネント
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
                    contentDescription = "保存済み",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                AppCaptionText(text = "保存済み")
            }
        }
        SaveState.Saving -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier.padding(horizontal = 8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                AppCaptionText(text = "保存中...")
            }
        }
        is SaveState.Failed -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier.padding(horizontal = 8.dp)
            ) {
                AppIconButton(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "保存失敗",
                    onClick = onRetry,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                AppCaptionText(text = "保存失敗")
            }
        }
    }
}
