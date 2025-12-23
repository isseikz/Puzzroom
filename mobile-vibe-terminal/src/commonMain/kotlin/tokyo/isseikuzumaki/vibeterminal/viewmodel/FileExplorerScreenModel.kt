package tokyo.isseikuzumaki.vibeterminal.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tokyo.isseikuzumaki.vibeterminal.domain.model.FileEntry
import tokyo.isseikuzumaki.vibeterminal.domain.repository.SshRepository

data class FileExplorerState(
    val currentPath: String = "/",
    val files: List<FileEntry> = emptyList(),
    val breadcrumbs: List<String> = listOf("/"),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedFile: FileEntry? = null
)

class FileExplorerScreenModel(
    private val sshRepository: SshRepository
) : ScreenModel {

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
