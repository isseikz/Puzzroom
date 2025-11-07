package tokyo.isseikuzumaki.quickdeploy.ui.registration

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.Resource
import org.jetbrains.compose.resources.stringResource
import puzzroom.quick_deploy_app.generated.resources.Res
import puzzroom.quick_deploy_app.generated.resources.action_close
import puzzroom.quick_deploy_app.generated.resources.app_action_register_device
import puzzroom.quick_deploy_app.generated.resources.app_description
import puzzroom.quick_deploy_app.generated.resources.app_name_common
import puzzroom.quick_deploy_app.generated.resources.app_status_downloading_apk
import puzzroom.quick_deploy_app.generated.resources.app_status_registering_device
import puzzroom.quick_deploy_app.generated.resources.error_title
import puzzroom.quick_deploy_app.generated.resources.help_button_description
import puzzroom.quick_deploy_app.generated.resources.registration_clear_registration
import puzzroom.quick_deploy_app.generated.resources.registration_copy_curl
import puzzroom.quick_deploy_app.generated.resources.registration_copy_url
import puzzroom.quick_deploy_app.generated.resources.registration_device_registered
import puzzroom.quick_deploy_app.generated.resources.registration_download_install
import puzzroom.quick_deploy_app.generated.resources.registration_open_settings
import puzzroom.quick_deploy_app.generated.resources.registration_permission_description
import puzzroom.quick_deploy_app.generated.resources.registration_permission_required
import puzzroom.quick_deploy_app.generated.resources.registration_ready_to_receive
import puzzroom.quick_deploy_app.generated.resources.registration_upload_url_title
import puzzroom.quick_deploy_app.generated.resources.toast_curl_copied
import puzzroom.quick_deploy_app.generated.resources.toast_url_copied

/**
 * Device registration screen (C-001)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    viewModel: RegistrationViewModel,
    onNavigateToGuide: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.app_name_common)) },
                actions = {
                    IconButton(onClick = onNavigateToGuide) {
                        Icon(Icons.Default.Help, contentDescription = stringResource(Res.string.help_button_description))
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is RegistrationUiState.Loading -> LoadingContent()
                is RegistrationUiState.NotRegistered -> NotRegisteredContent(
                    onRegister = { viewModel.registerDevice() }
                )
                is RegistrationUiState.Registering -> RegisteringContent()
                is RegistrationUiState.Downloading -> DownloadingContent()
                is RegistrationUiState.Registered -> {
                    val urlCopiedMessage = stringResource(Res.string.toast_url_copied)
                    val curlCopiedMessage = stringResource(Res.string.toast_curl_copied)
                    RegisteredContent(
                        uploadUrl = state.uploadUrl,
                        downloadUrl = state.downloadUrl,
                        canInstall = state.canInstall,
                        onCopyUrl = { url -> copyToClipboard(context, url, urlCopiedMessage) },
                        onCopyCurlCommand = { url -> copyCurlCommand(context, url, curlCopiedMessage) },
                        onRequestPermission = { viewModel.requestInstallPermission() },
                        onDownloadAndInstall = { viewModel.downloadAndInstall(state.downloadUrl) },
                        onClearRegistration = { viewModel.clearRegistration() }
                    )
                }
                is RegistrationUiState.Error -> ErrorContent(
                    message = state.message,
                    onDismiss = { viewModel.dismissError() }
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun NotRegisteredContent(onRegister: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.PhoneAndroid,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(Res.string.app_name_common),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.app_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRegister,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AppRegistration, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(Res.string.app_action_register_device))
        }
    }
}

@Composable
private fun RegisteringContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(Res.string.app_status_registering_device))
        }
    }
}

@Composable
private fun DownloadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(Res.string.app_status_downloading_apk))
        }
    }
}

@Composable
private fun RegisteredContent(
    uploadUrl: String,
    downloadUrl: String,
    canInstall: Boolean,
    onCopyUrl: (String) -> Unit,
    onCopyCurlCommand: (String) -> Unit,
    onRequestPermission: () -> Unit,
    onDownloadAndInstall: () -> Unit,
    onClearRegistration: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Status card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(Res.string.registration_device_registered),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(Res.string.registration_ready_to_receive),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Upload URL section
        Text(
            text = stringResource(Res.string.registration_upload_url_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = uploadUrl,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onCopyUrl(uploadUrl) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(Res.string.registration_copy_url), style = MaterialTheme.typography.bodySmall)
                    }

                    Button(
                        onClick = { onCopyCurlCommand(uploadUrl) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(Res.string.registration_copy_curl), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Install permission warning
        if (!canInstall) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(Res.string.registration_permission_required),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(Res.string.registration_permission_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(Res.string.registration_open_settings))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Manual download button
        Button(
            onClick = onDownloadAndInstall,
            modifier = Modifier.fillMaxWidth(),
            enabled = canInstall
        ) {
            Icon(Icons.Default.Download, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(Res.string.registration_download_install))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Clear registration button
        OutlinedButton(
            onClick = onClearRegistration,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(Res.string.registration_clear_registration))
        }
    }
}

@Composable
private fun ErrorContent(message: String, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.error_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onDismiss) {
            Text(stringResource(Res.string.action_close))
        }
    }
}

private fun copyToClipboard(context: Context, text: String, message: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Upload URL", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

private fun copyCurlCommand(context: Context, uploadUrl: String, message: String) {
    val curlCommand = """curl -X POST -F "file=@/path/to/your/app.apk" "$uploadUrl""""
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("cURL Command", curlCommand)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
