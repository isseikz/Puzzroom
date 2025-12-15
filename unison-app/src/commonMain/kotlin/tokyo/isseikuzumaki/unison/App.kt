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
import tokyo.isseikuzumaki.unison.screens.recorder.RecorderScreenPlatform
import tokyo.isseikuzumaki.unison.screens.editor.SyncEditorScreen
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

/**
 * Navigation destinations for Unison app
 */
@Serializable
object LibraryDestination

@Serializable
data class SessionGraph(val audioUri: String, val transcriptionUri: String)

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

            AppNav(navController)
        }
    }
}
