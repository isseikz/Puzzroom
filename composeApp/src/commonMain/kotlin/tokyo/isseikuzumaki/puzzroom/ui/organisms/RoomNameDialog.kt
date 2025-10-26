package tokyo.isseikuzumaki.puzzroom.ui.organisms

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Dialog for entering a room name before saving
 */
@Composable
fun RoomNameDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    initialName: String = "New Room"
) {
    var roomName by remember { mutableStateOf(initialName) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Enter Room Name")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Please enter a name for this room:")

                OutlinedTextField(
                    value = roomName,
                    onValueChange = {
                        roomName = it
                        isError = it.isBlank()
                    },
                    label = { Text("Room Name") },
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("Room name cannot be empty") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (roomName.isNotBlank()) {
                        onConfirm(roomName.trim())
                    } else {
                        isError = true
                    }
                },
                enabled = roomName.isNotBlank()
            ) {
                Text("Create Room")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
