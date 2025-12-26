package tokyo.isseikuzumaki.vibeterminal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import androidx.compose.runtime.LaunchedEffect
import tokyo.isseikuzumaki.vibeterminal.domain.model.ConnectionConfig
import tokyo.isseikuzumaki.vibeterminal.domain.model.SavedConnection
import tokyo.isseikuzumaki.vibeterminal.viewmodel.ConnectionListScreenModel
import tokyo.isseikuzumaki.vibeterminal.ui.components.StartUpCommandInput

class ConnectionListScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<ConnectionListScreenModel>()
        val state by screenModel.state.collectAsState()

        var showPasswordDialog by remember { mutableStateOf(false) }
        var selectedConnection by remember { mutableStateOf<SavedConnection?>(null) }
        var password by remember { mutableStateOf("") }
        var savePassword by remember { mutableStateOf(false) }

        // Check for auto-restore on startup
        LaunchedEffect(Unit) {
            screenModel.checkAndRestoreLastSession { config ->
                if (config != null) {
                    navigator.push(TerminalScreen(config))
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Vibe Terminal") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1A1A1A),
                        titleContentColor = Color(0xFF00FF00)
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { screenModel.showAddDialog() },
                    containerColor = Color(0xFF00FF00),
                    contentColor = Color.Black
                ) {
                    Icon(Icons.Default.Add, "Add Connection")
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (state.connections.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "No saved connections",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Tap + to add a new server",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    // Connection grid
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 300.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.connections) { connection ->
                            ConnectionCard(
                                connection = connection,
                                onConnect = {
                                    selectedConnection = connection
                                    // Try to load saved password
                                    screenModel.loadPassword(connection.id) { savedPwd ->
                                        password = savedPwd ?: ""
                                        savePassword = savedPwd != null
                                        showPasswordDialog = true
                                    }
                                },
                                onEdit = { screenModel.showEditDialog(connection) },
                                onDelete = { screenModel.deleteConnection(connection) }
                            )
                        }
                    }
                }
            }
        }

        // Add/Edit Connection Dialog
        if (state.showAddDialog) {
            ConnectionDialog(
                connection = state.editingConnection,
                onDismiss = { screenModel.hideDialog() },
                onSave = { screenModel.saveConnection(it) }
            )
        }

        // Password Dialog
        if (showPasswordDialog && selectedConnection != null) {
            PasswordDialog(
                connection = selectedConnection!!,
                password = password,
                savePassword = savePassword,
                onPasswordChange = { password = it },
                onSavePasswordChange = { savePassword = it },
                onDismiss = { showPasswordDialog = false },
                onConnect = {
                    showPasswordDialog = false
                    screenModel.updateLastUsed(selectedConnection!!.id)

                    // Save password if checkbox is checked
                    if (savePassword) {
                        screenModel.savePassword(selectedConnection!!.id, password)
                    } else {
                        screenModel.deletePassword(selectedConnection!!.id)
                    }

                    navigator.push(
                        TerminalScreen(
                            ConnectionConfig(
                                host = selectedConnection!!.host,
                                port = selectedConnection!!.port,
                                username = selectedConnection!!.username,
                                password = password,
                                connectionId = selectedConnection!!.id,
                                startupCommand = selectedConnection!!.startupCommand
                            )
                        )
                    )
                }
            )
        }

        // Error Snackbar
        state.errorMessage?.let { error ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { screenModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(error)
            }
        }
    }

    @Composable
    private fun ConnectionCard(
        connection: SavedConnection,
        onConnect: () -> Unit,
        onEdit: () -> Unit,
        onDelete: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2A2A2A)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = connection.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00FF00)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${connection.username}@${connection.host}:${connection.port}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onConnect,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00FF00),
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Connect")
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, "Edit", tint = Color(0xFF00FF00))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                    }
                }
            }
        }
    }

    @Composable
    private fun ConnectionDialog(
        connection: SavedConnection?,
        onDismiss: () -> Unit,
        onSave: (SavedConnection) -> Unit
    ) {
        var name by remember { mutableStateOf(connection?.name ?: "") }
        var host by remember { mutableStateOf(connection?.host ?: "") }
        var port by remember { mutableStateOf(connection?.port?.toString() ?: "22") }
        var username by remember { mutableStateOf(connection?.username ?: "") }
        var startupCommand by remember { mutableStateOf(connection?.startupCommand ?: "") }
        var deployPattern by remember { mutableStateOf(connection?.deployPattern ?: ">> VIBE_DEPLOY: (.*)") }
        var isAutoReconnect by remember { mutableStateOf(connection?.isAutoReconnect ?: false) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(if (connection == null) "Add Connection" else "Edit Connection") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("Host") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Port") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Startup Command Input
                    StartUpCommandInput(
                        command = startupCommand,
                        onCommandChange = { startupCommand = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Magic Deploy Pattern
                    OutlinedTextField(
                        value = deployPattern,
                        onValueChange = { deployPattern = it },
                        label = { Text("Magic Deploy Pattern (Regex)") },
                        placeholder = { Text(">> VIBE_DEPLOY: (.*)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Text(
                                "Regex to detect deployable files from terminal output. Use group 1 for file path.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )

                    // Auto Reconnect Checkbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isAutoReconnect,
                            onCheckedChange = { isAutoReconnect = it }
                        )
                        Text(
                            text = "Auto-reconnect on app restart",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val savedConnection = SavedConnection(
                            id = connection?.id ?: 0L,
                            name = name,
                            host = host,
                            port = port.toIntOrNull() ?: 22,
                            username = username,
                            authType = "password",
                            createdAt = connection?.createdAt ?: System.currentTimeMillis(),
                            lastUsedAt = connection?.lastUsedAt,
                            deployPattern = deployPattern.ifBlank { null },
                            startupCommand = startupCommand.ifBlank { null },
                            isAutoReconnect = isAutoReconnect
                        )
                        onSave(savedConnection)
                    },
                    enabled = name.isNotBlank() && host.isNotBlank() && username.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    @Composable
    private fun PasswordDialog(
        connection: SavedConnection,
        password: String,
        savePassword: Boolean,
        onPasswordChange: (String) -> Unit,
        onSavePasswordChange: (Boolean) -> Unit,
        onDismiss: () -> Unit,
        onConnect: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Enter SSH Password") },
            text = {
                Column {
                    Text(
                        text = "Connecting to ${connection.name}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = savePassword,
                            onCheckedChange = onSavePasswordChange
                        )
                        Text(
                            text = "Remember password (stored securely)",
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onConnect,
                    enabled = password.isNotBlank()
                ) {
                    Text("Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}
