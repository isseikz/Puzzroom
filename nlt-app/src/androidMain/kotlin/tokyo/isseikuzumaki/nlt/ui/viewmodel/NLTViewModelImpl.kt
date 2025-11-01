package tokyo.isseikuzumaki.nlt.ui.viewmodel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import tokyo.isseikuzumaki.nlt.ui.state.AuthState
import tokyo.isseikuzumaki.nlt.ui.state.NLTUiState
import tokyo.isseikuzumaki.nlt.ui.state.PermissionState
import tokyo.isseikuzumaki.nlt.ui.state.SettingsState
import tokyo.isseikuzumaki.nolotracker.data.repository.FirestoreRepositoryImpl

/**
 * Android implementation of NLTViewModel
 */
class NLTViewModelImpl(
    private val context: Context
) : ViewModel(), NLTViewModel {
    
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val repository = FirestoreRepositoryImpl()
    private val oneTapClient: SignInClient = Identity.getSignInClient(context)
    
    private var googleSignInLauncher: ActivityResultLauncher<IntentSenderRequest>? = null
    
    companion object {
        private const val TAG = "NLTViewModel"
        // Replace with your actual Web Client ID from Firebase Console
        private const val WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
    }
    
    private val _uiState = MutableStateFlow<NLTUiState>(NLTUiState.Loading)
    override val uiState: StateFlow<NLTUiState> = _uiState.asStateFlow()
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _permissionState = MutableStateFlow(PermissionState())
    override val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()
    
    private val _settingsState = MutableStateFlow(SettingsState())
    override val settingsState: StateFlow<SettingsState> = _settingsState.asStateFlow()
    
    init {
        // Check initial auth state
        checkAuthState()
        // Refresh permissions
        refreshPermissions()
    }
    
    private fun checkAuthState() {
        viewModelScope.launch {
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                _authState.value = AuthState.Success(
                    userId = currentUser.uid,
                    email = currentUser.email
                )
                _uiState.value = NLTUiState.Authenticated(
                    userId = currentUser.uid,
                    userEmail = currentUser.email
                )
                loadNotifications()
            } else {
                _authState.value = AuthState.Idle
                _uiState.value = NLTUiState.NotAuthenticated
            }
        }
    }
    
    /**
     * Set the Google Sign-in launcher
     */
    fun setGoogleSignInLauncher(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        googleSignInLauncher = launcher
    }
    
    override fun signInWithGoogle() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            try {
                val signInRequest = BeginSignInRequest.builder()
                    .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                            .setSupported(true)
                            .setServerClientId(WEB_CLIENT_ID)
                            .setFilterByAuthorizedAccounts(false)
                            .build()
                    )
                    .setAutoSelectEnabled(true)
                    .build()
                
                val result = oneTapClient.beginSignIn(signInRequest).await()
                
                val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent).build()
                googleSignInLauncher?.launch(intentSenderRequest)
                
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-in failed", e)
                _authState.value = AuthState.Error(e.message ?: "Google Sign-in failed")
                _uiState.value = NLTUiState.NotAuthenticated
            }
        }
    }
    
    /**
     * Handle Google Sign-in result
     */
    suspend fun handleGoogleSignInResult(data: Intent?) {
        try {
            val credential = oneTapClient.getSignInCredentialFromIntent(data)
            val idToken = credential.googleIdToken
            
            if (idToken != null) {
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val result = firebaseAuth.signInWithCredential(firebaseCredential).await()
                val user = result.user
                
                if (user != null) {
                    _authState.value = AuthState.Success(
                        userId = user.uid,
                        email = user.email
                    )
                    _uiState.value = NLTUiState.Authenticated(
                        userId = user.uid,
                        userEmail = user.email
                    )
                    loadNotifications()
                } else {
                    _authState.value = AuthState.Error("Sign in failed")
                    _uiState.value = NLTUiState.NotAuthenticated
                }
            } else {
                _authState.value = AuthState.Error("No ID token received")
                _uiState.value = NLTUiState.NotAuthenticated
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-in credential handling failed", e)
            _authState.value = AuthState.Error(e.message ?: "Authentication failed")
            _uiState.value = NLTUiState.NotAuthenticated
        }
    }
    
    override suspend fun signOut() {
        firebaseAuth.signOut()
        _authState.value = AuthState.Idle
        _uiState.value = NLTUiState.NotAuthenticated
    }
    
    override suspend fun loadNotifications() {
        val currentState = _uiState.value
        if (currentState !is NLTUiState.Authenticated) return
        
        _uiState.value = currentState.copy(isLoadingNotifications = true)
        
        viewModelScope.launch {
            try {
                repository.getNotificationRecords().collect { notifications ->
                    _uiState.value = NLTUiState.Authenticated(
                        userId = currentState.userId,
                        userEmail = currentState.userEmail,
                        notifications = notifications,
                        isLoadingNotifications = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isLoadingNotifications = false,
                    error = e.message
                )
            }
        }
    }
    
    override fun requestNotificationPermission() {
        // Open notification settings
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
    
    override fun requestLocationPermission() {
        // This will be handled by the Activity using ActivityResultContracts
        // The Activity will call refreshPermissions() after the result
    }
    
    override fun updateTargetPackages(packages: Set<String>) {
        _settingsState.value = _settingsState.value.copy(targetPackages = packages)
        // TODO: Save to SharedPreferences
    }
    
    override fun updateKeywords(keywords: Set<String>) {
        _settingsState.value = _settingsState.value.copy(keywords = keywords)
        // TODO: Save to SharedPreferences
    }
    
    override fun refreshPermissions() {
        val hasNotificationAccess = NotificationManagerCompat
            .getEnabledListenerPackages(context)
            .contains(context.packageName)
        
        // Check location permission
        val hasLocationPermission = context.checkSelfPermission(
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        _permissionState.value = _permissionState.value.copy(
            hasNotificationAccess = hasNotificationAccess,
            hasLocationPermission = hasLocationPermission
        )
    }
}
