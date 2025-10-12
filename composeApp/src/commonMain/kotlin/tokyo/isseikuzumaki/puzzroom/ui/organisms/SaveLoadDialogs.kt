package tokyo.isseikuzumaki.puzzroom.ui.organisms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppButton
import tokyo.isseikuzumaki.puzzroom.ui.atoms.AppText
import tokyo.isseikuzumaki.puzzroom.ui.atoms.VerticalSpacer

/**
 * Save dialog organism
 * Dialog for displaying and copying JSON data
 */
@Composable
fun SaveDialog(
    json: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText("Save Polygons") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                AppText("Copy and save the following JSON:")
                VerticalSpacer(height = 8.dp)
                TextField(
                    value = json,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .verticalScroll(rememberScrollState())
                )
            }
        },
        confirmButton = {
            AppButton(
                text = "Close",
                onClick = onDismiss
            )
        }
    )
}

/**
 * Load dialog organism
 * Dialog for inputting and loading JSON data
 */
@Composable
fun LoadDialog(
    onLoad: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var jsonInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText("Load") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                AppText("Input JSON:")
                VerticalSpacer(height = 8.dp)
                TextField(
                    value = jsonInput,
                    onValueChange = {
                        jsonInput = it
                        errorMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .verticalScroll(rememberScrollState())
                )
                if (errorMessage != null) {
                    VerticalSpacer(height = 8.dp)
                    AppText(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            AppButton(
                text = "Load",
                onClick = {
                    try {
                        onLoad(jsonInput)
                    } catch (e: Exception) {
                        errorMessage = "Failed to load: ${e.message}"
                    }
                }
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancel",
                onClick = onDismiss
            )
        }
    )
}
