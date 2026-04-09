package com.lifetracker.mobile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lifetracker.mobile.data.local.dao.HeroDao
import com.lifetracker.mobile.data.local.dao.TaskDao
import com.lifetracker.mobile.data.local.entity.HeroEntity
import com.lifetracker.mobile.data.local.entity.TaskEntity

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN syncError TEXT")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE heroes ADD COLUMN xpBoostPercent INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE heroes ADD COLUMN xpBoostTasksRemaining INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(
    entities = [HeroEntity::class, TaskEntity::class],
    version = 4,
    exportSchema = true
)

@TypeConverters(EnumConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun heroDao(): HeroDao
    abstract fun taskDao(): TaskDao
}

