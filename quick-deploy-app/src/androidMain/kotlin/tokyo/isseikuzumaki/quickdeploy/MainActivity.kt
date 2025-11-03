package tokyo.isseikuzumaki.quickdeploy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import tokyo.isseikuzumaki.quickdeploy.service.QuickDeployMessagingService

/**
 * Main activity for Quick Deploy app
 */
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "Notification permission granted")
        } else {
            Log.w(TAG, "Notification permission denied")
        }
    }

    // State to trigger auto-download
    private val shouldAutoDownload = mutableStateOf(false)
    private val downloadUrl = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission for Android 13+
        requestNotificationPermission()

        // Handle intent data (from FCM notification)
        handleIntent(intent)

        setContent {
            App(
                shouldAutoDownload = shouldAutoDownload.value,
                downloadUrl = downloadUrl.value
            ) {
                // Reset the flags after handling
                shouldAutoDownload.value = false
                downloadUrl.value = null
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            val deviceToken = it.getStringExtra(
                QuickDeployMessagingService.EXTRA_DEVICE_TOKEN
            )
            val url = it.getStringExtra(
                QuickDeployMessagingService.EXTRA_DOWNLOAD_URL
            )

            if (deviceToken != null && url != null) {
                Log.d(TAG, "Auto-download triggered from notification")
                Log.d(TAG, "Download URL: $url")
                shouldAutoDownload.value = true
                downloadUrl.value = url
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d(TAG, "Notification permission already granted")
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

@Preview
@Composable
fun AppPreview() {
    App()
}
