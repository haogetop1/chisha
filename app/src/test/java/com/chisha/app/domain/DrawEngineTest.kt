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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * 抽取概率的数值回归测试。
 * 这些是改权重、改惩罚系数时唯一会报警的东西——不要删。
 */
class DrawEngineTest {

    private fun recipe(
        id: String,
        rarity: Rarity,
        cuisine: Cuisine = Cuisine.CHUAN,
        slots: List<MealSlot> = listOf(MealSlot.LUNCH, MealSlot.DINNER)
    ) = Recipe(
        id = id, name = id, cuisine = cuisine, rarity = rarity, mealSlots = slots,
        difficulty = 2, timeMin = 20, servings = 2,
        ingredients = emptyList(),
        steps = RecipeSteps("", "", "", "", "", ""),
        tips = "", commonFails = ""
    )

    @Test
    fun `档位基础权重是 1 - 0_6 - 0_25`() {
        assertEquals(1.0, Rarity.COMMON.baseWeight, 1e-12)
        assertEquals(0.6, Rarity.SIGNATURE.baseWeight, 1e-12)
        assertEquals(0.25, Rarity.RARE.baseWeight, 1e-12)
    }

    @Test
    fun `评分惩罚按 0_6 的幂衰减并卡在 0_15 下限`() {
        val r = recipe("c1", Rarity.COMMON)
        assertEquals(1.0, DrawEngine.weightOf(r, 0), 1e-12)
        assertEquals(0.6, DrawEngine.weightOf(r, 1), 1e-12)
        assertEquals(0.36, DrawEngine.weightOf(r, 2), 1e-12)
        assertEquals(0.216, DrawEngine.weightOf(r, 3), 1e-12)
        // 0.6^4 = 0.1296 已低于下限，被抬到 0.15
        assertEquals(0.15, DrawEngine.weightOf(r, 4), 1e-12)
        assertEquals(0.15, DrawEngine.weightOf(r, 50), 1e-12)
    }

    @Test
    fun `池内概率严格归一`() {
        val pool = listOf(
            recipe("a", Rarity.COMMON), recipe("b", Rarity.COMMON),
            recipe("c", Rarity.SIGNATURE), recipe("d", Rarity.RARE)
        )
        val p = DrawEngine.probabilities(pool, emptyMap())
        // 两道家常 1.0 + 一道大菜 0.6 + 一道稀有 0.25 = 2.85
        val totalW = 1.0 + 1.0 + 0.6 + 0.25
        assertEquals(1.0, p.values.sum(), 1e-12)
        assertEquals(1.0 / totalW, p.getValue("a"), 1e-12)
        assertEquals(0.6 / totalW, p.getValue("c"), 1e-12)
        assertEquals(0.25 / totalW, p.getValue("d"), 1e-12)
    }

    @Test
    fun `空池返回空概率表和 null 结果`() {
        assertTrue(DrawEngine.probabilities(emptyList(), emptyMap()).isEmpty())
        assertNull(DrawEngine.draw(emptyList(), emptyMap()))
    }

    @Test
    fun `抽中的概率字段就是这次归一化后的真实概率`() {
        val pool = listOf(
            recipe("a", Rarity.COMMON), recipe("b", Rarity.SIGNATURE), recipe("c", Rarity.RARE)
        )
        val theo = DrawEngine.probabilities(pool, emptyMap())
        repeat(500) {
            val o = DrawEngine.draw(pool, emptyMap())!!
            assertEquals(theo.getValue(o.recipe.id), o.probability, 1e-12)
            assertEquals(pool.size, o.candidateCount)
        }
    }

    @Test
    fun `换一个会排除上一道 并且概率按剩下的候选重新归一`() {
        val pool = listOf(
            recipe("a", Rarity.COMMON), recipe("b", Rarity.COMMON), recipe("c", Rarity.RARE)
        )
        val totalW = 1.0 + 1.0 + 0.25
        val after = totalW - 1.0                       // 排除掉一道家常
        repeat(300) {
            val o = DrawEngine.draw(pool, emptyMap(), excludeId = "a")!!
            assertNotEquals("a", o.recipe.id)
            assertEquals(2, o.candidateCount)
            assertEquals(DrawEngine.weightOf(o.recipe, 0) / after, o.probability, 1e-12)
        }
    }

    @Test
    fun `池子只剩一道时排除它会退回原池 不会抽空`() {
        val pool = listOf(recipe("only", Rarity.RARE))
        val o = DrawEngine.draw(pool, emptyMap(), excludeId = "only")
        assertEquals("only", o!!.recipe.id)
        assertEquals(1.0, o.probability, 1e-12)
    }

    /**
     * 抽样频率与理论概率一致。
     * 判据用「偏差 / 标准差」而不是固定绝对容差——固定 1% 对 1 万样本等于 6.6σ，
     * 看着严格，实际是伪严格：既可能偶发误报，也挡不住真正超出 1% 的系统偏差。
     * 这里取 20 万次、单道 6σ（误报率约 2e-9），1% 级别的真实偏差一定会被抓到。
     */
    @Test
    fun `二十万次抽样频率与理论概率一致`() {
        val pool = listOf(
            recipe("a", Rarity.COMMON), recipe("b", Rarity.COMMON),
            recipe("c", Rarity.SIGNATURE), recipe("d", Rarity.RARE)
        )
        val theo = DrawEngine.probabilities(pool, emptyMap())
        val hit = HashMap<String, Int>()
        val n = 200_000
        repeat(n) { val o = DrawEngine.draw(pool, emptyMap())!!; hit[o.recipe.id] = (hit[o.recipe.id] ?: 0) + 1 }
        for (r in pool) {
            val p = theo.getValue(r.id)
            val diff = abs((hit[r.id] ?: 0).toDouble() / n - p)
            val sigma = kotlin.math.sqrt(p * (1 - p) / n)
            assertTrue(
                "${r.id} 理论 $p 实际 ${(hit[r.id] ?: 0).toDouble() / n} 偏差 ${diff / sigma}σ",
                diff < 6 * sigma
            )
        }
    }

    @Test
    fun `早餐池只含适合早餐的菜 选菜系只含该菜系`() {
        val all = listOf(
            recipe("bf", Rarity.COMMON, Cuisine.BREAKFAST, listOf(MealSlot.BREAKFAST)),
            recipe("lunch", Rarity.COMMON, Cuisine.CHUAN),
            recipe("yue", Rarity.COMMON, Cuisine.YUE)
        )
        assertEquals(listOf("bf"), DrawEngine.pool(all, null, MealSlot.BREAKFAST).map { it.id })
        assertEquals(
            listOf("lunch"),
            DrawEngine.pool(all, Cuisine.CHUAN, MealSlot.LUNCH).map { it.id }
        )
        // 早餐 + 指定菜系 的交集为空时不该崩
        assertTrue(DrawEngine.pool(all, Cuisine.YUE, MealSlot.BREAKFAST).isEmpty())
    }
}
