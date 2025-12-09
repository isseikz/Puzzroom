package tokyo.isseikuzumaki.unison

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import tokyo.isseikuzumaki.unison.screens.shadowing.ShadowingScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Launch in demo mode with seek bar
            ShadowingScreen()
        }
    }
}
