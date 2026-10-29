package com.chisha.app.data

import android.content.Context
import com.chisha.app.domain.AchievementDef
import com.chisha.app.domain.CalendarTable
import com.chisha.app.domain.Cuisine
import com.chisha.app.domain.FailCase
import com.chisha.app.domain.HeatStep
import com.chisha.app.domain.Ingredient
import com.chisha.app.domain.MealSlot
import com.chisha.app.domain.Rarity
import com.chisha.app.domain.Recipe
import com.chisha.app.domain.RecipeSteps
import org.json.JSONArray
import org.json.JSONObject
import java.util.zip.CRC32

/** 首次启动把 assets 里的菜谱与成就灌进 Room */
object SeedLoader {

    suspend fun seed(context: Context, db: AppDatabase) {
        CalendarTable.load(context)
        DishPhotos.load(context)
        PickingTips.load(context)
        CuttingTips.load(context)
        seedRecipes(context, db)
        seedAchievements(context, db)
        if (db.profileDao().get() == null) {
            db.profileDao().upsert(ProfileEntity(createdAt = System.currentTimeMillis()))
        }
    }

    private const val PREF = "chisha_seed"
    private const val KEY_RECIPE_HASH = "recipes_hash"

    private suspend fun seedRecipes(context: Context, db: AppDatabase) {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val text = context.assets.open("recipes.json").bufferedReader().use { it.readText() }
        // 用菜谱内容的指纹判断要不要重灌：改了 assets/recipes.json 就会自动覆盖，
        // 不需要手动维护版本号，老用户升级也能拿到新菜谱
        val crc = CRC32().apply { update(text.toByteArray(Charsets.UTF_8)) }.value.toString()
        if (db.recipeDao().count() > 0 && prefs.getString(KEY_RECIPE_HASH, null) == crc) return
        val arr = JSONArray(text)
        val rows = ArrayList<RecipeEntity>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val slots = o.optJSONArray("mealSlots")?.let { a ->
                (0 until a.length()).map { a.getString(it) }
            } ?: listOf("lunch", "dinner")
            // 火候表与翻车对照表塞进 stepsJson，省一次数据库迁移
            val stepsObj = o.optJSONObject("steps") ?: JSONObject()
            o.optJSONArray("heat")?.let { stepsObj.put("heat", it) }
            o.optJSONArray("fails")?.let { stepsObj.put("fails", it) }
            rows.add(
                RecipeEntity(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    cuisine = o.getString("cuisine"),
                    rarity = o.optString("rarity", "common"),
                    mealSlots = slots.joinToString(","),
                    difficulty = o.optInt("difficulty", 2),
                    timeMin = o.optInt("timeMin", 30),
                    servings = o.optInt("servings", 2),
                    ingredientsJson = o.optJSONArray("ingredients")?.toString() ?: "[]",
                    stepsJson = stepsObj.toString(),
                    tips = o.optString("tips", ""),
                    commonFails = o.optString("commonFails", "")
                )
            )
        }
        db.recipeDao().upsertAll(rows)
        prefs.edit().putString(KEY_RECIPE_HASH, crc).apply()
    }

    /**
     * 幂等补齐：只插入缺失的成就定义，不动已有进度。
     * 这样后续版本新增成就是「升级」，不会因为 count>0 就跳过初始化而让老用户永远看不到新成就。
     */
    private suspend fun seedAchievements(context: Context, db: AppDatabase) {
        val defs = AchievementDef.load(context)
        val existing = db.achievementDao().all().map { it.id }.toSet()
        val missing = defs
            .filter { it.id !in existing }
            .map { AchievementStateEntity(id = it.id, progress = 0, unlockedAt = null) }
        if (missing.isNotEmpty()) {
            db.achievementDao().upsertAll(missing)
        }
    }

    // ---- Entity -> 领域模型 ----

    fun toRecipe(e: RecipeEntity): Recipe {
        val parsed = parseSteps(e.stepsJson)
        return Recipe(
            id = e.id,
            name = e.name,
            cuisine = Cuisine.of(e.cuisine),
            rarity = Rarity.of(e.rarity),
            mealSlots = e.mealSlots.split(",").filter { it.isNotBlank() }.map { MealSlot.of(it) },
            difficulty = e.difficulty,
            timeMin = e.timeMin,
            servings = e.servings,
            ingredients = parseIngredients(e.ingredientsJson),
            steps = parsed.first,
            tips = e.tips,
            commonFails = e.commonFails,
            heat = parsed.second,
            fails = parsed.third
        )
    }

    private fun parseIngredients(text: String): List<Ingredient> = runCatching {
        val arr = JSONArray(text)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Ingredient(
                name = o.optString("name"),
                amount = o.optString("amount"),
                note = o.optString("note"),
                prep = o.optString("prep"),
                pick = o.optString("pick"),
                cut = o.optString("cut")
            )
        }
    }.getOrElse { emptyList() }

    private fun parseSteps(text: String): Triple<RecipeSteps, List<HeatStep>, List<FailCase>> = runCatching {
        val o = JSONObject(text)
        val steps = RecipeSteps(
            select = o.optString("select"),
            wash = o.optString("wash"),
            cut = o.optString("cut"),
            blanch = o.optString("blanch"),
            cook = o.optString("cook"),
            season = o.optString("season")
        )
        val heat = o.optJSONArray("heat")?.let { a ->
            (0 until a.length()).mapNotNull { i ->
                val h = a.optJSONObject(i) ?: return@mapNotNull null
                HeatStep(
                    stage = h.optString("stage"),
                    fire = h.optString("fire"),
                    time = h.optString("time"),
                    detail = h.optString("detail"),
                    signal = h.optString("signal")
                )
            }
        } ?: emptyList()
        val fails = o.optJSONArray("fails")?.let { a ->
            (0 until a.length()).mapNotNull { i ->
                val f = a.optJSONObject(i) ?: return@mapNotNull null
                FailCase(symptom = f.optString("symptom"), fix = f.optString("fix"))
            }
        } ?: emptyList()
        Triple(steps, heat, fails)
    }.getOrElse { Triple(RecipeSteps("", "", "", "", "", ""), emptyList(), emptyList()) }
}
