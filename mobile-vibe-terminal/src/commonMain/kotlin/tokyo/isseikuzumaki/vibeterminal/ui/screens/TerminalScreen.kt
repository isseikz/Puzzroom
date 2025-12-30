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

import io.github.isseikz.kmpinput.TerminalInputContainer
import io.github.isseikz.kmpinput.rememberTerminalInputContainerState
import io.github.isseikz.kmpinput.InputMode as KmpInputMode

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.platform.LocalDensity

import tokyo.isseikuzumaki.vibeterminal.viewmodel.InputMode
import androidx.compose.foundation.clickable
import tokyo.isseikuzumaki.vibeterminal.terminal.TerminalStateProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import androidx.compose.ui.text.style.TextAlign
import tokyo.isseikuzumaki.vibeterminal.util.Logger
import androidx.lifecycle.compose.LifecycleResumeEffect
import org.jetbrains.compose.resources.stringResource
import puzzroom.mobile_vibe_terminal.generated.resources.*

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

        // Collect Input from Library (only when connected)
        LaunchedEffect(terminalInputState.isReady, state.isConnected) {
            if (terminalInputState.isReady && state.isConnected) {
                terminalInputState.ptyInputStream.collect { bytes ->
                    val text = bytes.decodeToString()
                    screenModel.sendInput(text, appendNewline = false)
                }
            }
        }

        // **New**: セカンダリディスプレイの接続状態を監視
        val isSecondaryConnected by TerminalStateProvider.isSecondaryDisplayConnected.collectAsState()
        val secondaryMetrics by TerminalStateProvider.secondaryDisplayMetrics.collectAsState()

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

                // Terminal Input Container
                TerminalInputContainer(
                    state = terminalInputState,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    // Terminal Output - Screen Buffer Rendering
                    // **New**: セカンダリディスプレイ接続中はステータスメッセージを表示
                    if (isSecondaryConnected) {
                        // セカンダリディスプレイで表示中のステータス
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Text(
                                text = stringResource(Res.string.terminal_secondary_display),
                                color = Color(0xFF00FF00),
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        // メインディスプレイでターミナル表示
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
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

                        // Calculate columns and rows based on available space
                        // Use maxHeight as key to recalculate when layout changes (e.g., MacroInputPanel appears)
                        val maxHeightPx = with(density) { maxHeight.toPx().toInt() }
                        val maxWidthPx = with(density) { maxWidth.toPx().toInt() }

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

                        // Resize terminal when available space changes (e.g., MacroInputPanel appears)
                        LaunchedEffect(terminalSize.cols, terminalSize.rows, state.isConnected) {
                            if (state.isConnected &&
                                terminalSize.cols > 0 && terminalSize.rows > 0 &&
                                (terminalSize.cols != previousTerminalCols || terminalSize.rows != previousTerminalRows) &&
                                previousTerminalCols > 0 && previousTerminalRows > 0) {
                                // Size changed after initial connection - trigger resize
                                Logger.d("Terminal size changed: ${previousTerminalCols}x${previousTerminalRows} -> ${terminalSize.cols}x${terminalSize.rows}")
                                screenModel.resizeTerminal(terminalSize.cols, terminalSize.rows, terminalSize.widthPx, terminalSize.heightPx)
                            }
                            previousTerminalCols = terminalSize.cols
                            previousTerminalRows = terminalSize.rows
                        }

                        // **New**: セカンダリディスプレイ切断時にメインディスプレイでリサイズ
                        var previousSecondaryState by remember { mutableStateOf(isSecondaryConnected) }

                        LaunchedEffect(isSecondaryConnected, terminalSize.cols, terminalSize.rows) {
                            if (previousSecondaryState && !isSecondaryConnected) {
                                // 接続 → 切断 へ遷移
                                if (state.isConnected && terminalSize.cols > 0 && terminalSize.rows > 0) {
                                    Logger.d("Secondary display disconnected, resizing to main display: ${terminalSize.cols}x${terminalSize.rows}")
                                    screenModel.resizeTerminal(terminalSize.cols, terminalSize.rows, terminalSize.widthPx, terminalSize.heightPx)
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
                                } else if (terminalSize.cols > 0 && terminalSize.rows > 0) {
                                    // メインディスプレイのサイズで接続
                                    Logger.d("Connecting with main display size: ${terminalSize.cols}x${terminalSize.rows}")
                                    screenModel.connect(terminalSize.cols, terminalSize.rows, terminalSize.widthPx, terminalSize.heightPx)
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
                }

                // Buffered Input Deck with Macro Row
                // Show input panel when connected (visible on main display even with secondary display)
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
                        }
                    )
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
                modifier = Modifier.fillMaxWidth()
            ) {
                if (screenBuffer.isEmpty()) {
                    // Show empty state
                    Text(
                        text = stringResource(Res.string.terminal_initializing),
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
