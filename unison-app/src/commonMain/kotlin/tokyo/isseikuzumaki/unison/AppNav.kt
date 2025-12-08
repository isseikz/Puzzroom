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
import tokyo.isseikuzumaki.unison.screens.shadowing.ShadowingScreen

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
                onNavigateToSession = { uri ->
                    navController.navigate(SessionGraph(uri))
                }
            )
        }

        // Session Graph - Scoped navigation for heavy audio operations
        navigation<SessionGraph>(
            startDestination = ShadowingDestination
        ) {
            composable<ShadowingDestination> { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry<SessionGraph>()
                }
                val sessionGraph = parentEntry.toRoute<SessionGraph>()

                ShadowingScreen(
                    uri = sessionGraph.uri,
                    onNavigateBack = {
                        navController.popBackStack<LibraryDestination>(inclusive = false)
                    }
                )
            }

            composable<RecorderDestination> { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry<SessionGraph>()
                }
                val sessionGraph = parentEntry.toRoute<SessionGraph>()

                RecorderScreenPlatform(
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