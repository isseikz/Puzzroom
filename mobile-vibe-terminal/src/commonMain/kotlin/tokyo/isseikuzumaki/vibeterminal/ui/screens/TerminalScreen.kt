package tokyo.isseikuzumaki.vibeterminal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Keyboard
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

import io.github.isseikz.kmpinput.TerminalInputContainer
import io.github.isseikz.kmpinput.rememberTerminalInputContainerState
import io.github.isseikz.kmpinput.InputMode as KmpInputMode

import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.platform.LocalDensity

import tokyo.isseikuzumaki.vibeterminal.viewmodel.InputMode
import tokyo.isseikuzumaki.vibeterminal.terminal.DisplayTarget
import tokyo.isseikuzumaki.vibeterminal.terminal.TerminalDisplayManager
import tokyo.isseikuzumaki.vibeterminal.terminal.TerminalStateProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import tokyo.isseikuzumaki.vibeterminal.util.Logger
import androidx.lifecycle.compose.LifecycleResumeEffect
import org.jetbrains.compose.resources.stringResource
import puzzroom.mobile_vibe_terminal.generated.resources.*
import tokyo.isseikuzumaki.vibeterminal.terminal.TerminalFontConfig

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

        // Terminal Input Library State
        val terminalInputState = rememberTerminalInputContainerState()

        // Sync Input Mode (App -> Library)
        LaunchedEffect(state.inputMode) {
            val mode = if (state.inputMode == InputMode.TEXT) KmpInputMode.TEXT else KmpInputMode.RAW
            terminalInputState.setInputMode(mode)
        }

        // ターミナル表示先を監視 (TerminalDisplayManager の derived state)
        val displayTarget by TerminalDisplayManager.terminalDisplayTarget.collectAsState()
        val isSecondaryConnected = displayTarget == DisplayTarget.SECONDARY

        // Collect Input from Library (only when connected)
        // Note: handlerVersion ensures restart when handler changes (e.g., switching display modes)
        LaunchedEffect(terminalInputState.isReady, state.isConnected, terminalInputState.handlerVersion) {
            Logger.d("TerminalScreen: LaunchedEffect - isReady=${terminalInputState.isReady}, isConnected=${state.isConnected}, handlerVersion=${terminalInputState.handlerVersion}")
            if (terminalInputState.isReady && state.isConnected) {
                Logger.d("TerminalScreen: Starting to collect from ptyInputStream (handlerVersion=${terminalInputState.handlerVersion})")
                terminalInputState.ptyInputStream.collect { bytes ->
                    val text = bytes.decodeToString()
                    Logger.d("TerminalScreen: Received input from ptyInputStream: '$text' (${bytes.size} bytes)")
                    screenModel.sendInput(text, appendNewline = false)
                }
            }
        }

        val secondaryMetrics by TerminalStateProvider.secondaryDisplayMetrics.collectAsState()

        // SSH Connection Logic - Independent of display mode
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
                    Logger.d("Connecting with secondary display size: ${secondarySize.cols}x${secondarySize.rows}")
                    screenModel.connect(
                        secondarySize.cols,
                        secondarySize.rows,
                        secondarySize.widthPx,
                        secondarySize.heightPx
                    )
                } else {
                    // Use default terminal size for initial connection
                    // Will be resized when main display UI is measured
                    Logger.d("Connecting with default size: 80x24")
                    screenModel.connect(80, 24, 800, 600)
                }
                hasConnected = true
            }
        }

        var showFileExplorer by remember { mutableStateOf(false) }
        var selectedFilePath by remember { mutableStateOf<String?>(null) }
        var fileExplorerInitialPath by remember { mutableStateOf<String?>(null) }
        var isLoadingInitialPath by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()

        // Lifecycle observer: Check connection state when app resumes from background
        LifecycleResumeEffect(Unit) {
            Logger.d("TerminalScreen: Lifecycle RESUMED")
            screenModel.onAppResumed()
            onPauseOrDispose {
                Logger.d("TerminalScreen: Lifecycle PAUSED or DISPOSED")
            }
        }

        Scaffold(
            floatingActionButton = {
                // Show FAB with TerminalInputContainer only in secondary display mode
                if (isSecondaryConnected && state.isConnected) {
                    TerminalInputContainer(
                        state = terminalInputState,
                        modifier = Modifier.size(56.dp)
                    ) {
                        FloatingActionButton(
                            onClick = { /* TerminalInputContainer handles tap */ },
                            containerColor = Color(0xFF00FF00),
                            contentColor = Color.Black
                        ) {
                            Icon(
                                Icons.Default.Keyboard,
                                contentDescription = "Keyboard"
                            )
                        }
                    }
                }
            },
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.action_back))
                        }
                    },
                    actions = {
                        // File Explorer Button
                        IconButton(
                            onClick = {
                                isLoadingInitialPath = true
                                coroutineScope.launch {
                                    // Try to load saved path from database
                                    val savedPath = screenModel.loadLastFileExplorerPath()
                                    if (savedPath != null) {
                                        // Use saved path (persisted across app restarts)
                                        fileExplorerInitialPath = savedPath
                                    } else {
                                        // First time: Get home directory from SSH
                                        val homeDir = screenModel.getRemoteHomeDirectory()
                                        fileExplorerInitialPath = homeDir
                                    }
                                    isLoadingInitialPath = false
                                    showFileExplorer = true
                                }
                            },
                            enabled = state.isConnected && !isLoadingInitialPath
                        ) {
                            if (isLoadingInitialPath) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFF00FF00),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.FolderOpen,
                                    stringResource(Res.string.terminal_file_explorer),
                                    tint = if (state.isConnected) Color(0xFF00FF00) else Color.Gray
                                )
                            }
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
            // IME (keyboard) offset calculation for sliding UI upward when keyboard appears
            val density = LocalDensity.current
            val imeInsets = WindowInsets.ime
            val imeBottomPx = remember { derivedStateOf { imeInsets.getBottom(density) } }
            val imeOffsetDp by animateDpAsState(
                targetValue = if (imeBottomPx.value > 0) {
                    -with(density) { imeBottomPx.value.toDp() }
                } else {
                    0.dp
                },
                label = "imeOffset"
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .offset(y = imeOffsetDp)
                    .background(Color.Black)
            ) {
                // Connection Status
                if (state.isConnecting) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF00FF00)
                    )
                }

                // Reconnecting Status
                if (state.isReconnecting) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2D2D00))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFFFFAA00),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(Res.string.terminal_reconnecting),
                            color = Color(0xFFFFAA00),
                            fontSize = 12.sp
                        )
                    }
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

                if (isSecondaryConnected) {
                    // Secondary Display Mode: Show Macro Panel Full Screen
                    // Terminal output is shown on secondary display
                    // Input is captured by TerminalPresentation on secondary display
                    MacroInputPanel(
                        state = state,
                        inputText = TextFieldValue(""),
                        onInputChange = { },
                        onSendCommand = { },
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
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Main Display Mode: Standard Layout
                    TerminalInputContainer(
                        state = terminalInputState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        // Main Display Mode: Show Terminal
                        BoxWithConstraints(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val textMeasurer = rememberTextMeasurer()
                            val innerDensity = androidx.compose.ui.platform.LocalDensity.current

                            val textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = TerminalFontConfig.fontFamily,
                                fontSize = TerminalFontConfig.fontSize
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

                            // Calculate columns and rows based on available space
                            val maxHeightPx = with(innerDensity) { maxHeight.toPx().toInt() }
                            val maxWidthPx = with(innerDensity) { maxWidth.toPx().toInt() }

                            val terminalSize = remember(maxWidthPx, maxHeightPx) {
                                val cols = ((maxWidthPx / charWidth) - 1).coerceAtLeast(1)
                                val rows = (maxHeightPx / charHeight).coerceAtLeast(1)

                                object {
                                    val cols = cols
                                    val rows = rows
                                    val widthPx = maxWidthPx
                                    val heightPx = maxHeightPx
                                }
                            }

                            // Track previous terminal size to detect changes
                            var previousTerminalCols by remember { mutableStateOf(0) }
                            var previousTerminalRows by remember { mutableStateOf(0) }

                            // Resize terminal when available space changes
                            LaunchedEffect(terminalSize.cols, terminalSize.rows, state.isConnected) {
                                if (state.isConnected &&
                                    terminalSize.cols > 0 && terminalSize.rows > 0 &&
                                    (terminalSize.cols != previousTerminalCols || terminalSize.rows != previousTerminalRows) &&
                                    previousTerminalCols > 0 && previousTerminalRows > 0
                                ) {
                                    Logger.d("Terminal size changed: ${previousTerminalCols}x${previousTerminalRows} -> ${terminalSize.cols}x${terminalSize.rows}")
                                    screenModel.resizeTerminal(
                                        terminalSize.cols,
                                        terminalSize.rows,
                                        terminalSize.widthPx,
                                        terminalSize.heightPx,
                                        DisplayTarget.MAIN
                                    )
                                }
                                previousTerminalCols = terminalSize.cols
                                previousTerminalRows = terminalSize.rows
                            }

                            // セカンダリディスプレイ切断時にメインディスプレイでリサイズ
                            var previousSecondaryState by remember { mutableStateOf(isSecondaryConnected) }

                            LaunchedEffect(isSecondaryConnected, terminalSize.cols, terminalSize.rows) {
                                if (previousSecondaryState && !isSecondaryConnected) {
                                    if (state.isConnected && terminalSize.cols > 0 && terminalSize.rows > 0) {
                                        Logger.d("Secondary display disconnected, resizing to main display: ${terminalSize.cols}x${terminalSize.rows}")
                                        screenModel.resizeTerminal(
                                            terminalSize.cols,
                                            terminalSize.rows,
                                            terminalSize.widthPx,
                                            terminalSize.heightPx,
                                            DisplayTarget.MAIN
                                        )
                                    }
                                }
                                previousSecondaryState = isSecondaryConnected
                            }

                            // Resize terminal to actual measured size after connection
                            // (initial connection uses default 80x24, resize to actual size when UI is measured)
                            LaunchedEffect(state.isConnected, terminalSize.cols, terminalSize.rows) {
                                if (state.isConnected && terminalSize.cols > 0 && terminalSize.rows > 0 && !isSecondaryConnected) {
                                    val currentCols = state.screenBuffer.firstOrNull()?.size ?: 0
                                    val currentRows = state.screenBuffer.size
                                    if (currentCols != terminalSize.cols || currentRows != terminalSize.rows) {
                                        Logger.d("Resizing terminal to measured size: ${terminalSize.cols}x${terminalSize.rows}")
                                        screenModel.resizeTerminal(
                                            terminalSize.cols,
                                            terminalSize.rows,
                                            terminalSize.widthPx,
                                            terminalSize.heightPx,
                                            DisplayTarget.MAIN
                                        )
                                    }
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

                    // Buffered Input Deck with Macro Row (Main Display Mode Only)
                    if (state.isConnected) {
                        MacroInputPanel(
                            state = state,
                            inputText = TextFieldValue(""),
                            onInputChange = { },
                            onSendCommand = { },
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
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // File Explorer Sheet
        if (showFileExplorer && fileExplorerInitialPath != null) {
            FileExplorerSheet(
                sshRepository = sshRepository,
                initialPath = fileExplorerInitialPath!!,
                onDismiss = { showFileExplorer = false },
                onFileSelected = { file ->
                    selectedFilePath = file.path
                },
                onInstall = { file ->
                    screenModel.downloadAndInstallApk(file.path)
                },
                onPathChanged = { path ->
                    screenModel.updateLastFileExplorerPath(path)
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
                    Text(
                        text = stringResource(Res.string.terminal_initializing),
                        color = Color.Gray,
                        fontFamily = TerminalFontConfig.fontFamily,
                        fontSize = TerminalFontConfig.fontSize
                    )
                } else {
                    screenBuffer.forEachIndexed { rowIndex, row ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            row.forEachIndexed { colIndex, cell ->
                                // Skip padding cells - wide chars already occupy 2 cell widths
                                if (!cell.isWideCharPadding) {
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
    }

    @Composable
    private fun TerminalCellView(
        cell: tokyo.isseikuzumaki.vibeterminal.terminal.TerminalCell,
        isCursor: Boolean
    ) {
        val backgroundColor = if (isCursor) {
            Color(0xFF00FF00)
        } else {
            cell.backgroundColor
        }

        val foregroundColor = if (isCursor) {
            Color.Black
        } else {
            cell.foregroundColor
        }

        Text(
            text = cell.char.toString(),
            color = foregroundColor,
            fontFamily = TerminalFontConfig.fontFamily,
            fontSize = TerminalFontConfig.fontSize,
            modifier = Modifier.background(backgroundColor),
            style = androidx.compose.ui.text.TextStyle(
                fontWeight = if (cell.isBold) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                textDecoration = if (cell.isUnderline) androidx.compose.ui.text.style.TextDecoration.Underline else null
            )
        )
    }
}
