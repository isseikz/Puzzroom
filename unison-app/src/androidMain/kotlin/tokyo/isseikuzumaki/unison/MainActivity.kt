package tokyo.isseikuzumaki.unison

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext

/**
 * Local composition for file picker
 */
val LocalFilePicker = compositionLocalOf<FilePicker> {
    error("No FilePicker provided")
}

val LocalPermissionHandler = compositionLocalOf<PermissionHandler> {
    error("No PermissionHandler provided")
}

class MainActivity : ComponentActivity() {

    private lateinit var filePicker: FilePicker
    private lateinit var permissionHandler: PermissionHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize utilities
        filePicker = FilePicker(this)
        permissionHandler = PermissionHandler(this)

        // Initialize Koin if not already started
        if (GlobalContext.getOrNull() == null) {
            org.koin.core.context.startKoin {
                androidContext(this@MainActivity)
                modules(
                    tokyo.isseikuzumaki.unison.di.platformModule(),
                    tokyo.isseikuzumaki.unison.di.viewModelModule
                )
            }
        }

        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(
                LocalFilePicker provides filePicker,
                LocalPermissionHandler provides permissionHandler
            ) {
                // Remove when https://issuetracker.google.com/issues/364713509 is fixed
                LaunchedEffect(isSystemInDarkTheme()) {
                    enableEdgeToEdge()
                }
                App()
            }
        }
    }
}
