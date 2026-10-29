package com.chisha.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        RecipeEntity::class,
        CookLogEntity::class,
        RatingEntity::class,
        ProfileEntity::class,
        AchievementStateEntity::class,
        StatsEntity::class,
        DailyQuotaEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun cookDao(): CookDao
    abstract fun ratingDao(): RatingDao
    abstract fun profileDao(): ProfileDao
    abstract fun achievementDao(): AchievementDao
    abstract fun statsDao(): StatsDao
    abstract fun quotaDao(): QuotaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "chisha.db"
            ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
        }
    }
}
