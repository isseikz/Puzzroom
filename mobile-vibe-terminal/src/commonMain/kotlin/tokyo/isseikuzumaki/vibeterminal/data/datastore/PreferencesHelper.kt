package tokyo.isseikuzumaki.vibeterminal.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PreferencesHelper(private val dataStore: DataStore<Preferences>) {
    companion object {
        val FONT_SIZE = intPreferencesKey("font_size")
        val AUTO_INSTALL_ENABLED = booleanPreferencesKey("auto_install_enabled")
        const val DEFAULT_FONT_SIZE = 14
        const val DEFAULT_AUTO_INSTALL_ENABLED = false
    }

    val fontSize: Flow<Int> = dataStore.data.map { preferences ->
        preferences[FONT_SIZE] ?: DEFAULT_FONT_SIZE
    }

    suspend fun setFontSize(size: Int) {
        dataStore.edit { preferences ->
            preferences[FONT_SIZE] = size
        }
    }

    /**
     * 自動インストールが有効かどうかを返す Flow
     */
    val autoInstallEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[AUTO_INSTALL_ENABLED] ?: DEFAULT_AUTO_INSTALL_ENABLED
    }

    /**
     * 自動インストールの有効/無効を設定する
     */
    suspend fun setAutoInstallEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_INSTALL_ENABLED] = enabled
        }
    }
}
