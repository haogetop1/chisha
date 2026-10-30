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

package com.chisha.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chisha.app.data.AchievementStateEntity
import com.chisha.app.data.AppDatabase
import com.chisha.app.data.CookLogEntity
import com.chisha.app.data.DailyQuotaEntity
import com.chisha.app.data.ProfileEntity
import com.chisha.app.data.RatingEntity
import com.chisha.app.data.SeedLoader
import com.chisha.app.domain.AchievementDef
import com.chisha.app.domain.AchievementEngine
import com.chisha.app.domain.CalendarTable
import com.chisha.app.domain.Cuisine
import com.chisha.app.domain.DrawEngine
import com.chisha.app.domain.DrawOutcome
import com.chisha.app.domain.MealSlot
import com.chisha.app.domain.Recipe
import com.chisha.app.domain.StatsBuilder
import com.chisha.app.domain.XpEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DrawSession(
    val slot: MealSlot,
    val cuisine: Cuisine?,
    val outcome: DrawOutcome,
    val rerolls: Int,
    val poolSize: Int,
    /** 每真正抽一次 +1，用来判断老虎机是否需要重播动画 */
    val seq: Int = 0,
    /** 本次抽奖的滚动动画是否已经播过（从菜谱页返回时不该重播） */
    val animated: Boolean = false
)

data class CookResult(
    val recipe: Recipe,
    val slot: MealSlot,
    val xpEarned: Int,
    val levelBefore: Int,
    val levelAfter: Int,
    val totalXp: Int,
    val firstOfDay: Boolean,
    val fullDay: Boolean,
    val isNewRecipe: Boolean,
    val unlocked: List<AchievementDef>
)

