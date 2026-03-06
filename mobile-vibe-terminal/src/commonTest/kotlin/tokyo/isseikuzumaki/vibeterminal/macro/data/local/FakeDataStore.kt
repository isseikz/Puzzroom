package tokyo.isseikuzumaki.vibeterminal.macro.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Fake DataStore implementation for testing.
 */
class FakeDataStore : DataStore<Preferences> {
    private val _data = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = _data

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        val currentData = _data.value
        val newData = transform(currentData)
        _data.value = newData
        return newData
    }

    fun clear() {
        _data.value = emptyPreferences()
    }
}

/**
 * Extension to convert MutablePreferences to Preferences.
 */
private fun mutablePreferencesOf(vararg pairs: Preferences.Pair<*>): Preferences {
    return preferencesOf(*pairs)
}
