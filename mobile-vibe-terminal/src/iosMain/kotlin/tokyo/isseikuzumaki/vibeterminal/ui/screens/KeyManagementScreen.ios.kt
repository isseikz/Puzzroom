package tokyo.isseikuzumaki.vibeterminal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
import tokyo.isseikuzumaki.vibeterminal.security.SshKeyInfoCommon
import tokyo.isseikuzumaki.vibeterminal.security.SshKeyProvider
import tokyo.isseikuzumaki.vibeterminal.security.SshKeyProviderIos
import tokyo.isseikuzumaki.vibeterminal.util.ClipboardManager

actual fun KeyManagementScreen(): Screen = IosKeyManagementScreen()

private class IosKeyManagementScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val keyProvider = koinInject<SshKeyProviderIos>()

        var keys by remember { mutableStateOf(keyProvider.listKeys()) }
        var showDeleteDialog by remember { mutableStateOf<SshKeyInfoCommon?>(null) }
        var snackbarMessage by remember { mutableStateOf<String?>(null) }
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(snackbarMessage) {
            snackbarMessage?.let {
                snackbarHostState.showSnackbar(it)
                snackbarMessage = null
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("SSH Key Management") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            if (keys.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No SSH keys stored",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Import keys from the connection settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(keys) { keyInfo ->
                        KeyItemCard(
                            keyInfo = keyInfo,
                            keyProvider = keyProvider,
                            onCopied = { snackbarMessage = "Public key copied to clipboard" },
                            onDeleteRequest = { showDeleteDialog = keyInfo }
                        )
                    }
                }
            }
        }

        showDeleteDialog?.let { keyInfo ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Delete Key") },
                text = { Text("Delete key \"${keyInfo.alias}\"? This cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            keyProvider.deleteKey(keyInfo.alias)
                            keys = keyProvider.listKeys()
                            showDeleteDialog = null
                            snackbarMessage = "Key deleted"
                        }
                    ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun KeyItemCard(
    keyInfo: SshKeyInfoCommon,
    keyProvider: SshKeyProviderIos,
    onCopied: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Key,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(keyInfo.alias, style = MaterialTheme.typography.titleSmall)
                Text(
                    keyInfo.algorithm,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = {
                    val pubKey = keyProvider.getPublicKeyOpenSsh(keyInfo.alias)
                    if (pubKey != null) {
                        ClipboardManager.copyToClipboard(pubKey)
                        onCopied()
                    }
                }
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy public key")
            }
            IconButton(onClick = onDeleteRequest) {
                Icon(Icons.Default.Delete, contentDescription = "Delete key")
            }
        }
    }
}
