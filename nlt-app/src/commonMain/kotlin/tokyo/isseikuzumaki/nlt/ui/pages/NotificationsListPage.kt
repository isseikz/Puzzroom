package tokyo.isseikuzumaki.nlt.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import tokyo.isseikuzumaki.nlt.ui.state.NLTUiState
import tokyo.isseikuzumaki.nlt.ui.viewmodel.NLTViewModel
import tokyo.isseikuzumaki.nolotracker.data.model.NotificationRecord
import tokyo.isseikuzumaki.shared.ui.atoms.AppCard
import tokyo.isseikuzumaki.shared.ui.atoms.AppText
import tokyo.isseikuzumaki.shared.ui.atoms.AppIconButton

/**
 * Notifications list page showing all captured notification records
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsListPage(
    viewModel: NLTViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { AppText("通知履歴") },
                actions = {
                    // Refresh button
                    IconButton(onClick = {
                        scope.launch { viewModel.loadNotifications() }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "更新")
                    }
                    
                    // Settings button
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "設定")
                    }
                    
                    // Logout button
                    IconButton(onClick = {
                        scope.launch { viewModel.signOut() }
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "ログアウト")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is NLTUiState.Authenticated -> {
                if (state.isLoadingNotifications) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (state.notifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AppText(
                                text = "通知がありません",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            AppText(
                                text = "アプリから通知が届くと、ここに表示されます",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.notifications) { notification ->
                            NotificationRecordCard(
                                notification = notification,
                                onAddToFilter = { packageName ->
                                    scope.launch {
                                        viewModel.addPackageToFilter(packageName)
                                    }
                                }
                            )
                        }
                    }
                }
                
                // Error snackbar
                state.error?.let { error ->
                    Snackbar(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        AppText(error)
                    }
                }
            }
            else -> {
                // Should not reach here if navigation is correct
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    AppText("予期しないエラーが発生しました")
                }
            }
        }
    }
}

/**
 * Card component displaying a single notification record
 */
@Composable
private fun NotificationRecordCard(
    notification: NotificationRecord,
    onAddToFilter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row with app info and add button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // App icon placeholder (using first letter of package name)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                AppText(
                                    text = notification.packageName.firstOrNull()?.uppercase() ?: "?",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        
                        // App name (package name)
                        AppText(
                            text = notification.packageName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Plus button to add to filter
                AppIconButton(
                    imageVector = Icons.Default.Add,
                    contentDescription = "フィルターに追加",
                    onClick = { onAddToFilter(notification.packageName) },
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Title
            notification.title?.let { title ->
                AppText(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            // Text
            AppText(
                text = notification.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Payment info if parsed
            if (notification.isParsed && notification.parsedAmount != null) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        AppText(
                            text = "金額",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        AppText(
                            text = notification.parsedAmount ?: "-",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    notification.parsedMerchant?.let { merchant ->
                        Column(horizontalAlignment = Alignment.End) {
                            AppText(
                                text = "お店",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            AppText(
                                text = merchant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
            
            // Location info
            if (notification.hasLocation()) {
                Spacer(modifier = Modifier.height(8.dp))
                AppText(
                    text = "📍 ${notification.latitude}, ${notification.longitude}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Timestamp
            Spacer(modifier = Modifier.height(8.dp))
            AppText(
                text = notification.time?.toString() ?: "時刻不明",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
