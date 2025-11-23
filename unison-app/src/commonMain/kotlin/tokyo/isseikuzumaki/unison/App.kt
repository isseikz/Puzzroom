package tokyo.isseikuzumaki.unison

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import tokyo.isseikuzumaki.unison.screens.library.LibraryScreenPlatform
import tokyo.isseikuzumaki.unison.screens.recorder.RecorderScreen
import tokyo.isseikuzumaki.unison.screens.editor.SyncEditorScreen
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

/**
 * Navigation destinations for Unison app
 */
@Serializable
object LibraryDestination

@Serializable
data class SessionGraph(val uri: String)

@Serializable
object RecorderDestination

@Serializable
object EditorDestination

@Composable
fun App() {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    ) {
        Surface {
            val navController: NavHostController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = LibraryDestination
            ) {
                // Library Screen - Global scope, lightweight
                composable<LibraryDestination> {
                    LibraryScreenPlatform(
                        onNavigateToSession = { uri ->
                            navController.navigate(SessionGraph(uri))
                        }
                    )
                }

                // Session Graph - Scoped navigation for heavy audio operations
                navigation<SessionGraph>(
                    startDestination = RecorderDestination
                ) {
                    composable<RecorderDestination> { backStackEntry ->
                        val parentEntry = remember(backStackEntry) {
                            navController.getBackStackEntry<SessionGraph>()
                        }
                        val sessionGraph = parentEntry.toRoute<SessionGraph>()

                        RecorderScreen(
                            uri = sessionGraph.uri,
                            onNavigateBack = {
                                navController.popBackStack<LibraryDestination>(inclusive = false)
                            },
                            onNavigateToEditor = {
                                navController.navigate(EditorDestination)
                            }
                        )
                    }

                    composable<EditorDestination> { backStackEntry ->
                        val parentEntry = remember(backStackEntry) {
                            navController.getBackStackEntry<SessionGraph>()
                        }
                        val sessionGraph = parentEntry.toRoute<SessionGraph>()

                        SyncEditorScreen(
                            uri = sessionGraph.uri,
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onNavigateToRecorder = {
                                navController.popBackStack<RecorderDestination>(inclusive = false)
                            },
                            onFinish = {
                                navController.popBackStack<LibraryDestination>(inclusive = false)
                            }
                        )
                    }
                }
            }
        }
    }
}
