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

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction

import tokyo.isseikuzumaki.vibeterminal.viewmodel.InputMode
import androidx.compose.foundation.clickable
import tokyo.isseikuzumaki.vibeterminal.terminal.TerminalStateProvider
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import androidx.compose.ui.text.style.TextAlign
import tokyo.isseikuzumaki.vibeterminal.util.Logger

data class TerminalScreen(
    val config: ConnectionConfig
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val sshRepository = koinInject<SshRepository>()
        val apkInstaller = koinInject<tokyo.isseikuzumaki.vibeterminal.domain.installer.ApkInstaller>()
        val connectionRepository = koinInject<tokyo.isseikuzumaki.vibeterminal.domain.repository.ConnectionRepository>()
        val screenModel = remember(config) {
            TerminalScreenModel(config, sshRepository, apkInstaller, connectionRepository)
        }
        val state by screenModel.state.collectAsState()

        // **New**: セカンダリディスプレイの接続状態を監視
        val isSecondaryConnected by TerminalStateProvider.isSecondaryDisplayConnected.collectAsState()
        val secondaryMetrics by TerminalStateProvider.secondaryDisplayMetrics.collectAsState()

        var showFileExplorer by remember { mutableStateOf(false) }
        var selectedFilePath by remember { mutableStateOf<String?>(null) }

        val focusRequester = remember { FocusRequester() }
        var cmdInput by remember { mutableStateOf(TextFieldValue(" ")) }

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
                            screenModel.disconnectAndClearSession {
                                navigator.pop()
                            }
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

                // Hidden Command Input (captures software keyboard in CMD mode)
                if (state.isConnected && state.inputMode == InputMode.COMMAND) {
                    BasicTextField(
                        value = cmdInput,
                        onValueChange = { newValue ->
                            if (newValue.text.length > 1) {
                                // Character typed
                                val newChars = newValue.text.substring(1)
                                screenModel.sendInput(newChars, appendNewline = false)
                                cmdInput = TextFieldValue(" ")
                            } else if (newValue.text.isEmpty()) {
                                // Backspace
                                screenModel.sendInput("\u007F", appendNewline = false)
                                cmdInput = TextFieldValue(" ")
                            } else {
                                cmdInput = newValue
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            autoCorrect = false,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                screenModel.sendInput("\r", appendNewline = false)
                                cmdInput = TextFieldValue(" ")
                            }
                        ),
                        modifier = Modifier
                            .size(1.dp)
                            .focusRequester(focusRequester)
                    )

                    LaunchedEffect(state.inputMode) {
                        focusRequester.requestFocus()
                    }
                }

                // Terminal Output - Screen Buffer Rendering
                // **New**: セカンダリディスプレイ接続中はプレースホルダーを表示
                if (isSecondaryConnected) {
                    // セカンダリディスプレイで表示中のメッセージ
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "セカンダリディスプレイで表示中\n\nSecondary display is active",
                            color = Color(0xFF00FF00),
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    // メインディスプレイでターミナル表示
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable(
                                enabled = state.inputMode == InputMode.COMMAND,
                                onClick = { focusRequester.requestFocus() }
                            )
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

                    // **New**: セカンダリディスプレイ切断時にメインディスプレイでリサイズ
                    var previousSecondaryState by remember { mutableStateOf(isSecondaryConnected) }

                    LaunchedEffect(isSecondaryConnected, cols, rows) {
                        if (previousSecondaryState && !isSecondaryConnected) {
                            // 接続 → 切断 へ遷移
                            if (state.isConnected && cols > 0 && rows > 0) {
                                Logger.d("Secondary display disconnected, resizing to main display: ${cols}x${rows}")
                                screenModel.resizeTerminal(cols, rows, widthPx, heightPx)
                            }
                        }
                        previousSecondaryState = isSecondaryConnected
                    }

                    // Connect to SSH with the measured terminal size
                    // Only trigger once when not yet connected
                    // **New**: セカンダリディスプレイ優先のSSH接続
                    var hasConnected by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        if (hasConnected) return@LaunchedEffect

                        // セカンダリディスプレイのサイズ取得を試みる (1秒タイムアウト)
                        val secondarySize = withTimeoutOrNull(1000L) {
                            TerminalStateProvider.secondaryDisplayMetrics
                                .filterNotNull()
                                .first()
                        }

                        if (!state.isConnected && !state.isConnecting) {
                            if (secondarySize != null) {
                                // セカンダリディスプレイのサイズで接続
                                Logger.d("Connecting with secondary display size: ${secondarySize.cols}x${secondarySize.rows}")
                                screenModel.connect(
                                    secondarySize.cols,
                                    secondarySize.rows,
                                    secondarySize.widthPx,
                                    secondarySize.heightPx
                                )
                            } else if (cols > 0 && rows > 0) {
                                // メインディスプレイのサイズで接続
                                Logger.d("Connecting with main display size: ${cols}x${rows}")
                                screenModel.connect(cols, rows, widthPx, heightPx)
                            }
                            hasConnected = true
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
                }

                // Buffered Input Deck with Macro Row
                // **New**: セカンダリディスプレイ接続中は非表示
                if (state.isConnected && !isSecondaryConnected) {
                    var inputText by remember { mutableStateOf(TextFieldValue("")) }

                    MacroInputPanel(
                        state = state,
                        inputText = inputText,
                        onInputChange = { inputText = it },
                        onSendCommand = { command ->
                            screenModel.sendInput(command, appendNewline = true)
                            inputText = TextFieldValue("")
                        },
                        onDirectSend = { sequence ->
                            screenModel.sendInput(sequence, appendNewline = false)
                        },
                        onTabSelected = { tab ->
                            screenModel.selectMacroTab(tab)
                        },
                        onToggleInputMode = {
                            screenModel.toggleInputMode()
                        },
                        onToggleCtrl = {
                            screenModel.toggleCtrl()
                        },
                        onToggleAlt = {
                            screenModel.toggleAlt()
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
