package tokyo.isseikuzumaki.quickdeploy.ui.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tokyo.isseikuzumaki.quickdeploy.repository.DeviceRepository
import tokyo.isseikuzumaki.quickdeploy.util.ApkInstaller

/**
 * ViewModel for device registration screen (C-001)
 */
class RegistrationViewModel(
    private val deviceRepository: DeviceRepository,
    private val apkInstaller: ApkInstaller
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegistrationUiState>(RegistrationUiState.Loading)
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    init {
        loadRegistrationState()
    }

    /**
     * Load current registration state
     */
    private fun loadRegistrationState() {
        viewModelScope.launch {
            val uploadUrl = deviceRepository.getUploadUrl()
            val downloadUrl = deviceRepository.getDownloadUrl()

            if (uploadUrl != null && downloadUrl != null) {
                _uiState.value = RegistrationUiState.Registered(
                    uploadUrl = uploadUrl,
                    downloadUrl = downloadUrl,
                    canInstall = apkInstaller.canInstallPackages()
                )
            } else {
                _uiState.value = RegistrationUiState.NotRegistered
            }
        }
    }

    /**
     * Register device and generate URLs (C-001)
     */
    fun registerDevice() {
        viewModelScope.launch {
            _uiState.value = RegistrationUiState.Registering

            val result = deviceRepository.registerDevice()

            result.onSuccess { response ->
                _uiState.value = RegistrationUiState.Registered(
                    uploadUrl = response.uploadUrl,
                    downloadUrl = response.downloadUrl,
                    canInstall = apkInstaller.canInstallPackages()
                )
            }.onFailure { error ->
                _uiState.value = RegistrationUiState.Error(
                    message = error.message ?: "登録に失敗しました"
                )
            }
        }
    }

    /**
     * Clear registration and reset
     */
    fun clearRegistration() {
        deviceRepository.clearRegistration()
        _uiState.value = RegistrationUiState.NotRegistered
    }

    /**
     * Request install permission (C-005)
     */
    fun requestInstallPermission() {
        apkInstaller.openInstallPermissionSettings()
    }

    /**
     * Download and install APK
     */
    fun downloadAndInstall(downloadUrl: String) {
        viewModelScope.launch {
            _uiState.value = RegistrationUiState.Downloading

            val result = apkInstaller.downloadAndInstall(downloadUrl)

            result.onSuccess {
                // Return to registered state after installation starts
                loadRegistrationState()
            }.onFailure { error ->
                _uiState.value = RegistrationUiState.Error(
                    message = error.message ?: "ダウンロードに失敗しました"
                )
            }
        }
    }

    /**
     * Auto-download APK when triggered from notification
     * This is called when the app is opened from a push notification
     */
    fun autoDownloadFromNotification() {
        viewModelScope.launch {
            val downloadUrl = deviceRepository.getDownloadUrl()
            if (downloadUrl != null) {
                downloadAndInstall(downloadUrl)
            }
        }
    }

    /**
     * Dismiss error and return to previous state
     */
    fun dismissError() {
        loadRegistrationState()
    }

    /**
     * Refresh permission state when returning from settings
     * This is called when the app comes to foreground
     */
    fun refreshPermissionState() {
        val currentState = _uiState.value
        if (currentState is RegistrationUiState.Registered) {
            // Only update if we're in the Registered state
            val newCanInstall = apkInstaller.canInstallPackages()
            if (newCanInstall != currentState.canInstall) {
                _uiState.value = currentState.copy(canInstall = newCanInstall)
            }
        }
    }
}

/**
 * UI state for registration screen
 */
sealed interface RegistrationUiState {
    data object Loading : RegistrationUiState
    data object NotRegistered : RegistrationUiState
    data object Registering : RegistrationUiState
    data object Downloading : RegistrationUiState

    data class Registered(
        val uploadUrl: String,
        val downloadUrl: String,
        val canInstall: Boolean
    ) : RegistrationUiState

    data class Error(
        val message: String
    ) : RegistrationUiState
}
