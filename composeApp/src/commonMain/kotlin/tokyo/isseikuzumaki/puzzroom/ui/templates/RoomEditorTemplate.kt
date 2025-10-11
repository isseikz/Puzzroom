package tokyo.isseikuzumaki.puzzroom.ui.templates

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tokyo.isseikuzumaki.puzzroom.ui.molecules.ActionButton
import tokyo.isseikuzumaki.puzzroom.ui.organisms.EditorToolbar
import tokyo.isseikuzumaki.puzzroom.ui.state.SaveState

/**
 * Room editor template
 * 部屋編集画面のテンプレート
 */
@Composable
fun RoomEditorTemplate(
    saveState: SaveState,
    onSaveRetry: () -> Unit,
    toolbarActions: List<ActionButton>,
    toolbarExtraContent: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier,
    mainContent: @Composable BoxScope.() -> Unit,
    sidePanel: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Toolbar
        EditorToolbar(
            saveState = saveState,
            onSaveRetry = onSaveRetry,
            actions = toolbarActions,
            extraContent = toolbarExtraContent
        )

        // Main content area
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Main canvas area
            Box(modifier = Modifier.weight(1f)) {
                mainContent()
            }

            // Side panel
            sidePanel()
        }
    }
}
