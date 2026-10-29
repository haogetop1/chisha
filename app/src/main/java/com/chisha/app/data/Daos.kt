package com.chisha.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes")
    suspend fun all(): List<RecipeEntity>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun byId(id: String): RecipeEntity?

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<RecipeEntity>)

    /** 菜谱内容升级后按 id 覆盖，评分与做菜记录不受影响 */
    @Upsert
    suspend fun upsertAll(items: List<RecipeEntity>)
}

@Dao
interface CookDao {
    @Insert
    suspend fun insert(log: CookLogEntity): Long

    @Query("SELECT * FROM cook_logs ORDER BY cookedAt DESC")
    suspend fun all(): List<CookLogEntity>

    @Query("SELECT * FROM cook_logs ORDER BY cookedAt DESC")
    fun observeAll(): Flow<List<CookLogEntity>>

    @Query("SELECT * FROM cook_logs WHERE dayKey = :dayKey")
    suspend fun byDay(dayKey: String): List<CookLogEntity>

    @Query("SELECT COUNT(*) FROM cook_logs")
    suspend fun count(): Int

    @Query("SELECT DISTINCT recipeId FROM cook_logs")
    suspend fun distinctRecipeIds(): List<String>

    @Query("SELECT * FROM cook_logs WHERE recipeId = :recipeId ORDER BY cookedAt ASC")
    suspend fun byRecipe(recipeId: String): List<CookLogEntity>
}

@Dao
interface RatingDao {
    @Insert
    suspend fun insert(rating: RatingEntity): Long

    @Query("SELECT * FROM ratings ORDER BY createdAt DESC")
    suspend fun all(): List<RatingEntity>

    @Query("SELECT * FROM ratings ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RatingEntity>>

    @Query("SELECT * FROM ratings WHERE recipeId = :recipeId ORDER BY createdAt DESC LIMIT 1")
    suspend fun latestFor(recipeId: String): RatingEntity?

    @Query("SELECT * FROM ratings WHERE recipeId = :recipeId ORDER BY createdAt ASC")
    suspend fun historyFor(recipeId: String): List<RatingEntity>
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun get(): ProfileEntity?

    @Upsert
    suspend fun upsert(profile: ProfileEntity)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievement_state")
    suspend fun all(): List<AchievementStateEntity>

    @Query("SELECT * FROM achievement_state")
    fun observeAll(): Flow<List<AchievementStateEntity>>

    @Upsert
    suspend fun upsertAll(items: List<AchievementStateEntity>)

    @Query("SELECT COUNT(*) FROM achievement_state")
    suspend fun count(): Int
}

@Dao
interface StatsDao {
    @Query("SELECT * FROM stats WHERE id = 1")
    suspend fun get(): StatsEntity?

    @Upsert
    suspend fun upsert(stats: StatsEntity)
}

@Dao
interface QuotaDao {
    @Query("SELECT * FROM daily_quota WHERE dayKey = :dayKey")
    suspend fun get(dayKey: String): DailyQuotaEntity?

    @Upsert
    suspend fun upsert(quota: DailyQuotaEntity)

    @Query("SELECT * FROM daily_quota ORDER BY dayKey DESC LIMIT 60")
    suspend fun recent(): List<DailyQuotaEntity>
}
