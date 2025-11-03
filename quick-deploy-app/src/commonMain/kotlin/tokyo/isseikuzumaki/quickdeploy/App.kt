package tokyo.isseikuzumaki.quickdeploy

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import tokyo.isseikuzumaki.quickdeploy.ui.navigation.Screen
import tokyo.isseikuzumaki.shared.ui.theme.AppTheme

/**
 * Main entry point for Quick Deploy application.
 *
 * Quick Deploy is a streamlined APK deployment tool that enables instant distribution
 * of Android apps from build environments to test devices.
 */
@Composable
expect fun App()



/**
 * Common navigation structure
 */
@Composable
fun AppContent(
    registrationScreen: @Composable (() -> Unit) -> Unit,
    guideScreen: @Composable (() -> Unit) -> Unit
) {
    AppTheme {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = Screen.Registration
        ) {
            composable<Screen.Registration> {
                registrationScreen {
                    navController.navigate(Screen.Guide)
                }
            }

            composable<Screen.Guide> {
                guideScreen {
                    navController.popBackStack()
                }
            }
        }
    }
}
