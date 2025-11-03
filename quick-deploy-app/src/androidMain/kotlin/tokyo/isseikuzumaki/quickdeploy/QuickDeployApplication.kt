package tokyo.isseikuzumaki.quickdeploy

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

/**
 * Application class for Quick Deploy
 */
class QuickDeployApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        Log.d(TAG, "Quick Deploy Application initialized")
    }

    companion object {
        private const val TAG = "QuickDeployApp"
    }
}
