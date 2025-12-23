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
import tokyo.isseikuzumaki.vibeterminal.viewmodel.TerminalState

@Composable
fun MacroInputPanel(
    state: TerminalState,
    inputText: String,
    onInputChange: (String) -> Unit,
    onSendCommand: (String) -> Unit,
    onDirectSend: (String) -> Unit,
    onTabSelected: (MacroTab) -> Unit,
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
            onDirectSend = onDirectSend,
            onBufferInsert = { text ->
                onInputChange(inputText + text)
            }
        )

        HorizontalDivider(
            color = Color(0xFF00FF00).copy(alpha = 0.3f),
            thickness = 1.dp
        )

        // Buffered Input Field (always visible)
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
                    if (inputText.isNotBlank()) {
                        onSendCommand(inputText)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00FF00),
                    contentColor = Color.Black
                ),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Send", fontSize = 14.sp)
            }
        }
    }
}
