package com.lifetracker.mobile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lifetracker.mobile.data.local.dao.HeroDao
import com.lifetracker.mobile.data.local.dao.TaskDao
import com.lifetracker.mobile.data.local.entity.HeroEntity
import com.lifetracker.mobile.data.local.entity.TaskEntity

@Database(
    entities = [
        HeroEntity::class,
        TaskEntity::class,
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun heroDao(): HeroDao
    abstract fun taskDao(): TaskDao
}

