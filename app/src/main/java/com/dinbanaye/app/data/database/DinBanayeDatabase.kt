package com.dinbanaye.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dinbanaye.app.data.model.CategoryEntity
import com.dinbanaye.app.data.model.TaskEntity

@TypeConverters(Converters::class)
@Database(
    entities = [
        TaskEntity::class,
        CategoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DinBanayeDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: DinBanayeDatabase? = null

        fun getDatabase(context: Context): DinBanayeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DinBanayeDatabase::class.java,
                    "dinbanaye_database"
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }
}
