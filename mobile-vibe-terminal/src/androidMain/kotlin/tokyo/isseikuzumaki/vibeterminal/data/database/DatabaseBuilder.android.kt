package tokyo.isseikuzumaki.vibeterminal.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add new columns with default values
        db.execSQL("ALTER TABLE server_connections ADD COLUMN startupCommand TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE server_connections ADD COLUMN isAutoReconnect INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE server_connections ADD COLUMN monitorFilePath TEXT DEFAULT NULL")
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE server_connections ADD COLUMN lastFileExplorerPath TEXT DEFAULT NULL")
    }
}

fun getRoomDatabase(context: Context): AppDatabase {
    val dbFile = context.getDatabasePath("vibe_terminal.db")
    return Room.databaseBuilder<AppDatabase>(
        context = context.applicationContext,
        name = dbFile.absolutePath
    )
    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
    .build()
}
