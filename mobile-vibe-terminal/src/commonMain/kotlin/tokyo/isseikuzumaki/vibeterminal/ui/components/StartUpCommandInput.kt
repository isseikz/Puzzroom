package tokyo.isseikuzumaki.vibeterminal.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Input component for startup commands with preset options
 *
 * @param command Current command value
 * @param onCommandChange Callback when command changes
 * @param modifier Modifier for the component
 */
@Composable
fun StartUpCommandInput(
    command: String,
    onCommandChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    // Preset commands with descriptions
    val presets = listOf(
        Pair("tmux attach || tmux new", "tmux (Auto)"),
        Pair("screen -r || screen", "screen (Auto)"),
        Pair("byobu", "byobu"),
        Pair("tail -f /var/log/syslog", "Log Watch"),
        Pair("cd ~ && bash", "Home Directory")
    )

    Column(modifier = modifier) {
        OutlinedTextField(
            value = command,
            onValueChange = onCommandChange,
            label = { Text("Startup Command (Optional)") },
            placeholder = { Text("e.g., tmux attach || tmux new") },
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, "Preset Commands")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            presets.forEach { (cmd, label) ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                cmd,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    },
                    onClick = {
                        onCommandChange(cmd)
                        expanded = false
                    }
                )
            }
        }
    }
}
