package br.com.tairoroberto.filesencryptfiap.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context

@Database(entities = [(Item::class)], version = 15)
abstract class AppDatabase : RoomDatabase() {

    abstract fun itemsDao(): ItemsDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context?): AppDatabase =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: buildDatabase(context as Context).also { INSTANCE = it }
                }

        private fun buildDatabase(context: Context) =
                Room.databaseBuilder(context.applicationContext,
                        AppDatabase::class.java, "filesEncryptFiap.db")
                        .fallbackToDestructiveMigration()
                        .build()
    }
}
