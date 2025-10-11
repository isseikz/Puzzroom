package tokyo.isseikuzumaki.puzzroom.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tokyo.isseikuzumaki.puzzroom.ui.state.SaveState

/**
 * 保存状態インジケーター (compatibility wrapper)
 * This is a compatibility wrapper that delegates to the Atomic Design molecule
 */
@Composable
fun SaveStateIndicator(
    saveState: SaveState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Delegate to the Atomic Design molecule
    tokyo.isseikuzumaki.puzzroom.ui.molecules.SaveStateIndicator(
        saveState = saveState,
        onRetry = onRetry,
        modifier = modifier
    )
}
