package tokyo.isseikuzumaki.puzzroom.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SaveDialog(
    json: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Polygons を保存") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text("以下の JSON をコピーして保存してください:")
                Spacer(modifier = Modifier.height(8.dp))
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
            Button(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    )
}

@Composable
fun LoadDialog(
    onLoad: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var jsonInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Polygons を読み込み") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text("JSON を貼り付けてください:")
                Spacer(modifier = Modifier.height(8.dp))
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                try {
                    onLoad(jsonInput)
                } catch (e: Exception) {
                    errorMessage = "読み込みに失敗しました: ${e.message}"
                }
            }) {
                Text("読み込む")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}
