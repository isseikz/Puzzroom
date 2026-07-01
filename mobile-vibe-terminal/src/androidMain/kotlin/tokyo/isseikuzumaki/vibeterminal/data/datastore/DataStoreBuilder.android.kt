package tokyo.isseikuzumaki.vibeterminal.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vibe_terminal_settings")

fun createDataStore(context: Context): DataStore<Preferences> {
    return context.dataStore
}

actual fun createDataStore(): DataStore<Preferences> {
    throw IllegalStateException("Use createDataStore(context) from Android code")
}
