package tokyo.isseikuzumaki.vibeterminal.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

expect fun createDataStore(): DataStore<Preferences>
