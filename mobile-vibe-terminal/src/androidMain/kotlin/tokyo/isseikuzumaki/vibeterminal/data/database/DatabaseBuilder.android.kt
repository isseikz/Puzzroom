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

fun getRoomDatabase(context: Context): AppDatabase {
    val dbFile = context.getDatabasePath("vibe_terminal.db")
    return Room.databaseBuilder<AppDatabase>(
        context = context.applicationContext,
        name = dbFile.absolutePath
    )
    .addMigrations(MIGRATION_2_3)
    .build()
}
