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

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val cuisine: String,
    val rarity: String,
    val mealSlots: String,
    val difficulty: Int,
    val timeMin: Int,
    val servings: Int,
    val ingredientsJson: String,
    val stepsJson: String,
    val tips: String,
    val commonFails: String
)

@Entity(tableName = "cook_logs")
data class CookLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: String,
    val recipeName: String,
    val cuisine: String,
    val rarity: String,
    val mealSlot: String,
    val dayKey: String,
    val cookedAt: Long,
    val photoPath: String?,
    val xpEarned: Int,
    val drawProbability: Double,
    val rerolls: Int,
    val difficulty: Int,
    val timeMin: Int,
    val photoSource: String?
)

@Entity(tableName = "ratings")
data class RatingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: String,
    val stars: Int,
    val comment: String,
    val createdAt: Long,
    val dayKey: String
)

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val level: Int = 1,
    val totalXp: Int = 0,
    val nickname: String = "掌勺人",
    val createdAt: Long = 0L
)

@Entity(tableName = "achievement_state")
data class AchievementStateEntity(
    @PrimaryKey val id: String,
    val unlockedAt: Long? = null,
    val progress: Int = 0
)

@Entity(tableName = "stats")
data class StatsEntity(
    @PrimaryKey val id: Int = 1,
    val countersJson: String = "{}",
    val updatedAt: Long = 0L
)

@Entity(tableName = "daily_quota")
data class DailyQuotaEntity(
    @PrimaryKey val dayKey: String,
    val breakfastUsed: Boolean = false,
    val lunchUsed: Boolean = false,
    val dinnerUsed: Boolean = false
)
