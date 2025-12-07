package tokyo.isseikuzumaki.unison.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tokyo.isseikuzumaki.unison.data.AudioRepository

/**
 * Lightweight ViewModel for Library screen
 * Only handles file picker results and basic validation
 */
class LibraryViewModel(
    private val audioRepository: AudioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Idle)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    fun onFileSelected(uri: String) {
        viewModelScope.launch {
            _uiState.value = LibraryUiState.Validating
            val isValid = audioRepository.validateAudioFile(uri)
            if (isValid) {
                _uiState.value = LibraryUiState.Valid(uri)
            } else {
                _uiState.value = LibraryUiState.Error("Invalid audio file")
            }
        }
    }

    fun clearError() {
        _uiState.value = LibraryUiState.Idle
    }
}

sealed class LibraryUiState {
    data object Idle : LibraryUiState()
    data object Validating : LibraryUiState()
    data class Valid(val uri: String) : LibraryUiState()
    data class Error(val message: String) : LibraryUiState()
}
