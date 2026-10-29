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
