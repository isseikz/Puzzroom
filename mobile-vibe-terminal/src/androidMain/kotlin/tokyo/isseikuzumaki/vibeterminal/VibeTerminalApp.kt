package tokyo.isseikuzumaki.vibeterminal

import android.app.Application
import android.content.Context
import timber.log.Timber

class VibeTerminalApp : Application() {

    companion object {
        lateinit var applicationContext: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        Companion.applicationContext = this

        // Initialize Timber
        Timber.plant(Timber.DebugTree())
    }
}
