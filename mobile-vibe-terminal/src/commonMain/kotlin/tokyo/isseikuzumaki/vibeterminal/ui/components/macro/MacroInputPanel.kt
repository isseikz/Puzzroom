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
                if (state.inputMode == InputMode.TEXT && isNavSequence(sequence)) {
                    handleNavInText(sequence, inputText, onInputChange)
                } else {
                    onDirectSend(sequence)
                }
            },
            onBufferInsert = { text ->
                val newText = inputText.text.substring(0, inputText.selection.start) +
                        text +
                        inputText.text.substring(inputText.selection.end)
                val newSelection = TextRange(inputText.selection.start + text.length)
                onInputChange(inputText.copy(text = newText, selection = newSelection))
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
                label = { Text(if (state.inputMode == InputMode.TEXT) "TEXT MODE" else "CMD MODE") },
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
                label = { Text("CTRL") },
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
                label = { Text("ALT") },
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

        // Buffered Input Field (visible only in TEXT mode)
        AnimatedVisibility(visible = state.inputMode == InputMode.TEXT) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    placeholder = { Text("Enter command...", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF00FF00),
                        unfocusedTextColor = Color(0xFF00FF00),
                        focusedContainerColor = Color.Black,
                        unfocusedContainerColor = Color.Black,
                        cursorColor = Color(0xFF00FF00),
                        focusedBorderColor = Color(0xFF00FF00),
                        unfocusedBorderColor = Color(0xFF00FF00).copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                )

                Button(
                    onClick = {
                        if (inputText.text.isEmpty()) {
                            onDirectSend("\r")
                        } else {
                            onSendCommand(inputText.text)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00FF00),
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(if (inputText.text.isEmpty()) "Enter" else "Send", fontSize = 14.sp)
                }
            }
        }
    }
}

private fun isNavSequence(sequence: String): Boolean {
    return sequence == "\u001B[A" || sequence == "\u001B[B" ||
            sequence == "\u001B[C" || sequence == "\u001B[D"
}

private fun handleNavInText(
    sequence: String,
    currentValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit
) {
    val cursor = currentValue.selection.start
    val text = currentValue.text

    when (sequence) {
        "\u001B[C" -> { // Right
            if (cursor < text.length) {
                onValueChange(currentValue.copy(selection = TextRange(cursor + 1)))
            }
        }
        "\u001B[D" -> { // Left
            if (cursor > 0) {
                onValueChange(currentValue.copy(selection = TextRange(cursor - 1)))
            }
        }
        "\u001B[A" -> { // Up -> Start of line
            onValueChange(currentValue.copy(selection = TextRange(0)))
        }
        "\u001B[B" -> { // Down -> End of line
            onValueChange(currentValue.copy(selection = TextRange(text.length)))
        }
    }
}
