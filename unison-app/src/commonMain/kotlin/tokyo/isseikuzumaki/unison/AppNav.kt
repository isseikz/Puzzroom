package tokyo.isseikuzumaki.unison

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import tokyo.isseikuzumaki.unison.screens.editor.SyncEditorScreen
import tokyo.isseikuzumaki.unison.screens.library.LibraryScreenPlatform
import tokyo.isseikuzumaki.unison.screens.recorder.RecorderScreenPlatform

@Composable
fun AppNav(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = LibraryDestination
    ) {
        // Library Screen - Global scope, lightweight
        composable<LibraryDestination> {
            LibraryScreenPlatform(
                onNavigateToSession = { audioUri, transcriptionUri ->
                    navController.navigate(SessionGraph(audioUri, transcriptionUri))
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

                RecorderScreenPlatform(
                    audioUri = sessionGraph.audioUri,
                    transcriptionUri = sessionGraph.transcriptionUri,
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
                    audioUri = sessionGraph.audioUri,
                    transcriptionUri = sessionGraph.transcriptionUri,
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