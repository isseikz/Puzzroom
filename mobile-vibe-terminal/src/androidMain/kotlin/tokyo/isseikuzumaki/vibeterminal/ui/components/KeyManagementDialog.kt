package tokyo.isseikuzumaki.vibeterminal.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.vibeterminal.security.KeyAlgorithm
import tokyo.isseikuzumaki.vibeterminal.security.SshKeyInfoCommon
import tokyo.isseikuzumaki.vibeterminal.security.SshKeyManager
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog for selecting an existing key or creating a new one.
 */
@Composable
fun KeySelectionDialog(
    sshKeyManager: SshKeyManager,
    selectedKeyAlias: String?,
    onKeySelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var keys by remember { mutableStateOf(sshKeyManager.listKeys()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showPublicKeyDialog by remember { mutableStateOf<String?>(null) }
    var selectedAlias by remember { mutableStateOf(selectedKeyAlias) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("SSH Key") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (keys.isEmpty()) {
                    Text(
                        text = "No SSH keys found. Create a new key to use public key authentication.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                } else {
                    Text(
                        text = "Select a key:",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(keys) { keyInfo ->
                            KeyItem(
                                keyInfo = keyInfo,
                                isSelected = keyInfo.alias == selectedAlias,
                                onSelect = { selectedAlias = keyInfo.alias },
                                onShowPublicKey = { showPublicKeyDialog = keyInfo.alias },
                                onDelete = {
                                    sshKeyManager.deleteKey(keyInfo.alias)
                                    keys = sshKeyManager.listKeys()
                                    if (selectedAlias == keyInfo.alias) {
                                        selectedAlias = null
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Key")
                }
                Button(
                    onClick = {
                        onKeySelected(selectedAlias)
                        onDismiss()
                    },
                    enabled = selectedAlias != null
                ) {
                    Text("Select")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showCreateDialog) {
        CreateKeyDialog(
            sshKeyManager = sshKeyManager,
            onKeyCreated = { alias ->
                keys = sshKeyManager.listKeys()
                selectedAlias = alias
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    showPublicKeyDialog?.let { alias ->
        PublicKeyDialog(
            sshKeyManager = sshKeyManager,
            keyAlias = alias,
            onDismiss = { showPublicKeyDialog = null }
        )
    }
}

@Composable
private fun KeyItem(
    keyInfo: SshKeyInfoCommon,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onShowPublicKey: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .selectable(
                selected = isSelected,
                onClick = onSelect
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF2A3A2A) else Color(0xFF2A2A2A)
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF00FF00))
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Key,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF00FF00) else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = keyInfo.alias,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${keyInfo.algorithm} - ${dateFormat.format(Date(keyInfo.createdAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            IconButton(onClick = onShowPublicKey) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy Public Key",
                    tint = Color(0xFF00FF00)
                )
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Key?") },
            text = {
                Text(
                    "Are you sure you want to delete the key \"${keyInfo.alias}\"?\n\n" +
                    "Note: You will also need to remove the public key from your server's authorized_keys file."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Dialog for creating a new SSH key.
 */
@Composable
fun CreateKeyDialog(
    sshKeyManager: SshKeyManager,
    onKeyCreated: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var keyName by remember { mutableStateOf("") }
    var selectedAlgorithm by remember { mutableStateOf(KeyAlgorithm.RSA_4096) }
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text("Create SSH Key") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = keyName,
                    onValueChange = {
                        keyName = it.filter { c -> c.isLetterOrDigit() || c == '_' || c == '-' }
                        errorMessage = null
                    },
                    label = { Text("Key Name") },
                    placeholder = { Text("my-server-key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating
                )

                Text(
                    text = "Algorithm:",
                    style = MaterialTheme.typography.labelMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KeyAlgorithm.entries.forEach { algorithm ->
                        FilterChip(
                            selected = selectedAlgorithm == algorithm,
                            onClick = { selectedAlgorithm = algorithm },
                            label = { Text(algorithm.displayName) },
                            enabled = !isCreating
                        )
                    }
                }

                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (isCreating) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFF00FF00),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Generating key...", color = Color.Gray)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (keyName.isBlank()) {
                        errorMessage = "Key name is required"
                        return@Button
                    }
                    if (sshKeyManager.keyExists(keyName)) {
                        errorMessage = "A key with this name already exists"
                        return@Button
                    }

                    isCreating = true
                    try {
                        sshKeyManager.generateKeyPair(keyName, selectedAlgorithm)
                        onKeyCreated(keyName)
                    } catch (e: Exception) {
                        errorMessage = "Failed to create key: ${e.message}"
                        isCreating = false
                    }
                },
                enabled = keyName.isNotBlank() && !isCreating
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isCreating
            ) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog for displaying and copying the public key.
 */
@Composable
fun PublicKeyDialog(
    sshKeyManager: SshKeyManager,
    keyAlias: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val publicKey = remember {
        sshKeyManager.getPublicKeyOpenSSH(keyAlias, comment = "vibe-terminal")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Public Key: $keyAlias") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Add this to your server's ~/.ssh/authorized_keys file:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1A1A1A)
                    )
                ) {
                    Text(
                        text = publicKey ?: "Error: Could not retrieve public key",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF00FF00),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    publicKey?.let { key ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("SSH Public Key", key)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Public key copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                },
                enabled = publicKey != null
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Copy")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
