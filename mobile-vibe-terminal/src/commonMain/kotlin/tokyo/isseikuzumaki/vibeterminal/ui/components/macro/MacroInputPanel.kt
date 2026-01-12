package tokyo.isseikuzumaki.vibeterminal.ui.components.macro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.focusProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardHide
import org.jetbrains.compose.resources.stringResource
import puzzroom.mobile_vibe_terminal.generated.resources.*
import io.github.isseikz.kmpinput.TerminalInputContainer
import io.github.isseikz.kmpinput.TerminalInputContainerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroInputPanel(
    selectedMacroTab: MacroTab,
    isAlternateScreen: Boolean,
    isImeEnabled: Boolean,
    isSoftKeyboardVisible: Boolean,
    isCtrlActive: Boolean,
    isAltActive: Boolean,
    isHardwareKeyboardConnected: Boolean,
    onDirectSend: (String) -> Unit,
    onTabSelected: (MacroTab) -> Unit,
    onToggleImeMode: () -> Unit,
    onToggleSoftKeyboard: () -> Unit,
    onToggleCtrl: () -> Unit,
    onToggleAlt: () -> Unit,
    modifier: Modifier = Modifier,
    terminalInputState: TerminalInputContainerState? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Tab Row
        MacroTabRow(
            selectedTab = selectedMacroTab,
            isAlternateScreen = isAlternateScreen,
            onTabSelected = onTabSelected
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            thickness = 1.dp
        )

        // Button Grid
        MacroButtonGrid(
            tab = selectedMacroTab,
            isAlternateScreen = isAlternateScreen,
            onDirectSend = { sequence ->
                onDirectSend(sequence)
            },
            onBufferInsert = { text ->
                onDirectSend(text)
            }
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            thickness = 1.dp
        )

        // Controls Row (Input Mode, Keyboard, Ctrl, Alt)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // IME Mode Toggle (TEXT MODE is special - uses primary color outline)
            // Disabled when hardware keyboard is connected (locked to CMD mode)
            FilterChip(
                selected = isImeEnabled,
                onClick = onToggleImeMode,
                enabled = !isHardwareKeyboardConnected,
                label = { Text(if (isImeEnabled) stringResource(Res.string.macro_text_mode) else stringResource(Res.string.macro_cmd_mode)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = !isHardwareKeyboardConnected,
                    selected = isImeEnabled,
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                modifier = Modifier.focusProperties { canFocus = false }
            )

            // Keyboard Visibility Toggle
            if (terminalInputState != null) {
                TerminalInputContainer(
                    state = terminalInputState,
                    modifier = Modifier.wrapContentSize()
                ) {
                    IconButton(
                        onClick = onToggleSoftKeyboard,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (isSoftKeyboardVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = if (isSoftKeyboardVisible) Icons.Default.KeyboardHide else Icons.Default.Keyboard,
                            contentDescription = "Toggle Keyboard"
                        )
                    }
                }
            } else {
                IconButton(
                    onClick = onToggleSoftKeyboard,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = if (isSoftKeyboardVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = if (isSoftKeyboardVisible) Icons.Default.KeyboardHide else Icons.Default.Keyboard,
                        contentDescription = "Toggle Keyboard"
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Ctrl Toggle
            FilterChip(
                selected = isCtrlActive,
                onClick = onToggleCtrl,
                label = { Text(stringResource(Res.string.macro_ctrl)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isCtrlActive,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    selectedBorderColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.focusProperties { canFocus = false }
            )

            // Alt Toggle
            FilterChip(
                selected = isAltActive,
                onClick = onToggleAlt,
                label = { Text(stringResource(Res.string.macro_alt)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isAltActive,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    selectedBorderColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.focusProperties { canFocus = false }
            )
        }
    }
}
