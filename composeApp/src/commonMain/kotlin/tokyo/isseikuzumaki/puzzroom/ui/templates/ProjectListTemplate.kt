package tokyo.isseikuzumaki.puzzroom.ui.templates

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppIcon
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppIconButton
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppText

/**
 * Project list screen template
 * プロジェクト一覧画面のテンプレート
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListTemplate(
    onCreateNew: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { AppText("マイプロジェクト") },
                actions = {
                    AppIconButton(
                        imageVector = Icons.Default.Add,
                        contentDescription = "新規作成",
                        onClick = onCreateNew,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        content(paddingValues)
    }
}
