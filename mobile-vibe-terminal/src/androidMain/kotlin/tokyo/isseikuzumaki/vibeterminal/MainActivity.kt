package tokyo.isseikuzumaki.vibeterminal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cafe.adriel.voyager.navigator.Navigator
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import tokyo.isseikuzumaki.vibeterminal.di.appModule
import tokyo.isseikuzumaki.vibeterminal.di.dataModule
import tokyo.isseikuzumaki.vibeterminal.di.platformModule
import tokyo.isseikuzumaki.vibeterminal.ui.screens.ConnectionListScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Apache SSHD for Android (must be done before any SSHD classes are loaded)
        org.apache.sshd.common.util.io.PathUtils.setUserHomeFolderResolver {
            // Use app's files directory as fake home folder
            filesDir.toPath()
        }

        // Initialize Koin only if not already started
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(this@MainActivity)
                modules(appModule, dataModule, platformModule())
            }
        }

        enableEdgeToEdge()
        setContent {
            Navigator(ConnectionListScreen())
        }
    }
}
