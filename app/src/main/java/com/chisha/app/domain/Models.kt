package com.chisha.app.domain

import androidx.compose.ui.graphics.Color
import com.chisha.app.ui.theme.RarityCommon
import com.chisha.app.ui.theme.RarityRare
import com.chisha.app.ui.theme.RaritySignature

/** 稀有度：标签永不变，只有权重会被评分拉低 */
enum class Rarity(val key: String, val label: String, val baseWeight: Double, val color: Color) {
    COMMON("common", "家常", 1.0, RarityCommon),
    SIGNATURE("signature", "大菜", 0.6, RaritySignature),
    RARE("rare", "稀有", 0.25, RarityRare);

    companion object {
        fun of(key: String?): Rarity = entries.firstOrNull { it.key == key } ?: COMMON
    }
}

/** 11 个菜系，total 是菜库里的道数，用来算「全清」类成就 */
enum class Cuisine(
    val key: String,
    val label: String,
    val total: Int,
    val color: Color
) {
    CHUAN("川菜", "川菜", 45, Color(0xFFC0392B)),
    YUE("粤菜", "粤菜", 45, Color(0xFF1F7A5C)),
    XIANG("湘菜", "湘菜", 30, Color(0xFFD64545)),
    LU("鲁菜", "鲁菜", 25, Color(0xFF8C6239)),
    SU("苏菜", "苏菜", 25, Color(0xFF4A7C9B)),
    ZHE("浙菜", "浙菜", 25, Color(0xFF3F8F7B)),
    MIN("闽菜", "闽菜", 20, Color(0xFFB5651D)),
    HUI("徽菜", "徽菜", 20, Color(0xFF6B5B4A)),
    HOME("家常菜", "家常菜", 50, Color(0xFFC8703C)),
    BREAKFAST("早餐面点", "早餐面点", 50, Color(0xFFD9A441)),
    SOUP("汤羹小吃", "汤羹小吃", 30, Color(0xFFA0522D));

    companion object {
        fun of(key: String?): Cuisine = entries.firstOrNull { it.key == key } ?: HOME
        val all: List<Cuisine> get() = entries.toList()
    }
}

enum class MealSlot(val key: String, val label: String) {
    BREAKFAST("breakfast", "早餐"),
    LUNCH("lunch", "午餐"),
    DINNER("dinner", "晚餐");

    companion object {
        fun of(key: String?): MealSlot =
            entries.firstOrNull { it.key == key } ?: MealSlot.LUNCH

        /** 按当前时间推荐一个名额 */
        fun suggest(hour: Int): MealSlot = when {
            hour < 10 -> BREAKFAST
            hour < 15 -> LUNCH
            else -> DINNER
        }
    }
}

data class Ingredient(
    val name: String,
    val amount: String,
    val note: String = "",
    /** 买回来怎么处理：洗法、切法、泡发、腌制…（v2 菜谱才有） */
    val prep: String = "",
    /** 在超市/菜场怎么挑这味食材（v2 菜谱才有，没有会去通用挑菜库里匹配） */
    val pick: String = "",
    /** 这味食材怎么切（v2 菜谱才有，没有会去通用切菜库里匹配） */
    val cut: String = ""
)

/** 炒菜流程的一行：按时间流走，每一步说清「做什么 + 多大火 + 多久 + 怎么判断」 */
data class HeatStep(
    val stage: String,
    val fire: String,
    val time: String,
    /** 这一步具体做什么（时间流的主体） */
    val detail: String = "",
    /** 怎么判断这一步到位了 */
    val signal: String = ""
)

/** 翻车对照表的一行 */
data class FailCase(val symptom: String, val fix: String)

data class Recipe(
    val id: String,
    val name: String,
    val cuisine: Cuisine,
    val rarity: Rarity,
    val mealSlots: List<MealSlot>,
    val difficulty: Int,
    val timeMin: Int,
    val servings: Int,
    val ingredients: List<Ingredient>,
    val steps: RecipeSteps,
    val tips: String,
    val commonFails: String,
    val heat: List<HeatStep> = emptyList(),
    val fails: List<FailCase> = emptyList()
) {
    val fitsBreakfast: Boolean get() = mealSlots.contains(MealSlot.BREAKFAST)
    val rarityLabel: String get() = rarity.label

    /** v2 加强版菜谱：有火候表或处理说明 */
    val isRich: Boolean
        get() = heat.isNotEmpty() || fails.isNotEmpty() || ingredients.any { it.prep.isNotBlank() }
}

data class RecipeSteps(
    val select: String,
    val wash: String,
    val cut: String,
    val blanch: String,
    val cook: String,
    val season: String
) {
    /** 教程页按这个顺序渲染六段 */
    val ordered: List<Pair<String, String>>
        get() = listOf(
            "选菜技巧" to select,
            "洗菜技巧" to wash,
            "切菜技巧" to cut,
            "焯水与预处理" to blanch,
            "下锅炒制" to cook,
            "调味收尾" to season
        )
}

/** 成就判定用的统计快照 */
data class StatSnapshot(
    val counters: Map<String, Int> = emptyMap()
) {
    operator fun get(key: String): Int = counters[key] ?: 0
}
