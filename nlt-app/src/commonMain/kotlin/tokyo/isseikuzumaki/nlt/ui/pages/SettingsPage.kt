package tokyo.isseikuzumaki.nlt.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.nlt.ui.viewmodel.NLTViewModel
import tokyo.isseikuzumaki.shared.ui.atoms.AppButton
import tokyo.isseikuzumaki.shared.ui.atoms.AppText
import tokyo.isseikuzumaki.shared.ui.atoms.AppTextField

/**
 * Settings page for configuring target apps and keywords
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    viewModel: NLTViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settingsState by viewModel.settingsState.collectAsState()
    val permissionState by viewModel.permissionState.collectAsState()
    
    var packagesText by remember {
        mutableStateOf(settingsState.targetPackages.joinToString("\n"))
    }
    var keywordsText by remember {
        mutableStateOf(settingsState.keywords.joinToString(", "))
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { AppText("設定") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Permissions section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppText(
                        text = "権限",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    
                    // Notification permission
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            AppText(
                                text = "通知アクセス",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            AppText(
                                text = if (permissionState.hasNotificationAccess) "許可済み" else "未許可",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (permissionState.hasNotificationAccess)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error
                            )
                        }
                        
                        if (!permissionState.hasNotificationAccess) {
                            AppButton(
                                text = "設定を開く",
                                onClick = { viewModel.requestNotificationPermission() }
                            )
                        }
                    }
                    
                    Divider()
                    
                    // Location permission
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            AppText(
                                text = "位置情報",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            AppText(
                                text = if (permissionState.hasLocationPermission) "許可済み" else "未許可",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (permissionState.hasLocationPermission)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error
                            )
                        }
                        
                        if (!permissionState.hasLocationPermission) {
                            AppButton(
                                text = "リクエスト",
                                onClick = { viewModel.requestLocationPermission() }
                            )
                        }
                    }
                }
            }
            
            // Target packages section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppText(
                    text = "対象アプリ (パッケージ名)",
                    style = MaterialTheme.typography.titleMedium
                )
                
                AppText(
                    text = "監視する通知のパッケージ名を1行に1つずつ入力してください",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = packagesText,
                    onValueChange = { packagesText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    placeholder = {
                        AppText("例:\ncom.example.paymentapp\njp.co.paymentservice")
                    },
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                
                AppButton(
                    text = "保存",
                    onClick = {
                        val packages = packagesText
                            .split("\n")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .toSet()
                        viewModel.updateTargetPackages(packages)
                    },
                    modifier = Modifier.align(Alignment.End)
                )
            }
            
            // Keywords section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppText(
                    text = "フィルターキーワード",
                    style = MaterialTheme.typography.titleMedium
                )
                
                AppText(
                    text = "通知テキストに含まれるキーワードをカンマ区切りで入力してください",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                AppTextField(
                    value = keywordsText,
                    onValueChange = { keywordsText = it },
                    label = "キーワード",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false
                )
                
                AppButton(
                    text = "保存",
                    onClick = {
                        val keywords = keywordsText
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .toSet()
                        viewModel.updateKeywords(keywords)
                    },
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
