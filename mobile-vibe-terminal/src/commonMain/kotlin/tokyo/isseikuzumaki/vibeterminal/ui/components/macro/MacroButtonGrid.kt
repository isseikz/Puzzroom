package tokyo.isseikuzumaki.vibeterminal.ui.components.macro

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MacroButtonGrid(
    tab: MacroTab,
    isAlternateScreen: Boolean,
    onDirectSend: (String) -> Unit,
    onBufferInsert: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = when (tab) {
        MacroTab.BASIC -> MacroConfig.basicKeys
        MacroTab.NAV -> MacroConfig.navKeys
        MacroTab.VIM -> MacroConfig.vimKeys
        MacroTab.FUNCTION -> MacroConfig.functionKeys
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        keys.forEach { macroKey ->
            val isEmphasis = isAlternateScreen && tab == MacroTab.VIM

            MacroButton(
                label = macroKey.label,
                onClick = {
                    when (val action = macroKey.action) {
                        is MacroAction.DirectSend -> onDirectSend(action.sequence)
                        is MacroAction.BufferInsert -> onBufferInsert(action.text)
                    }
                },
                isEmphasis = isEmphasis
            )
        }
    }
}
