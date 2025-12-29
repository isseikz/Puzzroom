package tokyo.isseikuzumaki.vibeterminal.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import tokyo.isseikuzumaki.vibeterminal.data.database.dao.ServerConnectionDao
import tokyo.isseikuzumaki.vibeterminal.data.database.entity.ServerConnection

@Database(
    entities = [ServerConnection::class],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverConnectionDao(): ServerConnectionDao
}
