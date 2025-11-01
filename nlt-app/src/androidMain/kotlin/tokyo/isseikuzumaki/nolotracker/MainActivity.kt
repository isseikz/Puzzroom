package tokyo.isseikuzumaki.nolotracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import tokyo.isseikuzumaki.nlt.ui.pages.NotificationsListPage
import tokyo.isseikuzumaki.nlt.ui.pages.PermissionsSetupPage
import tokyo.isseikuzumaki.nlt.ui.pages.SettingsPage
import tokyo.isseikuzumaki.nlt.ui.pages.SignInPage
import tokyo.isseikuzumaki.nlt.ui.state.NLTUiState
import tokyo.isseikuzumaki.nlt.ui.viewmodel.NLTViewModelImpl
import tokyo.isseikuzumaki.shared.ui.theme.AppTheme

/**
 * Main Activity for NLT application.
 * 
 * Entry point for the NLT Android application that sets up the UI with
 * Compose Multiplatform and the shared theme.
 */
class MainActivity : ComponentActivity() {
    
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission result will be handled by the ViewModel
        viewModelInstance?.refreshPermissions()
    }
    
    private var viewModelInstance: NLTViewModelImpl? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NLTApp(
                        onRequestLocationPermission = {
                            requestLocationPermission()
                        }
                    )
                }
            }
        }
    }
    
    private fun requestLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                viewModelInstance?.refreshPermissions()
            }
            else -> {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }
}

/**
 * Main composable for NLT application with navigation
 */
@Composable
fun NLTApp(
    onRequestLocationPermission: () -> Unit
) {
    val navController = rememberNavController()
    val viewModel: NLTViewModelImpl = viewModel { NLTViewModelImpl(navController.context) }
    val uiState by viewModel.uiState.collectAsState()
    
    // Determine start destination based on UI state
    val startDestination = when (uiState) {
        is NLTUiState.NotAuthenticated -> "signin"
        is NLTUiState.Authenticated -> "notifications"
        else -> "signin"
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("signin") {
            SignInPage(
                viewModel = viewModel
            )
            
            // Navigate to permissions when authenticated
            LaunchedEffect(uiState) {
                if (uiState is NLTUiState.Authenticated) {
                    navController.navigate("permissions") {
                        popUpTo("signin") { inclusive = true }
                    }
                }
            }
        }
        
        composable("permissions") {
            PermissionsSetupPage(
                viewModel = viewModel,
                onContinue = {
                    navController.navigate("notifications") {
                        popUpTo("permissions") { inclusive = true }
                    }
                }
            )
        }
        
        composable("notifications") {
            NotificationsListPage(
                viewModel = viewModel,
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
            
            // Navigate back to signin if logged out
            LaunchedEffect(uiState) {
                if (uiState is NLTUiState.NotAuthenticated) {
                    navController.navigate("signin") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
        
        composable("settings") {
            SettingsPage(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
