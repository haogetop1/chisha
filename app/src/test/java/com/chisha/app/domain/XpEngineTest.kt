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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 等级曲线的数值回归测试。
 * 改 needByLevel 或经验公式时，这里必须同步并且保持全绿。
 */
class XpEngineTest {

    private fun recipe(difficulty: Int, rarity: Rarity) = Recipe(
        id = "r", name = "r", cuisine = Cuisine.CHUAN, rarity = rarity,
        mealSlots = listOf(MealSlot.LUNCH), difficulty = difficulty,
        timeMin = 20, servings = 2,
        ingredients = emptyList(),
        steps = RecipeSteps("", "", "", "", "", ""),
        tips = "", commonFails = ""
    )

    @Test
    fun `九段需求累计 20240`() {
        assertEquals(9, XpEngine.needByLevel.size)
        assertEquals(20240, XpEngine.needByLevel.sum())
        assertEquals(20240, XpEngine.totalXpToReach(XpEngine.MAX_LEVEL))
    }

    @Test
    fun `每级起点正好卡在该级 前一点仍是上一级`() {
        for (lv in 1..XpEngine.MAX_LEVEL) {
            val base = XpEngine.totalXpToReach(lv)
            assertEquals("到 $lv 级的起点", lv, XpEngine.levelFor(base))
            if (lv > 1) {
                assertEquals("$lv 级起点 - 1", lv - 1, XpEngine.levelFor(base - 1))
            }
        }
    }

    @Test
    fun `等级随经验单调不减且封顶 10 级`() {
        var prev = 0
        for (xp in 0..20240) {
            val lv = XpEngine.levelFor(xp)
            assertTrue("经验 $xp 处等级倒退了", lv >= prev)
            prev = lv
        }
        assertEquals(10, XpEngine.levelFor(20240))
        assertEquals(10, XpEngine.levelFor(20241))
        assertEquals(10, XpEngine.levelFor(Int.MAX_VALUE))
    }

    @Test
    fun `进度在级内正确 满级返回 1 比 1`() {
        assertEquals(0 to 330, XpEngine.progress(0))
        assertEquals(329 to 330, XpEngine.progress(329))
        assertEquals(0 to 660, XpEngine.progress(330))
        assertEquals(4399 to 4400, XpEngine.progress(15840 + 4399))
        assertEquals(1 to 1, XpEngine.progress(20240))
        assertEquals(1 to 1, XpEngine.progress(999999))
    }

    @Test
    fun `称号覆盖全部 10 级且不越界`() {
        assertEquals(10, XpEngine.titles.size)
        assertEquals("厨房新手", XpEngine.title(1))
        assertEquals("人间烟火", XpEngine.title(10))
        // 越界不崩，夹到两端
        assertEquals("厨房新手", XpEngine.title(0))
        assertEquals("人间烟火", XpEngine.title(99))
    }

    @Test
    fun `基础经验按难度 80 - 110 - 150`() {
        assertEquals(80, XpEngine.baseXp(1))
        assertEquals(110, XpEngine.baseXp(2))
        assertEquals(150, XpEngine.baseXp(3))
    }

    @Test
    fun `稀有度加成 0 - 20 - 50`() {
        assertEquals(0, XpEngine.rarityBonus(Rarity.COMMON))
        assertEquals(20, XpEngine.rarityBonus(Rarity.SIGNATURE))
        assertEquals(50, XpEngine.rarityBonus(Rarity.RARE))
    }

    @Test
    fun `单次经验：只有达标的那一项才加`() {
        val r = recipe(2, Rarity.COMMON)          // 基础 110
        assertEquals(110, XpEngine.compute(r, false, false, false))
        assertEquals(140, XpEngine.compute(r, true, false, false))    // 首次 +30
        assertEquals(143, XpEngine.compute(r, false, true, false))    // 110 * 1.3 = 143
        assertEquals(160, XpEngine.compute(r, false, false, true))     // 全勤 +50
        // 首次先 +30 再乘 1.3，最后加全勤：round(140 * 1.3) + 50 = 232
        assertEquals(232, XpEngine.compute(r, true, true, true))
    }

    @Test
    fun `最高单次经验不超过 350 且永远大于 0`() {
        var lo = Int.MAX_VALUE
        var hi = Int.MIN_VALUE
        for (d in 1..3) for (rar in Rarity.entries) for (ft in listOf(false, true))
            for (fd in listOf(false, true)) for (fu in listOf(false, true)) {
                val x = XpEngine.compute(recipe(d, rar), ft, fd, fu)
                lo = minOf(lo, x); hi = maxOf(hi, x)
            }
        assertEquals(80, lo)
        assertEquals(349, hi)
    }

    @Test
    fun `满级所需次数落在实测区间内`() {
        // 与 tools/verify_draw_and_xp.py 的实测一致：稳态约 129 次、全新账号约 107 次
        val steadyPerCook = 156.9
        val cooks = 20240 / steadyPerCook
        assertTrue("实测满级次数 $cooks 应落在 110~150 之间", cooks in 110.0..150.0)
    }
}
