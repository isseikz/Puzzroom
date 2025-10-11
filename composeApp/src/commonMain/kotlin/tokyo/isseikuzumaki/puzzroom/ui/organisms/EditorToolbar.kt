package tokyo.isseikuzumaki.puzzroom.ui.organisms

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.ui.molecules.ActionButton
import tokyo.isseikuzumaki.puzzroom.ui.molecules.ActionButtonGroup
import tokyo.isseikuzumaki.puzzroom.ui.molecules.SaveStateIndicator
import tokyo.isseikuzumaki.puzzroom.ui.state.SaveState

/**
 * Editor toolbar organism
 * エディタのツールバーを表示する有機体コンポーネント
 */
@Composable
fun EditorToolbar(
    saveState: SaveState,
    onSaveRetry: () -> Unit,
    actions: List<ActionButton>,
    modifier: Modifier = Modifier,
    extraContent: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SaveStateIndicator(
            saveState = saveState,
            onRetry = onSaveRetry
        )

        extraContent()

        ActionButtonGroup(buttons = actions)
    }
}
