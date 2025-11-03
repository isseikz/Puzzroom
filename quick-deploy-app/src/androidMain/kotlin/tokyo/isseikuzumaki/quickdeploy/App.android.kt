package tokyo.isseikuzumaki.quickdeploy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import tokyo.isseikuzumaki.quickdeploy.api.QuickDeployApiClient
import tokyo.isseikuzumaki.quickdeploy.repository.DeviceRepository
import tokyo.isseikuzumaki.quickdeploy.ui.guide.GuideScreen
import tokyo.isseikuzumaki.quickdeploy.ui.registration.RegistrationScreen
import tokyo.isseikuzumaki.quickdeploy.ui.registration.RegistrationViewModel
import tokyo.isseikuzumaki.quickdeploy.util.ApkInstaller

/**
 * Android-specific App implementation
 */
@Composable
actual fun App() {
    App(shouldAutoDownload = false, downloadUrl = null) {}
}

/**
 * App with auto-download support from notifications
 */
@Composable
fun App(shouldAutoDownload: Boolean, downloadUrl: String?, onAutoDownloadHandled: () -> Unit) {
    val context = LocalContext.current

    val baseUrl = "https://register-o45ehp4r5q-uc.a.run.app"

    val apiClient = QuickDeployApiClient(baseUrl)
    val deviceRepository = DeviceRepository(context, apiClient)
    val apkInstaller = ApkInstaller(context, apiClient)

    AppContent(
        registrationScreen = { onNavigateToGuide ->
            val viewModel = viewModel<RegistrationViewModel> {
                RegistrationViewModel(deviceRepository, apkInstaller)
            }

            // Handle auto-download from notification with URL
            LaunchedEffect(shouldAutoDownload, downloadUrl) {
                if (shouldAutoDownload && downloadUrl != null) {
                    viewModel.downloadAndInstall(downloadUrl)
                    onAutoDownloadHandled()
                }
            }

            RegistrationScreen(viewModel, onNavigateToGuide)
        },
        guideScreen = { onNavigateBack ->
            GuideScreen(onNavigateBack)
        }
    )
}
