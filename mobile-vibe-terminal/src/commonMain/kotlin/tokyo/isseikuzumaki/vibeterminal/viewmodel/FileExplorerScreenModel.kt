package tokyo.isseikuzumaki.vibeterminal.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tokyo.isseikuzumaki.vibeterminal.domain.downloader.FileDownloader
import tokyo.isseikuzumaki.vibeterminal.domain.model.FileEntry
import tokyo.isseikuzumaki.vibeterminal.domain.model.FileTransferState
import tokyo.isseikuzumaki.vibeterminal.domain.model.TransferPurpose
import tokyo.isseikuzumaki.vibeterminal.domain.model.TransferStatus
import tokyo.isseikuzumaki.vibeterminal.domain.repository.SshRepository
import tokyo.isseikuzumaki.vibeterminal.domain.sharer.FileSharer
import tokyo.isseikuzumaki.vibeterminal.util.MimeTypeUtil

data class FileExplorerState(
    val currentPath: String = "/",
    val files: List<FileEntry> = emptyList(),
    val breadcrumbs: List<String> = listOf("/"),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedFile: FileEntry? = null,
    val activeTransfer: FileTransferState? = null,
    val transferSuccessMessage: String? = null
)

class FileExplorerScreenModel(
    private val sshRepository: SshRepository,
    private val fileDownloader: FileDownloader,
    private val fileSharer: FileSharer
) : ScreenModel {

    private var transferJob: Job? = null

    private val _state = MutableStateFlow(FileExplorerState())
    val state: StateFlow<FileExplorerState> = _state.asStateFlow()

    fun loadDirectory(path: String) {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            val result = sshRepository.listFiles(path)

            result.fold(
                onSuccess = { files ->
                    val breadcrumbs = generateBreadcrumbs(path)
                    _state.update {
                        it.copy(
                            currentPath = path,
                            files = files,
                            breadcrumbs = breadcrumbs,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load directory"
                        )
                    }
                }
            )
        }
    }

    fun navigateToPath(path: String) {
        loadDirectory(path)
    }

    fun navigateUp() {
        val currentPath = _state.value.currentPath
        if (currentPath == "/") return

        val parentPath = currentPath.substringBeforeLast("/").ifEmpty { "/" }
        loadDirectory(parentPath)
    }

    fun selectFile(file: FileEntry) {
        if (file.isDirectory) {
            loadDirectory(file.path)
        } else {
            _state.update { it.copy(selectedFile = file) }
        }
    }

    fun clearSelectedFile() {
        _state.update { it.copy(selectedFile = null) }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun clearTransferSuccessMessage() {
        _state.update { it.copy(transferSuccessMessage = null) }
    }

    /**
     * Download a file to the device's download directory.
     */
    fun downloadFile(file: FileEntry) {
        if (file.isDirectory) return
        if (_state.value.activeTransfer != null) return // Already transferring

        transferJob = screenModelScope.launch {
            try {
                // Initialize transfer state
                _state.update {
                    it.copy(
                        activeTransfer = FileTransferState(
                            fileEntry = file,
                            purpose = TransferPurpose.Download,
                            status = TransferStatus.Pending
                        ),
                        errorMessage = null
                    )
                }

                // Get download directory and generate unique filename
                val downloadDir = fileDownloader.getDownloadDirectory()
                val localFile = fileDownloader.generateUniqueFilename(downloadDir, file.name)

                // Update to in-progress
                _state.update {
                    it.copy(
                        activeTransfer = it.activeTransfer?.copy(status = TransferStatus.InProgress)
                    )
                }

                // Download with progress
                val result = sshRepository.downloadFileWithProgress(
                    remotePath = file.path,
                    localFile = localFile,
                    totalBytes = file.size
                ) { bytesTransferred, totalBytes ->
                    val progress = if (totalBytes > 0) {
                        (bytesTransferred.toFloat() / totalBytes).coerceIn(0f, 1f)
                    } else 0f

                    _state.update {
                        it.copy(
                            activeTransfer = it.activeTransfer?.copy(
                                progress = progress,
                                bytesTransferred = bytesTransferred
                            )
                        )
                    }
                }

                result.fold(
                    onSuccess = {
                        // Notify system about download
                        val mimeType = MimeTypeUtil.getMimeType(file.name)
                        fileDownloader.notifyDownloadComplete(localFile, mimeType)

                        _state.update {
                            it.copy(
                                activeTransfer = it.activeTransfer?.copy(
                                    status = TransferStatus.Completed,
                                    progress = 1f,
                                    localPath = localFile.absolutePath
                                ),
                                transferSuccessMessage = "Downloaded: ${file.name}"
                            )
                        }

                        // Clear transfer state after a short delay
                        kotlinx.coroutines.delay(500)
                        _state.update { it.copy(activeTransfer = null) }
                    },
                    onFailure = { error ->
                        _state.update {
                            it.copy(
                                activeTransfer = it.activeTransfer?.copy(
                                    status = TransferStatus.Failed,
                                    error = getErrorMessage(error)
                                ),
                                errorMessage = getErrorMessage(error)
                            )
                        }
                        // Clear transfer state after showing error
                        kotlinx.coroutines.delay(2000)
                        _state.update { it.copy(activeTransfer = null) }
                    }
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                _state.update {
                    it.copy(
                        activeTransfer = it.activeTransfer?.copy(status = TransferStatus.Cancelled),
                        errorMessage = null
                    )
                }
                kotlinx.coroutines.delay(500)
                _state.update { it.copy(activeTransfer = null) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        activeTransfer = it.activeTransfer?.copy(
                            status = TransferStatus.Failed,
                            error = getErrorMessage(e)
                        ),
                        errorMessage = getErrorMessage(e)
                    )
                }
                kotlinx.coroutines.delay(2000)
                _state.update { it.copy(activeTransfer = null) }
            }
        }
    }

    /**
     * Share a file via the system share sheet.
     * Downloads to a temporary location first, then opens share dialog.
     */
    fun shareFile(file: FileEntry) {
        if (file.isDirectory) return
        if (_state.value.activeTransfer != null) return // Already transferring

        transferJob = screenModelScope.launch {
            try {
                // Initialize transfer state
                _state.update {
                    it.copy(
                        activeTransfer = FileTransferState(
                            fileEntry = file,
                            purpose = TransferPurpose.Share,
                            status = TransferStatus.Pending
                        ),
                        errorMessage = null
                    )
                }

                // Get share cache directory
                val shareDir = fileSharer.getShareCacheDirectory()
                val localFile = java.io.File(shareDir, file.name)

                // Update to in-progress
                _state.update {
                    it.copy(
                        activeTransfer = it.activeTransfer?.copy(status = TransferStatus.InProgress)
                    )
                }

                // Download with progress
                val result = sshRepository.downloadFileWithProgress(
                    remotePath = file.path,
                    localFile = localFile,
                    totalBytes = file.size
                ) { bytesTransferred, totalBytes ->
                    val progress = if (totalBytes > 0) {
                        (bytesTransferred.toFloat() / totalBytes).coerceIn(0f, 1f)
                    } else 0f

                    _state.update {
                        it.copy(
                            activeTransfer = it.activeTransfer?.copy(
                                progress = progress,
                                bytesTransferred = bytesTransferred
                            )
                        )
                    }
                }

                result.fold(
                    onSuccess = {
                        // Share the file
                        val mimeType = MimeTypeUtil.getMimeType(file.name)
                        val shareResult = fileSharer.shareFile(localFile, mimeType)

                        shareResult.fold(
                            onSuccess = {
                                _state.update {
                                    it.copy(
                                        activeTransfer = it.activeTransfer?.copy(
                                            status = TransferStatus.Completed,
                                            progress = 1f,
                                            localPath = localFile.absolutePath
                                        )
                                    )
                                }
                            },
                            onFailure = { error ->
                                _state.update {
                                    it.copy(
                                        activeTransfer = it.activeTransfer?.copy(
                                            status = TransferStatus.Failed,
                                            error = getErrorMessage(error)
                                        ),
                                        errorMessage = getErrorMessage(error)
                                    )
                                }
                            }
                        )

                        // Clear transfer state
                        kotlinx.coroutines.delay(500)
                        _state.update { it.copy(activeTransfer = null) }

                        // Cleanup old share cache files
                        fileSharer.cleanupShareCache()
                    },
                    onFailure = { error ->
                        _state.update {
                            it.copy(
                                activeTransfer = it.activeTransfer?.copy(
                                    status = TransferStatus.Failed,
                                    error = getErrorMessage(error)
                                ),
                                errorMessage = getErrorMessage(error)
                            )
                        }
                        kotlinx.coroutines.delay(2000)
                        _state.update { it.copy(activeTransfer = null) }
                    }
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                _state.update {
                    it.copy(
                        activeTransfer = it.activeTransfer?.copy(status = TransferStatus.Cancelled),
                        errorMessage = null
                    )
                }
                kotlinx.coroutines.delay(500)
                _state.update { it.copy(activeTransfer = null) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        activeTransfer = it.activeTransfer?.copy(
                            status = TransferStatus.Failed,
                            error = getErrorMessage(e)
                        ),
                        errorMessage = getErrorMessage(e)
                    )
                }
                kotlinx.coroutines.delay(2000)
                _state.update { it.copy(activeTransfer = null) }
            }
        }
    }

    /**
     * Cancel the current file transfer operation.
     */
    fun cancelTransfer() {
        transferJob?.cancel()
        transferJob = null
        _state.update {
            it.copy(
                activeTransfer = it.activeTransfer?.copy(status = TransferStatus.Cancelled)
            )
        }
        screenModelScope.launch {
            kotlinx.coroutines.delay(500)
            _state.update { it.copy(activeTransfer = null) }
        }
    }

    /**
     * Convert exception to user-friendly error message.
     */
    private fun getErrorMessage(error: Throwable): String {
        return when {
            error.message?.contains("No space left", ignoreCase = true) == true ||
            error.message?.contains("disk full", ignoreCase = true) == true ->
                "Not enough storage space. Free up space and try again."

            error.message?.contains("permission", ignoreCase = true) == true ->
                "Storage permission required. Grant permission in Settings."

            error.message?.contains("not found", ignoreCase = true) == true ||
            error.message?.contains("No such file", ignoreCase = true) == true ->
                "File no longer exists on server."

            error.message?.contains("connection", ignoreCase = true) == true ||
            error.message?.contains("network", ignoreCase = true) == true ||
            error.message?.contains("timeout", ignoreCase = true) == true ->
                "Download failed. Check your connection and try again."

            error.message?.contains("session", ignoreCase = true) == true ||
            error.message?.contains("disconnect", ignoreCase = true) == true ->
                "Connection lost. Reconnect to continue."

            else -> error.message ?: "Download failed. Please try again."
        }
    }

    private fun generateBreadcrumbs(path: String): List<String> {
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
}