data class AppUiState(
    val loading: Boolean = true,
    val recipes: List<Recipe> = emptyList(),
    val profile: ProfileEntity = ProfileEntity(),
    val quota: DailyQuotaEntity = DailyQuotaEntity(StatsBuilder.dayKey(System.currentTimeMillis())),
    val todayLogs: List<CookLogEntity> = emptyList(),
    val allLogs: List<CookLogEntity> = emptyList(),
    val ratings: List<RatingEntity> = emptyList(),
    val session: DrawSession? = null,
    val lastResult: CookResult? = null,
    val achievementDefs: List<AchievementDef> = emptyList(),
    val achievementStates: Map<String, AchievementStateEntity> = emptyMap(),
    val counters: Map<String, Int> = emptyMap(),
    val lowRatings: Map<String, Int> = emptyMap(),
    val todayTerm: String? = null,
    val todayFestival: String? = null,
    val message: String? = null
) {
    val usedSlots: Set<MealSlot>
        get() = buildSet {
            if (quota.breakfastUsed) add(MealSlot.BREAKFAST)
            if (quota.lunchUsed) add(MealSlot.LUNCH)
            if (quota.dinnerUsed) add(MealSlot.DINNER)
        }

    val unlockedCount: Int get() = achievementStates.values.count { it.unlockedAt != null }
    val collectionCount: Int get() = allLogs.map { it.recipeId }.distinct().size
    val todayXp: Int get() = todayLogs.sumOf { it.xpEarned }
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)

    var state by mutableStateOf(AppUiState())
        private set

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                SeedLoader.seed(app, db)
            }
            refresh()
        }
    }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        val dayKey = StatsBuilder.dayKey(System.currentTimeMillis())
        CalendarTable.load(getApplication())
        val recipes = db.recipeDao().all().map { SeedLoader.toRecipe(it) }
        val profile = db.profileDao().get() ?: ProfileEntity()
        val quota = db.quotaDao().get(dayKey) ?: DailyQuotaEntity(dayKey)
        val todayLogs = db.cookDao().byDay(dayKey)
        val allLogs = db.cookDao().all()
        val ratings = db.ratingDao().all()
        val defs = AchievementDef.load(getApplication())
        val states = db.achievementDao().all().associateBy { it.id }
        val snapshot = StatsBuilder.build(db)
        val low = StatsBuilder.lowRatingCounts(db)
        val (todayTerm, todayFestival) = CalendarTable.todayInfo(dayKey)
        withContext(Dispatchers.Main) {
            state = state.copy(
                loading = false,
                recipes = recipes,
                profile = profile,
                quota = quota,
                todayLogs = todayLogs,
                allLogs = allLogs,
                ratings = ratings,
                achievementDefs = defs,
                achievementStates = states,
                counters = snapshot.counters,
                lowRatings = low,
                todayTerm = todayTerm,
                todayFestival = todayFestival
            )
        }
    }

    fun message(text: String) {
        state = state.copy(message = text)
    }

    fun clearMessage() {
        state = state.copy(message = null)
    }

    fun nextSlot(): MealSlot? = MealSlot.entries.firstOrNull { it !in state.usedSlots }

    fun canCook(): Boolean = nextSlot() != null

    // ---------------- 抽菜 ----------------

    fun draw(slot: MealSlot, cuisine: Cuisine?, rerolls: Int = 0, excludeId: String? = null) {
        val pool = DrawEngine.pool(state.recipes, cuisine, slot)
        val outcome = DrawEngine.draw(pool, state.lowRatings, excludeId)
        if (outcome == null) {
            state = state.copy(message = "这个池子里没有可抽的菜了")
            return
        }
        state = state.copy(
            session = DrawSession(
                slot = slot,
                cuisine = cuisine,
                outcome = outcome,
                rerolls = rerolls,
                poolSize = pool.size,
                seq = (state.session?.seq ?: 0) + 1,
                animated = false
            )
        )
    }

    /** 老虎机动画播完标记一下，返回再进时直接显示结果 */
    fun markSessionAnimated() {
        val s = state.session ?: return
        if (s.animated) return
        state = state.copy(session = s.copy(animated = true))
    }

    fun reroll() {
        val s = state.session ?: return
        draw(s.slot, s.cuisine, s.rerolls + 1, s.outcome.recipe.id)
    }

    fun clearSession() {
        state = state.copy(session = null)
    }

    fun clearResult() {
        state = state.copy(lastResult = null)
    }

    // ---------------- 结算 ----------------

    fun completeCooking(photoFileName: String?, photoSource: String?, onDone: (CookResult?) -> Unit) {
        val session = state.session
        if (session == null) {
            onDone(null)
            return
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                commitCook(session, photoFileName, photoSource)
            }
            state = state.copy(session = null, lastResult = result)
            refresh()
            onDone(result)
        }
    }

    private suspend fun commitCook(
        session: DrawSession,
        photoFileName: String?,
        photoSource: String?
    ): CookResult {
        val now = System.currentTimeMillis()
        val dayKey = StatsBuilder.dayKey(now)
        val recipe = session.outcome.recipe

        // 一次性奖励按「有成品图」的口径算：没拍照的完成只进图鉴，不占用首单 / 首次额度
        val todayPhotoLogs = db.cookDao().byDayWithPhoto(dayKey)
        val firstOfDay = todayPhotoLogs.isEmpty()
        val isNewRecipe = db.cookDao().photoCountByRecipe(recipe.id) == 0

        val quota = db.quotaDao().get(dayKey) ?: DailyQuotaEntity(dayKey)
        val newQuota = when (session.slot) {
            MealSlot.BREAKFAST -> quota.copy(breakfastUsed = true)
            MealSlot.LUNCH -> quota.copy(lunchUsed = true)
            MealSlot.DINNER -> quota.copy(dinnerUsed = true)
        }
        val fullDay = newQuota.breakfastUsed && newQuota.lunchUsed && newQuota.dinnerUsed

        val profileBefore = db.profileDao().get() ?: ProfileEntity()
        // 没有成品图就只是「已完成（无图）」，不给经验
        val xp = if (photoFileName == null) 0 else XpEngine.compute(recipe, isNewRecipe, firstOfDay, fullDay)
        val newTotal = profileBefore.totalXp + xp
        val levelAfter = XpEngine.levelFor(newTotal)

        db.cookDao().insert(
            CookLogEntity(
                recipeId = recipe.id,
                recipeName = recipe.name,
                cuisine = recipe.cuisine.key,
                rarity = recipe.rarity.key,
                mealSlot = session.slot.key,
                dayKey = dayKey,
                cookedAt = now,
                photoPath = photoFileName,
                xpEarned = xp,
                drawProbability = session.outcome.probability,
                rerolls = session.rerolls,
                difficulty = recipe.difficulty,
                timeMin = recipe.timeMin,
                photoSource = photoSource
            )
        )
        db.quotaDao().upsert(newQuota)
        db.profileDao().upsert(profileBefore.copy(totalXp = newTotal, level = levelAfter))

        val unlocked = syncAchievements()

        return CookResult(
            recipe = recipe,
            slot = session.slot,
            xpEarned = xp,
            levelBefore = profileBefore.level,
            levelAfter = levelAfter,
            totalXp = newTotal,
            firstOfDay = firstOfDay,
            fullDay = fullDay,
            isNewRecipe = isNewRecipe,
            unlocked = unlocked
        )
    }

    /** 重算计数器并解锁成就，返回本次新解锁的成就 */
    private suspend fun syncAchievements(): List<AchievementDef> {
        val defs = AchievementDef.load(getApplication())
        val states = db.achievementDao().all().associateBy { it.id }
        val unlockedIds = states.values.filter { it.unlockedAt != null }.map { it.id }.toSet()
        val snapshot = StatsBuilder.build(db)
        val eval = AchievementEngine.evaluate(defs, snapshot, unlockedIds)

        val now = System.currentTimeMillis()
        val updated = defs.map { def ->
            val prev = states[def.id]
            val isNew = eval.newlyUnlocked.any { it.id == def.id }
            AchievementStateEntity(
                id = def.id,
                unlockedAt = prev?.unlockedAt ?: if (isNew) now else null,
                progress = eval.progress[def.id] ?: 0
            )
        }
        db.achievementDao().upsertAll(updated)
        return eval.newlyUnlocked
    }

    // ---------------- 评价 ----------------

    fun submitRating(recipeId: String, stars: Int, comment: String, onDone: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                db.ratingDao().insert(
                    RatingEntity(
                        recipeId = recipeId,
                        stars = stars,
                        comment = comment.trim(),
                        createdAt = now,
                        dayKey = StatsBuilder.dayKey(now)
                    )
                )
                syncAchievements()
            }
            refresh()
            onDone()
        }
    }

    fun ratingFor(recipeId: String): RatingEntity? =
        state.ratings.firstOrNull { it.recipeId == recipeId }

    fun setNickname(name: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val p = db.profileDao().get() ?: ProfileEntity()
                db.profileDao().upsert(p.copy(nickname = name.ifBlank { "掌勺人" }))
            }
            refresh()
        }
    }

    fun recipeById(id: String): Recipe? = state.recipes.firstOrNull { it.id == id }
}
