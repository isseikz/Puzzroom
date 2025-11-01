package tokyo.isseikuzumaki.nlt.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.nlt.ui.viewmodel.NLTViewModel
import tokyo.isseikuzumaki.shared.ui.atoms.AppButton
import tokyo.isseikuzumaki.shared.ui.atoms.AppText

/**
 * Permissions setup page for first-time setup
 */
@Composable
fun PermissionsSetupPage(
    viewModel: NLTViewModel,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val permissionState by viewModel.permissionState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.refreshPermissions()
    }
    
    val allPermissionsGranted = permissionState.hasNotificationAccess && 
                                permissionState.hasLocationPermission
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppText(
            text = "権限の設定",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        AppText(
            text = "NLTを使用するには以下の権限が必要です",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Notification permission
        PermissionItem(
            title = "通知アクセス",
            description = "アプリの通知を監視するために必要です",
            isGranted = permissionState.hasNotificationAccess,
            onRequestClick = { viewModel.requestNotificationPermission() }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Location permission
        PermissionItem(
            title = "位置情報",
            description = "通知受信時の位置を記録するために必要です",
            isGranted = permissionState.hasLocationPermission,
            onRequestClick = { viewModel.requestLocationPermission() }
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Continue button
        AppButton(
            text = "続ける",
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            enabled = allPermissionsGranted
        )
        
        if (!allPermissionsGranted) {
            Spacer(modifier = Modifier.height(8.dp))
            AppText(
                text = "すべての権限を許可してください",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Skip button (for testing)
        TextButton(onClick = onContinue) {
            AppText(
                text = "スキップ（後で設定）",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Permission item component
 */
@Composable
private fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequestClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isGranted)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                AppText(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                AppText(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (!isGranted) {
                Spacer(modifier = Modifier.width(8.dp))
                AppButton(
                    text = "許可",
                    onClick = onRequestClick
                )
            }
        }
    }
}
