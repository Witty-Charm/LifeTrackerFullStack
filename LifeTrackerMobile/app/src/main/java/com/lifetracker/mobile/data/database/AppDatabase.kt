package com.lifetracker.mobile.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lifetracker.mobile.data.Hero
import com.lifetracker.mobile.data.GameTask
import com.lifetracker.mobile.data.dao.HeroDao
import com.lifetracker.mobile.data.dao.TaskDao

@Database(
    entities = [Hero::class, GameTask::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun heroDao(): HeroDao
    abstract fun taskDao(): TaskDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        const val DATABASE_NAME = "lifetracker_db"
    }
}

