package tokyo.isseikuzumaki.nlt.ui.viewmodel

import kotlinx.coroutines.flow.StateFlow
import tokyo.isseikuzumaki.nlt.ui.state.AuthState
import tokyo.isseikuzumaki.nlt.ui.state.NLTUiState
import tokyo.isseikuzumaki.nlt.ui.state.PermissionState
import tokyo.isseikuzumaki.nlt.ui.state.SettingsState

/**
 * ViewModel interface for NLT application
 * 
 * Platform-specific implementations handle authentication, permissions, and data loading.
 */
interface NLTViewModel {
    /**
     * Current UI state
     */
    val uiState: StateFlow<NLTUiState>
    
    /**
     * Current authentication state
     */
    val authState: StateFlow<AuthState>
    
    /**
     * Current permission state
     */
    val permissionState: StateFlow<PermissionState>
    
    /**
     * Current settings state
     */
    val settingsState: StateFlow<SettingsState>
    
    /**
     * Sign in with Google
     */
    fun signInWithGoogle()
    
    /**
     * Sign out the current user
     */
    suspend fun signOut()
    
    /**
     * Load notification records for the authenticated user
     */
    suspend fun loadNotifications()
    
    /**
     * Request notification access permission
     */
    fun requestNotificationPermission()
    
    /**
     * Request location permission
     */
    fun requestLocationPermission()
    
    /**
     * Update target package names for filtering
     */
    fun updateTargetPackages(packages: Set<String>)
    
    /**
     * Update keywords for filtering
     */
    fun updateKeywords(keywords: Set<String>)
    
    /**
     * Refresh permission states
     */
    fun refreshPermissions()
}
