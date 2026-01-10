package tokyo.isseikuzumaki.vibeterminal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import tokyo.isseikuzumaki.vibeterminal.data.datastore.PreferencesHelper
import tokyo.isseikuzumaki.vibeterminal.domain.model.ConnectionConfig
import tokyo.isseikuzumaki.vibeterminal.domain.model.SavedConnection
import tokyo.isseikuzumaki.vibeterminal.security.SshKeyInfoCommon
import tokyo.isseikuzumaki.vibeterminal.viewmodel.ConnectionListScreenModel
import tokyo.isseikuzumaki.vibeterminal.ui.components.StartUpCommandInput
import tokyo.isseikuzumaki.vibeterminal.ui.components.PlatformAdBanner
import tokyo.isseikuzumaki.vibeterminal.terminal.TerminalDisplayManager
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
        val preferencesHelper = koinInject<PreferencesHelper>()
        val coroutineScope = rememberCoroutineScope()

        var showPasswordDialog by remember { mutableStateOf(false) }
        var selectedConnection by remember { mutableStateOf<SavedConnection?>(null) }
        var password by remember { mutableStateOf("") }
        var savePassword by remember { mutableStateOf(false) }
        var showSettingsMenu by remember { mutableStateOf(false) }

        // Auto-install setting
        val autoInstallEnabled by preferencesHelper.autoInstallEnabled.collectAsState(initial = false)

        // External display connection state (informational)
        val isDisplayConnected by TerminalDisplayManager.isDisplayConnected.collectAsState()

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
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    ),
                    actions = {
                        // Display connection indicator (informational only)
                        if (isDisplayConnected) {
                            Text(
                                text = stringResource(Res.string.connection_list_test_display),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        }

                        // Add connection button
                        IconButton(onClick = { screenModel.showAddDialog() }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = stringResource(Res.string.connection_list_add),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Settings menu
                        Box {
                            IconButton(onClick = { showSettingsMenu = true }) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            DropdownMenu(
                                expanded = showSettingsMenu,
                                onDismissRequest = { showSettingsMenu = false }
                            ) {
                                // SSH Keys management
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Settings,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text("SSH Keys")
                                                Text(
                                                    "Manage public key authentication",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        showSettingsMenu = false
                                        navigator.push(KeyManagementScreen())
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(stringResource(Res.string.settings_auto_install))
                                                Text(
                                                    stringResource(Res.string.settings_auto_install_description),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Switch(
                                                checked = autoInstallEnabled,
                                                onCheckedChange = { enabled ->
                                                    coroutineScope.launch {
                                                        preferencesHelper.setAutoInstallEnabled(enabled)
                                                    }
                                                }
                                            )
                                        }
                                    },
                                    onClick = {
                                        coroutineScope.launch {
                                            preferencesHelper.setAutoInstallEnabled(!autoInstallEnabled)
                                        }
                                    }
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                PlatformAdBanner()
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (state.connections.isEmpty()) {
                    // Empty state - show add new connection card
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 300.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            AddConnectionCard(
                                onAdd = { screenModel.showAddDialog() }
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
                                    if (connection.authType == "key" && connection.keyAlias != null) {
                                        // Public key auth: connect directly without password dialog
                                        screenModel.updateLastUsed(connection.id)
                                        navigator.push(
                                            TerminalScreen(
                                                ConnectionConfig(
                                                    host = connection.host,
                                                    port = connection.port,
                                                    username = connection.username,
                                                    password = null,
                                                    keyAlias = connection.keyAlias,
                                                    connectionId = connection.id,
                                                    startupCommand = connection.startupCommand,
                                                    deployPattern = connection.deployPattern,
                                                    monitorFilePath = connection.monitorFilePath
                                                )
                                            )
                                        )
                                    } else {
                                        // Password auth: show password dialog
                                        screenModel.loadPassword(connection.id) { savedPwd ->
                                            password = savedPwd ?: ""
                                            savePassword = savedPwd != null
                                            showPasswordDialog = true
                                        }
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
                availableKeys = state.availableKeys,
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
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onConnect),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left side: Host info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = connection.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${connection.username}@${connection.host}:${connection.port}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Right side: Actions
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    // Connect indicator
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = stringResource(Res.string.action_connect),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Edit and Delete icons
                    Row {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                stringResource(Res.string.action_edit),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                stringResource(Res.string.action_delete),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun AddConnectionCard(
        onAdd: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.connection_list_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.connection_list_empty_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.connection_list_add))
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ConnectionDialog(
        connection: SavedConnection?,
        availableKeys: List<SshKeyInfoCommon>,
        onDismiss: () -> Unit,
        onSave: (SavedConnection) -> Unit
    ) {
        var name by remember { mutableStateOf(connection?.name ?: "") }
        var host by remember { mutableStateOf(connection?.host ?: "") }
        var port by remember { mutableStateOf(connection?.port?.toString() ?: "22") }
        var username by remember { mutableStateOf(connection?.username ?: "") }
        var authType by remember { mutableStateOf(connection?.authType ?: "password") }
        var keyAlias by remember { mutableStateOf(connection?.keyAlias ?: "") }
        var startupCommand by remember { mutableStateOf(connection?.startupCommand ?: "") }
        var deployPattern by remember { mutableStateOf(connection?.deployPattern ?: ">> VIBE_DEPLOY: (.*)") }
        var monitorFilePath by remember { mutableStateOf(connection?.monitorFilePath ?: "") }
        var isAutoReconnect by remember { mutableStateOf(connection?.isAutoReconnect ?: false) }
        var keyDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = stringResource(if (connection == null) Res.string.connection_dialog_add_title else Res.string.connection_dialog_edit_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
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

                    // Authentication Type Selection
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Authentication",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = authType == "password",
                                onClick = { authType = "password" },
                                label = { Text("Password") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            FilterChip(
                                selected = authType == "key",
                                onClick = { authType = "key" },
                                label = { Text("Public Key") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    // Key Selection (shown when authType is "key")
                    if (authType == "key") {
                        if (availableKeys.isEmpty()) {
                            // No keys available
                            Text(
                                text = "No SSH keys available. Create keys in Settings > SSH Keys.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            // Key dropdown
                            ExposedDropdownMenuBox(
                                expanded = keyDropdownExpanded,
                                onExpandedChange = { keyDropdownExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = if (keyAlias.isNotBlank()) {
                                        val keyInfo = availableKeys.find { it.alias == keyAlias }
                                        if (keyInfo != null) "${keyInfo.alias} (${keyInfo.algorithm})" else keyAlias
                                    } else "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("SSH Key") },
                                    placeholder = { Text("Select a key") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = keyDropdownExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                )
                                ExposedDropdownMenu(
                                    expanded = keyDropdownExpanded,
                                    onDismissRequest = { keyDropdownExpanded = false }
                                ) {
                                    availableKeys.forEach { key ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(key.alias)
                                                    Text(
                                                        text = key.algorithm,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            onClick = {
                                                keyAlias = key.alias
                                                keyDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

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
                            authType = authType,
                            keyAlias = if (authType == "key" && keyAlias.isNotBlank()) keyAlias else null,
                            createdAt = connection?.createdAt ?: System.currentTimeMillis(),
                            lastUsedAt = connection?.lastUsedAt,
                            deployPattern = deployPattern.ifBlank { null },
                            startupCommand = startupCommand.ifBlank { null },
                            isAutoReconnect = isAutoReconnect,
                            monitorFilePath = monitorFilePath.ifBlank { null }
                        )
                        onSave(savedConnection)
                    },
                    enabled = name.isNotBlank() && host.isNotBlank() && username.isNotBlank() &&
                              (authType == "password" || (authType == "key" && keyAlias.isNotBlank())),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(Res.string.action_save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
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
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = stringResource(Res.string.password_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.password_dialog_connecting_to, connection.name),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = { Text(stringResource(Res.string.password_dialog_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = savePassword,
                            onCheckedChange = onSavePasswordChange,
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                checkmarkColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        Text(
                            text = stringResource(Res.string.password_dialog_remember),
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onConnect,
                    enabled = password.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(Res.string.action_connect))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }
        )
    }
}
