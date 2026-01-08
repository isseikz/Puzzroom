package tokyo.isseikuzumaki.vibeterminal.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
import tokyo.isseikuzumaki.vibeterminal.security.KeyAlgorithm
import tokyo.isseikuzumaki.vibeterminal.security.SshKeyInfo
import tokyo.isseikuzumaki.vibeterminal.security.SshKeyManager
import java.text.SimpleDateFormat
import java.util.*

actual fun KeyManagementScreen(): Screen = KeyManagementScreenImpl()

private class KeyManagementScreenImpl : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val sshKeyManager = koinInject<SshKeyManager>()

        var keys by remember { mutableStateOf(sshKeyManager.listKeys()) }
        var showCreateDialog by remember { mutableStateOf(false) }
        var showPublicKeyDialog by remember { mutableStateOf<String?>(null) }
        var showDeleteConfirm by remember { mutableStateOf<SshKeyInfo?>(null) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("SSH Keys") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF00FF00)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1A1A1A),
                        titleContentColor = Color(0xFF00FF00)
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = Color(0xFF00FF00),
                    contentColor = Color.Black
                ) {
                    Icon(Icons.Default.Add, "Create Key")
                }
            },
            containerColor = Color.Black
        ) { padding ->
            if (keys.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Key,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "No SSH keys",
                            color = Color.Gray,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Create a key to use public key authentication",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00FF00),
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Create Key")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(keys) { keyInfo ->
                        KeyCard(
                            keyInfo = keyInfo,
                            onShowPublicKey = { showPublicKeyDialog = keyInfo.alias },
                            onDelete = { showDeleteConfirm = keyInfo }
                        )
                    }
                }
            }
        }

        // Create Key Dialog
        if (showCreateDialog) {
            CreateKeyDialog(
                sshKeyManager = sshKeyManager,
                onKeyCreated = { alias ->
                    keys = sshKeyManager.listKeys()
                    showCreateDialog = false
                    // Show public key immediately after creation
                    showPublicKeyDialog = alias
                },
                onDismiss = { showCreateDialog = false }
            )
        }

        // Public Key Dialog
        showPublicKeyDialog?.let { alias ->
            PublicKeyDialog(
                sshKeyManager = sshKeyManager,
                keyAlias = alias,
                onDismiss = { showPublicKeyDialog = null }
            )
        }

        // Delete Confirmation Dialog
        showDeleteConfirm?.let { keyInfo ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = null },
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
                            sshKeyManager.deleteKey(keyInfo.alias)
                            keys = sshKeyManager.listKeys()
                            showDeleteConfirm = null
                        }
                    ) {
                        Text("Delete", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    @Composable
    private fun KeyCard(
        keyInfo: SshKeyInfo,
        onShowPublicKey: () -> Unit,
        onDelete: () -> Unit
    ) {
        val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2A2A2A)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Key,
                    contentDescription = null,
                    tint = Color(0xFF00FF00),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = keyInfo.alias,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = keyInfo.algorithm,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF00FF00)
                    )
                    Text(
                        text = "Created: ${dateFormat.format(Date(keyInfo.createdAt))}",
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
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red
                    )
                }
            }
        }
    }

    @Composable
    private fun CreateKeyDialog(
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

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KeyAlgorithm.entries.forEach { algorithm ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedAlgorithm == algorithm,
                                    onClick = { selectedAlgorithm = algorithm },
                                    enabled = !isCreating
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(algorithm.displayName)
                                    Text(
                                        text = when (algorithm) {
                                            KeyAlgorithm.RSA_4096 -> "Most compatible, works with older servers"
                                            KeyAlgorithm.ECDSA_P256 -> "Faster, smaller keys, modern servers"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
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

    @Composable
    private fun PublicKeyDialog(
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
}
