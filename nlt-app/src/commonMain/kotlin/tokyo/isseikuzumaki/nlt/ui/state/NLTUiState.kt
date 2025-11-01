package tokyo.isseikuzumaki.nlt.ui.state

import tokyo.isseikuzumaki.nolotracker.data.model.NotificationRecord

/**
 * UI state for NLT application
 */
sealed interface NLTUiState {
    /**
     * Initial loading state
     */
    data object Loading : NLTUiState
    
    /**
     * User is not authenticated
     */
    data object NotAuthenticated : NLTUiState
    
    /**
     * User is authenticated and data is loaded
     */
    data class Authenticated(
        val userId: String,
        val userEmail: String?,
        val notifications: List<NotificationRecord> = emptyList(),
        val isLoadingNotifications: Boolean = false,
        val error: String? = null
    ) : NLTUiState
    
    /**
     * Error state
     */
    data class Error(val message: String) : NLTUiState
}

/**
 * Authentication state
 */
sealed interface AuthState {
    data object Idle : AuthState
    data object Loading : AuthState
    data class Success(val userId: String, val email: String?) : AuthState
    data class Error(val message: String) : AuthState
}

/**
 * Permission state
 */
data class PermissionState(
    val hasNotificationAccess: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val shouldShowNotificationRationale: Boolean = false,
    val shouldShowLocationRationale: Boolean = false
)

/**
 * Settings state
 */
data class SettingsState(
    val targetPackages: Set<String> = setOf(
        "com.example.paymentapp",
        "jp.co.paymentservice"
    ),
    val keywords: Set<String> = setOf(
        "支払い", "決済", "payment", "paid"
    )
)
