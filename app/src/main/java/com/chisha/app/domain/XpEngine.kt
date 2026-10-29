package com.chisha.app.domain

import kotlin.math.roundToInt

/**
 * 经验与等级。
 * 1→10 级累计 20240 经验，按平均每次约 110~120 经验，约 170~190 次完成满级。
 */
object XpEngine {

    const val MAX_LEVEL = 10

    /** 升到下一级所需的经验，索引 0 表示 1→2 */
    val needByLevel = intArrayOf(330, 660, 1100, 1650, 2200, 2750, 3300, 3850, 4400)

    val titles = listOf(
        "厨房新手", "打下手", "灶台学徒", "家常掌勺", "火候初成",
        "调味有度", "一桌好菜", "灶前老手", "私房主厨", "人间烟火"
    )

    fun title(level: Int): String = titles[(level - 1).coerceIn(0, titles.lastIndex)]

    /** 达到 level 级所需的累计经验 */
    fun totalXpToReach(level: Int): Int {
        if (level <= 1) return 0
        if (level > MAX_LEVEL) return Int.MAX_VALUE
        return needByLevel.take(level - 1).sum()
    }

    fun levelFor(totalXp: Int): Int {
        var lv = 1
        var acc = 0
        for (need in needByLevel) {
            if (totalXp >= acc + need) {
                acc += need
                lv++
            } else break
        }
        return lv.coerceAtMost(MAX_LEVEL)
    }

    /** 当前等级内的 (已获得, 本级需要) */
    fun progress(totalXp: Int): Pair<Int, Int> {
        val lv = levelFor(totalXp)
        if (lv >= MAX_LEVEL) return 1 to 1
        val base = totalXpToReach(lv)
        return (totalXp - base) to needByLevel[lv - 1]
    }

    fun baseXp(difficulty: Int): Int = when (difficulty) {
        1 -> 80
        3 -> 150
        else -> 110
    }

    fun rarityBonus(rarity: Rarity): Int = when (rarity) {
        Rarity.COMMON -> 0
        Rarity.SIGNATURE -> 20
        Rarity.RARE -> 50
    }

    /**
     * @param firstTime 第一次做这道菜
     * @param firstOfDay 当天第一道完成的菜
     * @param fullDay 当天早中晚三个名额全部完成
     */
    fun compute(
        recipe: Recipe,
        firstTime: Boolean,
        firstOfDay: Boolean,
        fullDay: Boolean
    ): Int {
        var xp = baseXp(recipe.difficulty) + rarityBonus(recipe.rarity)
        if (firstTime) xp += 30
        if (firstOfDay) xp = (xp * 1.3).roundToInt()
        if (fullDay) xp += 50
        return xp
    }
}
