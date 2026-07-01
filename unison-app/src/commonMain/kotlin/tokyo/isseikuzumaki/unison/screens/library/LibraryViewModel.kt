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
 * Handles file picker results and basic validation for both audio and transcription files
 */
class LibraryViewModel(
    private val audioRepository: AudioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Idle)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private var audioUri: String? = null
    private var transcriptionUri: String? = null

    fun onAudioFileSelected(uri: String) {
        viewModelScope.launch {
            _uiState.value = LibraryUiState.Validating
            val isValid = audioRepository.validateAudioFile(uri)
            if (isValid) {
                audioUri = uri
                _uiState.value = LibraryUiState.AudioSelected(uri)
            } else {
                _uiState.value = LibraryUiState.Error("Invalid audio file")
            }
        }
    }

    fun onTranscriptionFileSelected(uri: String) {
        transcriptionUri = uri
        _uiState.value = LibraryUiState.TranscriptionSelected(uri)
    }

    fun onStartSession() {
        val audio = audioUri
        val transcription = transcriptionUri

        if (audio == null) {
            _uiState.value = LibraryUiState.Error("Please select an audio file")
            return
        }

        if (transcription == null) {
            _uiState.value = LibraryUiState.Error("Please select a transcription file")
            return
        }

        _uiState.value = LibraryUiState.Valid(audio, transcription)
    }

    fun clearError() {
        _uiState.value = if (audioUri != null && transcriptionUri != null) {
            LibraryUiState.BothSelected(audioUri!!, transcriptionUri!!)
        } else if (audioUri != null) {
            LibraryUiState.AudioSelected(audioUri!!)
        } else if (transcriptionUri != null) {
            LibraryUiState.TranscriptionSelected(transcriptionUri!!)
        } else {
            LibraryUiState.Idle
        }
    }
}

sealed class LibraryUiState {
    data object Idle : LibraryUiState()
    data object Validating : LibraryUiState()
    data class AudioSelected(val audioUri: String) : LibraryUiState()
    data class TranscriptionSelected(val transcriptionUri: String) : LibraryUiState()
    data class BothSelected(val audioUri: String, val transcriptionUri: String) : LibraryUiState()
    data class Valid(val audioUri: String, val transcriptionUri: String) : LibraryUiState()
    data class Error(val message: String) : LibraryUiState()
}
