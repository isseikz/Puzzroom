package tokyo.isseikuzumaki.vibeterminal

import android.app.Application
import timber.log.Timber

class VibeTerminalApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Timber
        Timber.plant(Timber.DebugTree())
    }
}
