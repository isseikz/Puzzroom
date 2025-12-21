package tokyo.isseikuzumaki.vibeterminal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import tokyo.isseikuzumaki.vibeterminal.domain.repository.SshRepository
import tokyo.isseikuzumaki.vibeterminal.ui.utils.SyntaxHighlighter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeViewerSheet(
    filePath: String,
    onDismiss: () -> Unit
) {
    val sshRepository = koinInject<SshRepository>()
    var fileContent by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(filePath) {
        scope.launch {
            isLoading = true
            errorMessage = null

            val result = sshRepository.readFileContent(filePath)
            result.fold(
                onSuccess = { content ->
                    fileContent = content
                    isLoading = false
                },
                onFailure = { error ->
                    errorMessage = error.message ?: "Failed to load file"
                    isLoading = false
                }
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0D1117),
        contentColor = Color.White,
        modifier = Modifier.fillMaxHeight(0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Code Viewer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF39D353)
                    )
                    Text(
                        filePath.substringAfterLast("/"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        "Close",
                        tint = Color(0xFF39D353)
                    )
                }
            }

            Text(
                filePath,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Divider(color = Color(0xFF21262D), thickness = 1.dp)

            // Content
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF39D353))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Loading file...",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Error loading file",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFFF7B72)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                errorMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }
                fileContent != null -> {
                    val extension = filePath.substringAfterLast(".", "")
                    val lines = fileContent!!.lines()

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0D1117)),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        itemsIndexed(lines) { index, line ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                // Line number
                                Text(
                                    text = "${index + 1}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = Color(0xFF6E7681),
                                    modifier = Modifier
                                        .width(40.dp)
                                        .padding(end = 8.dp)
                                )

                                // Code line with syntax highlighting
                                Text(
                                    text = SyntaxHighlighter.highlight(line, extension),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
