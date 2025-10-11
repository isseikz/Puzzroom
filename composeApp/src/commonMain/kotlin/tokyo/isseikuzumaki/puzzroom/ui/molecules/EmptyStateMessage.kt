package tokyo.isseikuzumaki.puzzroom.ui.molecules

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppButton
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppIcon
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppText
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppTitleText
import androidx.compose.material3.MaterialTheme

/**
 * Empty state message molecule
 * 空状態を表示する分子コンポーネント
 */
@Composable
fun EmptyStateMessage(
    title: String,
    description: String,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppTitleText(text = title)
        Spacer(modifier = Modifier.height(8.dp))
        AppText(
            text = description,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        AppButton(
            text = actionText,
            onClick = onAction
        )
    }
}

/**
 * Empty project list message
 */
@Composable
fun EmptyProjectListMessage(
    onCreateNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateMessage(
        title = "プロジェクトがありません",
        description = "新しいプロジェクトを作成しましょう",
        actionText = "新規作成",
        onAction = onCreateNew,
        modifier = modifier
    )
}
