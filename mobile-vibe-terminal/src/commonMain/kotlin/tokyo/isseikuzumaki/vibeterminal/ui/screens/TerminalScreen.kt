package tokyo.isseikuzumaki.vibeterminal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.runtime.remember
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
import tokyo.isseikuzumaki.vibeterminal.domain.model.ConnectionConfig
import tokyo.isseikuzumaki.vibeterminal.domain.repository.SshRepository
import tokyo.isseikuzumaki.vibeterminal.viewmodel.TerminalScreenModel
import tokyo.isseikuzumaki.vibeterminal.ui.components.FileExplorerSheet
import tokyo.isseikuzumaki.vibeterminal.ui.components.CodeViewerSheet
import tokyo.isseikuzumaki.vibeterminal.ui.components.TerminalCanvas
import tokyo.isseikuzumaki.vibeterminal.ui.components.macro.MacroInputPanel

data class TerminalScreen(
    val config: ConnectionConfig
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val sshRepository = koinInject<SshRepository>()
        val apkInstaller = koinInject<tokyo.isseikuzumaki.vibeterminal.domain.installer.ApkInstaller>()
        val screenModel = remember(config) {
            TerminalScreenModel(config, sshRepository, apkInstaller)
        }
        val state by screenModel.state.collectAsState()

        var showFileExplorer by remember { mutableStateOf(false) }
        var selectedFilePath by remember { mutableStateOf<String?>(null) }

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
                    actions = {
                        IconButton(
                            onClick = { showFileExplorer = true },
                            enabled = state.isConnected
                        ) {
                            Icon(
                                Icons.Default.FolderOpen,
                                "File Explorer",
                                tint = if (state.isConnected) Color(0xFF00FF00) else Color.Gray
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1A1A1A),
                        titleContentColor = Color(0xFF00FF00),
                        navigationIconContentColor = Color(0xFF00FF00)
                    )
                )
            },
            floatingActionButton = {
                // Magic Deploy FAB
                if (state.detectedApkPath != null) {
                    FloatingActionButton(
                        onClick = { screenModel.downloadAndInstallApk() },
                        containerColor = Color(0xFF00FF00),
                        contentColor = Color.Black
                    ) {
                        if (state.isDownloadingApk) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.Black
                            )
                        } else {
                            Text("🚀", fontSize = 24.sp)
                        }
                    }
                }
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

                // Terminal Output - Screen Buffer Rendering
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    val textMeasurer = rememberTextMeasurer()
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    
                    val textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                    
                    // Measure character size
                    val sampleLayout = remember(textStyle) {
                        textMeasurer.measure(
                            text = "W",
                            style = textStyle
                        )
                    }
                    val charWidth = sampleLayout.size.width
                    val charHeight = sampleLayout.size.height
                    
                    // Calculate columns and rows
                    val widthPx = with(density) { maxWidth.toPx().toInt() }
                    val heightPx = with(density) { maxHeight.toPx().toInt() }

                    // Subtract 1 from cols to prevent line wrapping issues (% symbols)
                    val cols = ((widthPx / charWidth) - 1).coerceAtLeast(1)
                    val rows = (heightPx / charHeight).coerceAtLeast(1)

                    // Connect to SSH with the measured terminal size
                    // Only trigger once when not yet connected
                    LaunchedEffect(Unit) {
                        if (!state.isConnected && !state.isConnecting && cols > 0 && rows > 0) {
                            screenModel.connect(cols, rows, widthPx, heightPx)
                        }
                    }

                    if (state.isAlternateScreen) {
                        TerminalCanvas(
                            buffer = state.screenBuffer,
                            cursorRow = state.cursorRow,
                            cursorCol = state.cursorCol,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        TerminalBufferView(
                            screenBuffer = state.screenBuffer,
                            cursorRow = state.cursorRow,
                            cursorCol = state.cursorCol,
                            bufferUpdateCounter = state.bufferUpdateCounter
                        )
                    }
                }

                // Buffered Input Deck with Macro Row
                if (state.isConnected) {
                    var inputText by remember { mutableStateOf("") }

                    MacroInputPanel(
                        state = state,
                        inputText = inputText,
                        onInputChange = { inputText = it },
                        onSendCommand = { command ->
                            screenModel.sendCommand(command)
                            inputText = ""
                        },
                        onDirectSend = { sequence ->
                            screenModel.sendCommand(sequence)
                        },
                        onTabSelected = { tab ->
                            screenModel.selectMacroTab(tab)
                        }
                    )
                }
            }
        }

        // File Explorer Sheet
        if (showFileExplorer) {
            FileExplorerSheet(
                sshRepository = sshRepository,
                onDismiss = { showFileExplorer = false },
                onFileSelected = { file ->
                    selectedFilePath = file.path
                }
            )
        }

        // Code Viewer Sheet
        selectedFilePath?.let { filePath ->
            CodeViewerSheet(
                sshRepository = sshRepository,
                filePath = filePath,
                onDismiss = { selectedFilePath = null }
            )
        }
    }

    @Composable
    private fun TerminalBufferView(
        screenBuffer: Array<Array<tokyo.isseikuzumaki.vibeterminal.terminal.TerminalCell>>,
        cursorRow: Int,
        cursorCol: Int,
        bufferUpdateCounter: Int
    ) {
        // Force recomposition when buffer updates
        key(bufferUpdateCounter) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (screenBuffer.isEmpty()) {
                    // Show empty state
                    Text(
                        text = "Initializing terminal...",
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                } else {
                    // Render each row of the terminal
                    screenBuffer.forEachIndexed { rowIndex, row ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            row.forEachIndexed { colIndex, cell ->
                                TerminalCellView(
                                    cell = cell,
                                    isCursor = rowIndex == cursorRow && colIndex == cursorCol
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TerminalCellView(
        cell: tokyo.isseikuzumaki.vibeterminal.terminal.TerminalCell,
        isCursor: Boolean
    ) {
        val backgroundColor = if (isCursor) {
            Color(0xFF00FF00)  // Green cursor
        } else {
            cell.backgroundColor
        }

        val foregroundColor = if (isCursor) {
            Color.Black  // Black text on green cursor
        } else {
            cell.foregroundColor
        }

        Text(
            text = cell.char.toString(),
            color = foregroundColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.background(backgroundColor),
            style = androidx.compose.ui.text.TextStyle(
                fontWeight = if (cell.isBold) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                textDecoration = if (cell.isUnderline) androidx.compose.ui.text.style.TextDecoration.Underline else null
            )
        )
    }
}
