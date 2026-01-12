package tokyo.isseikuzumaki.magicdeploy

import android.content.Context
import androidx.startup.Initializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MagicDeployInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            MagicDeployServer(context).start()
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}
