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
import org.jetbrains.compose.resources.stringResource
import puzzroom.mobile_vibe_terminal.generated.resources.*

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
    val presetTmux = stringResource(Res.string.startup_preset_tmux)
    val presetScreen = stringResource(Res.string.startup_preset_screen)
    val presetByobu = stringResource(Res.string.startup_preset_byobu)
    val presetLogWatch = stringResource(Res.string.startup_preset_log_watch)
    val presetHome = stringResource(Res.string.startup_preset_home)

    val presets = listOf(
        Pair("tmux attach || tmux new", presetTmux),
        Pair("screen -r || screen", presetScreen),
        Pair("byobu", presetByobu),
        Pair("tail -f /var/log/syslog", presetLogWatch),
        Pair("cd ~ && bash", presetHome)
    )

    Column(modifier = modifier) {
        OutlinedTextField(
            value = command,
            onValueChange = onCommandChange,
            label = { Text(stringResource(Res.string.startup_command_label)) },
            placeholder = { Text(stringResource(Res.string.startup_command_placeholder)) },
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, stringResource(Res.string.startup_command_presets))
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
