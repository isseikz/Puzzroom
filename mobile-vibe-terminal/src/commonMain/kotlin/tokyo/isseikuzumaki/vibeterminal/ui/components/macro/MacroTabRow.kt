package tokyo.isseikuzumaki.vibeterminal.ui.components.macro

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import puzzroom.mobile_vibe_terminal.generated.resources.*

@Composable
fun MacroTabRow(
    selectedTab: MacroTab,
    isAlternateScreen: Boolean,
    onTabSelected: (MacroTab) -> Unit,
    modifier: Modifier = Modifier
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                color = MaterialTheme.colorScheme.primary
            )
        },
        modifier = modifier
    ) {
        MacroTab.entries.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                text = {
                    Text(
                        text = when (tab) {
                            MacroTab.BASIC -> stringResource(Res.string.macro_tab_basic)
                            MacroTab.NAV -> stringResource(Res.string.macro_tab_nav)
                            MacroTab.VIM -> stringResource(Res.string.macro_tab_vim)
                            MacroTab.FUNCTION -> stringResource(Res.string.macro_tab_function)
                        },
                        fontSize = 14.sp
                    )
                }
            )
        }
    }
}
