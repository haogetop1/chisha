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

package com.chisha.app.domain

import kotlin.math.roundToInt

/**
 * 经验与等级。
 *
 * 1→10 级累计 20240 经验。按 365 道菜的难度/稀有度分布实测：
 *   · 单次「裸经验」（基础+稀有加成）均值 127.5
 *   · 叠加「1/3 顿饭吃到首单 ×1.3」「1/3 天吃到全勤 +50」后 ≈ 156.9 / 次
 *   · 因此满级约 129 次，每天三顿全勤约 43 天；全新账号因每道菜首次 +30，约 107 次
 *
 * 注：这个速度比早期注释写的「约 170~190 次」快。没有调高 needByLevel 去凑那个数字，
 * 因为 levelFor 是从 totalXp 反推等级的——抬高阈值会让已有用户下次做菜直接掉级。
 * 真要拉长曲线，请走一次带数据迁移的版本升级，而不是改常量。
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
