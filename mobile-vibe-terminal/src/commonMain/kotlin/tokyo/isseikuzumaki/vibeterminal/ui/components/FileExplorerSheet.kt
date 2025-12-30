package tokyo.isseikuzumaki.vibeterminal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import tokyo.isseikuzumaki.vibeterminal.domain.model.FileEntry
import tokyo.isseikuzumaki.vibeterminal.domain.repository.SshRepository
import kotlin.math.log10
import kotlin.math.pow
import org.jetbrains.compose.resources.stringResource
import puzzroom.mobile_vibe_terminal.generated.resources.*

data class FileExplorerState(
    val currentPath: String = "/",
    val files: List<FileEntry> = emptyList(),
    val breadcrumbs: List<String> = listOf("/"),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerSheet(
    sshRepository: SshRepository,
    initialPath: String = "/",
    onDismiss: () -> Unit,
    onFileSelected: (FileEntry) -> Unit,
    onInstall: (FileEntry) -> Unit,
    onPathChanged: (String) -> Unit = {}
) {
    var state by remember { mutableStateOf(FileExplorerState(currentPath = initialPath)) }
    val scope = rememberCoroutineScope()

    fun generateBreadcrumbs(path: String): List<String> {
        if (path == "/") return listOf("/")

        val parts = path.trim('/').split("/")
        val breadcrumbs = mutableListOf("/")

        var currentPath = ""
        for (part in parts) {
            currentPath += "/$part"
            breadcrumbs.add(currentPath)
        }

        return breadcrumbs
    }

    fun loadDirectory(path: String) {
        scope.launch {
            state = state.copy(isLoading = true, errorMessage = null)

            val result = sshRepository.listFiles(path)

            result.fold(
                onSuccess = { files ->
                    val breadcrumbs = generateBreadcrumbs(path)
                    state = state.copy(
                        currentPath = path,
                        files = files,
                        breadcrumbs = breadcrumbs,
                        isLoading = false,
                        errorMessage = null
                    )
                    // Notify parent of path change for persistence
                    onPathChanged(path)
                },
                onFailure = { error ->
                    state = state.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load directory"
                    )
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        loadDirectory(initialPath)
    }

    fun navigateUp() {
        val currentPath = state.currentPath
        if (currentPath == "/") return

        val parentPath = currentPath.substringBeforeLast("/").ifEmpty { "/" }
        loadDirectory(parentPath)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0D1117),
        contentColor = Color.White,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header with back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navigateUp() },
                    enabled = state.currentPath != "/"
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        stringResource(Res.string.action_back),
                        tint = if (state.currentPath != "/") Color(0xFF39D353) else Color.Gray
                    )
                }
                Text(
                    stringResource(Res.string.file_explorer_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF39D353)
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            // Breadcrumbs
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(state.breadcrumbs) { breadcrumb ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val rootLabel = stringResource(Res.string.file_explorer_root)
                        Text(
                            text = if (breadcrumb == "/") rootLabel else breadcrumb.substringAfterLast("/"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (breadcrumb == state.currentPath) {
                                Color(0xFF39D353)
                            } else {
                                Color(0xFF58A6FF)
                            },
                            modifier = Modifier
                                .clickable {
                                    if (breadcrumb != state.currentPath) {
                                        loadDirectory(breadcrumb)
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        if (breadcrumb != state.breadcrumbs.last()) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Divider(color = Color(0xFF21262D), thickness = 1.dp)

            // Loading or error state
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF39D353))
                    }
                }
                state.errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(Res.string.file_explorer_error),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFFF7B72)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                state.errorMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }
                state.files.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(Res.string.file_explorer_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                }
                else -> {
                    // File list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(state.files) { file ->
                            FileItem(
                                file = file,
                                onClick = {
                                    if (file.isDirectory) {
                                        loadDirectory(file.path)
                                    } else {
                                        onFileSelected(file)
                                        onDismiss()
                                    }
                                },
                                onInstall = {
                                    onInstall(file)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileItem(
    file: FileEntry,
    onClick: () -> Unit,
    onInstall: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color(0xFF161B22),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = if (file.isDirectory) Color(0xFF39D353) else Color(0xFF58A6FF),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!file.isDirectory) {
                    Text(
                        text = formatFileSize(file.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            
            if (!file.isDirectory && file.name.endsWith(".apk", ignoreCase = true)) {
                IconButton(onClick = onInstall) {
                    Icon(
                        Icons.Default.Download, // Using Download icon for install/deploy
                        contentDescription = stringResource(Res.string.file_explorer_install_apk),
                        tint = Color(0xFF00FF00)
                    )
                }
            }
        }
    }
}

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
    val formattedSize = size / 1024.0.pow(digitGroups.toDouble())
    return "${(formattedSize * 10).toInt() / 10.0} ${units[digitGroups]}"
}
