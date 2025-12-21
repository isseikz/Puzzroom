package tokyo.isseikuzumaki.vibeterminal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import tokyo.isseikuzumaki.vibeterminal.domain.model.ConnectionConfig
import tokyo.isseikuzumaki.vibeterminal.domain.repository.SshRepository
import tokyo.isseikuzumaki.vibeterminal.viewmodel.TerminalScreenModel
import tokyo.isseikuzumaki.vibeterminal.ui.utils.AnsiParser

data class TerminalScreen(
    val config: ConnectionConfig
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val sshRepository = koinInject<SshRepository>()
        val screenModel = remember(config) {
            TerminalScreenModel(config, sshRepository)
        }
        val state by screenModel.state.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "${config.username}@${config.host}:${config.port}",
                            fontSize = 14.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            screenModel.disconnect()
                            navigator.pop()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1A1A1A),
                        titleContentColor = Color(0xFF00FF00),
                        navigationIconContentColor = Color(0xFF00FF00)
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color.Black)
            ) {
                // Connection Status
                if (state.isConnecting) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF00FF00)
                    )
                }

                // Error Message
                state.errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = Color.Red,
                        modifier = Modifier.padding(8.dp),
                        fontSize = 12.sp
                    )
                }

                // Terminal Output
                val listState = rememberLazyListState()
                val coroutineScope = rememberCoroutineScope()

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    items(state.logLines) { line ->
                        Text(
                            text = AnsiParser.parse(line),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Auto-scroll to bottom when new lines arrive
                LaunchedEffect(state.logLines.size) {
                    if (state.logLines.isNotEmpty()) {
                        coroutineScope.launch {
                            listState.animateScrollToItem(state.logLines.size - 1)
                        }
                    }
                }

                // Buffered Input Deck with Macro Row
                if (state.isConnected) {
                    var inputText by remember { mutableStateOf("") }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1A1A1A))
                    ) {
                        // Macro Row - Common terminal keys
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            MacroButton("ESC") { screenModel.sendCommand("\u001B") }
                            MacroButton("TAB") { screenModel.sendCommand("\t") }
                            MacroButton("CTRL+C") { screenModel.sendCommand("\u0003") }
                            MacroButton("CTRL+D") { screenModel.sendCommand("\u0004") }
                            MacroButton("CTRL+Z") { screenModel.sendCommand("\u001A") }
                            MacroButton("|") { inputText += "|" }
                            MacroButton("->") { inputText += " -> " }
                            MacroButton("&&") { inputText += " && " }
                            MacroButton("||") { inputText += " || " }
                            MacroButton("~/") { inputText += "~/" }
                            MacroButton("../") { inputText += "../" }
                        }

                        HorizontalDivider(color = Color(0xFF00FF00).copy(alpha = 0.3f), thickness = 1.dp)

                        // Buffered Input Field
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = { Text("Enter command...", color = Color.Gray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF00FF00),
                                    unfocusedTextColor = Color(0xFF00FF00),
                                    focusedContainerColor = Color.Black,
                                    unfocusedContainerColor = Color.Black,
                                    cursorColor = Color(0xFF00FF00),
                                    focusedBorderColor = Color(0xFF00FF00),
                                    unfocusedBorderColor = Color(0xFF00FF00).copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.weight(1f),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp
                                )
                            )

                            Button(
                                onClick = {
                                    screenModel.sendCommand(inputText)
                                    inputText = ""
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00FF00),
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text("Send", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun MacroButton(label: String, onClick: () -> Unit) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2A2A2A),
                contentColor = Color(0xFF00FF00)
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
