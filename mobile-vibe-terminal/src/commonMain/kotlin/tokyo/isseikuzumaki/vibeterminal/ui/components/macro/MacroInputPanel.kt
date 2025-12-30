package tokyo.isseikuzumaki.vibeterminal.ui.components.macro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import tokyo.isseikuzumaki.vibeterminal.viewmodel.InputMode
import tokyo.isseikuzumaki.vibeterminal.viewmodel.TerminalState
import org.jetbrains.compose.resources.stringResource
import puzzroom.mobile_vibe_terminal.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroInputPanel(
    state: TerminalState,
    inputText: TextFieldValue,
    onInputChange: (TextFieldValue) -> Unit,
    onSendCommand: (String) -> Unit,
    onDirectSend: (String) -> Unit,
    onTabSelected: (MacroTab) -> Unit,
    onToggleInputMode: () -> Unit,
    onToggleCtrl: () -> Unit,
    onToggleAlt: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A))
    ) {
        // Tab Row
        MacroTabRow(
            selectedTab = state.selectedMacroTab,
            isAlternateScreen = state.isAlternateScreen,
            onTabSelected = onTabSelected
        )

        HorizontalDivider(
            color = Color(0xFF00FF00).copy(alpha = 0.3f),
            thickness = 1.dp
        )

        // Button Grid
        MacroButtonGrid(
            tab = state.selectedMacroTab,
            isAlternateScreen = state.isAlternateScreen,
            onDirectSend = { sequence ->
                onDirectSend(sequence)
            },
            onBufferInsert = { text ->
                // Directly send text macros instead of buffering
                onDirectSend(text)
            }
        )

        HorizontalDivider(
            color = Color(0xFF00FF00).copy(alpha = 0.3f),
            thickness = 1.dp
        )

        // Controls Row (Input Mode, Ctrl, Alt)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mode Toggle
            FilterChip(
                selected = state.inputMode == InputMode.TEXT,
                onClick = onToggleInputMode,
                label = { Text(if (state.inputMode == InputMode.TEXT) stringResource(Res.string.macro_text_mode) else stringResource(Res.string.macro_cmd_mode)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF00FF00),
                    selectedLabelColor = Color.Black,
                    containerColor = Color.DarkGray,
                    labelColor = Color.White
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = state.inputMode == InputMode.TEXT,
                    borderColor = Color(0xFF00FF00).copy(alpha = 0.5f),
                    selectedBorderColor = Color(0xFF00FF00)
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            // Ctrl Toggle
            FilterChip(
                selected = state.isCtrlActive,
                onClick = onToggleCtrl,
                label = { Text(stringResource(Res.string.macro_ctrl)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF00FF00),
                    selectedLabelColor = Color.Black,
                    containerColor = Color.DarkGray,
                    labelColor = Color.White
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = state.isCtrlActive,
                    borderColor = Color(0xFF00FF00).copy(alpha = 0.5f),
                    selectedBorderColor = Color(0xFF00FF00)
                )
            )

            // Alt Toggle
            FilterChip(
                selected = state.isAltActive,
                onClick = onToggleAlt,
                label = { Text(stringResource(Res.string.macro_alt)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF00FF00),
                    selectedLabelColor = Color.Black,
                    containerColor = Color.DarkGray,
                    labelColor = Color.White
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = state.isAltActive,
                    borderColor = Color(0xFF00FF00).copy(alpha = 0.5f),
                    selectedBorderColor = Color(0xFF00FF00)
                )
            )
        }
    }
}

