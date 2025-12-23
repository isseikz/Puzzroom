package tokyo.isseikuzumaki.vibeterminal.data.database

import android.content.Context
import androidx.room.Room

fun getRoomDatabase(context: Context): AppDatabase {
    val dbFile = context.getDatabasePath("vibe_terminal.db")
    return Room.databaseBuilder<AppDatabase>(
        context = context.applicationContext,
        name = dbFile.absolutePath
    ).build()
}
