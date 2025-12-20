package tokyo.isseikuzumaki.vibeterminal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import tokyo.isseikuzumaki.vibeterminal.domain.model.ConnectionConfig
import tokyo.isseikuzumaki.vibeterminal.viewmodel.ConnectionListScreenModel

class ConnectionListScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<ConnectionListScreenModel>()

        var showPasswordDialog by remember { mutableStateOf(false) }
        var password by remember { mutableStateOf("") }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Vibe Terminal") }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Phase 1: SSH Terminal Test",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        password = ""
                        showPasswordDialog = true
                    }
                ) {
                    Text("Connect to Server")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Server: 192.168.2.10:20828\nUsername: isseikz",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Password Dialog
        if (showPasswordDialog) {
            AlertDialog(
                onDismissRequest = { showPasswordDialog = false },
                title = { Text("Enter SSH Password") },
                text = {
                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            println("=== Connect button clicked ===")
                            showPasswordDialog = false
                            try {
                                println("Creating ConnectionConfig...")
                                val config = ConnectionConfig(
                                    host = "192.168.2.10",
                                    port = 20828,
                                    username = "isseikz",
                                    password = password
                                )
                                println("Config created: $config")
                                println("Pushing TerminalScreen...")
                                navigator.push(TerminalScreen(config))
                                println("TerminalScreen pushed successfully")
                            } catch (e: Exception) {
                                println("=== ERROR in navigation ===")
                                println("Error: ${e.message}")
                                e.printStackTrace()
                            }
                        }
                    ) {
                        Text("Connect")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPasswordDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
