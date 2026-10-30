/*
 * 吃啥 · 离线单机做菜 App
 * Copyright (C) 2026 haogetop1
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
