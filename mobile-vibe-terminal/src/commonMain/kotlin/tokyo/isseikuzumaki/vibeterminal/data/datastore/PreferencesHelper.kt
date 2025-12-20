package tokyo.isseikuzumaki.vibeterminal.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PreferencesHelper(private val dataStore: DataStore<Preferences>) {
    companion object {
        val FONT_SIZE = intPreferencesKey("font_size")
        const val DEFAULT_FONT_SIZE = 14
    }

    val fontSize: Flow<Int> = dataStore.data.map { preferences ->
        preferences[FONT_SIZE] ?: DEFAULT_FONT_SIZE
    }

    suspend fun setFontSize(size: Int) {
        dataStore.edit { preferences ->
            preferences[FONT_SIZE] = size
        }
    }
}
