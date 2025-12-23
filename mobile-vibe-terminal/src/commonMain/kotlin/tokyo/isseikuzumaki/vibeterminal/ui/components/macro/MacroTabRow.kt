package tokyo.isseikuzumaki.vibeterminal.ui.components.macro

import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

@Composable
fun MacroTabRow(
    selectedTab: MacroTab,
    isAlternateScreen: Boolean,
    onTabSelected: (MacroTab) -> Unit,
    modifier: Modifier = Modifier
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = Color(0xFF1A1A1A),
        contentColor = Color(0xFF00FF00),
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                color = if (isAlternateScreen && selectedTab == MacroTab.VIM) {
                    Color(0xFF39D353)
                } else {
                    Color(0xFF00FF00)
                }
            )
        },
        modifier = modifier
    ) {
        MacroTab.entries.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        text = when (tab) {
                            MacroTab.BASIC -> "Basic"
                            MacroTab.NAV -> "Nav"
                            MacroTab.VIM -> "Vim"
                            MacroTab.FUNCTION -> "Function"
                        },
                        fontSize = 14.sp
                    )
                }
            )
        }
    }
}
