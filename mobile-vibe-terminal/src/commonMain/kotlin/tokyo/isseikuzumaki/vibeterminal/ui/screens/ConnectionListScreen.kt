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
import tokyo.isseikuzumaki.vibeterminal.platform.launchSecondaryDisplay
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import puzzroom.mobile_vibe_terminal.generated.resources.*

class ConnectionListScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<ConnectionListScreenModel>()
        val state by screenModel.state.collectAsState()
        val context = LocalContext.current

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
                    title = { Text(stringResource(Res.string.connection_list_title)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1A1A1A),
                        titleContentColor = Color(0xFF00FF00)
                    ),
                    actions = {
                        // Secondary Display Test Button (Android only)
                        Button(
                            onClick = {
                                try {
                                    launchSecondaryDisplay(context)
                                } catch (e: Exception) {
                                    tokyo.isseikuzumaki.vibeterminal.util.Logger.e(e, "Failed to launch secondary display")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(stringResource(Res.string.connection_list_test_display), fontSize = 12.sp)
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { screenModel.showAddDialog() },
                    containerColor = Color(0xFF00FF00),
                    contentColor = Color.Black
                ) {
                    Icon(Icons.Default.Add, stringResource(Res.string.connection_list_add))
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
                                stringResource(Res.string.connection_list_empty_title),
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(Res.string.connection_list_empty_message),
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
                                startupCommand = selectedConnection!!.startupCommand,
                                deployPattern = selectedConnection!!.deployPattern,
                                monitorFilePath = selectedConnection!!.monitorFilePath
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
                        Text(stringResource(Res.string.action_dismiss))
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
                        Text(stringResource(Res.string.action_connect))
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, stringResource(Res.string.action_edit), tint = Color(0xFF00FF00))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, stringResource(Res.string.action_delete), tint = Color.Red)
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
        var monitorFilePath by remember { mutableStateOf(connection?.monitorFilePath ?: "") }
        var isAutoReconnect by remember { mutableStateOf(connection?.isAutoReconnect ?: false) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(if (connection == null) Res.string.connection_dialog_add_title else Res.string.connection_dialog_edit_title)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(Res.string.connection_dialog_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text(stringResource(Res.string.connection_dialog_host)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text(stringResource(Res.string.connection_dialog_port)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(stringResource(Res.string.connection_dialog_username)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Startup Command Input
                    StartUpCommandInput(
                        command = startupCommand,
                        onCommandChange = { startupCommand = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Monitor File Path
                    OutlinedTextField(
                        value = monitorFilePath,
                        onValueChange = { monitorFilePath = it },
                        label = { Text(stringResource(Res.string.connection_dialog_monitor_path)) },
                        placeholder = { Text(stringResource(Res.string.connection_dialog_monitor_path_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Text(
                                stringResource(Res.string.connection_dialog_monitor_path_description),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )

                    // Magic Deploy Pattern
                    OutlinedTextField(
                        value = deployPattern,
                        onValueChange = { deployPattern = it },
                        label = { Text(stringResource(Res.string.connection_dialog_deploy_pattern)) },
                        placeholder = { Text(stringResource(Res.string.connection_dialog_deploy_pattern_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Text(
                                stringResource(Res.string.connection_dialog_deploy_pattern_description),
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
                            text = stringResource(Res.string.connection_dialog_auto_reconnect),
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
                            isAutoReconnect = isAutoReconnect,
                            monitorFilePath = monitorFilePath.ifBlank { null }
                        )
                        onSave(savedConnection)
                    },
                    enabled = name.isNotBlank() && host.isNotBlank() && username.isNotBlank()
                ) {
                    Text(stringResource(Res.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.action_cancel))
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
            title = { Text(stringResource(Res.string.password_dialog_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(Res.string.password_dialog_connecting_to, connection.name),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = { Text(stringResource(Res.string.password_dialog_password)) },
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
                            text = stringResource(Res.string.password_dialog_remember),
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
                    Text(stringResource(Res.string.action_connect))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }
        )
    }
}
