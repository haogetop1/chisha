package com.chisha.app.domain

import kotlin.math.pow
import kotlin.random.Random

data class DrawOutcome(
    val recipe: Recipe,
    val probability: Double,
    val tierProbability: Double
)

/**
 * 加权抽取。
 * 单道权重 = 稀有度基础权重 × 评分惩罚（≤2 星每次 ×0.6，下限 0.15）
 * 概率在池内归一化，因此「降权」只影响该菜所属的池，不会跨菜系。
 */
object DrawEngine {

    const val PENALTY_BASE = 0.6
    const val PENALTY_FLOOR = 0.15

    fun weightOf(recipe: Recipe, lowRatingCount: Int): Double {
        val penalty = PENALTY_BASE.pow(lowRatingCount.toDouble()).coerceAtLeast(PENALTY_FLOOR)
        return recipe.rarity.baseWeight * penalty
    }

    /** 决定抽取池：早餐只出早餐子集，选了菜系则收窄到该菜系 */
    fun pool(all: List<Recipe>, cuisine: Cuisine?, slot: MealSlot): List<Recipe> {
        val bySlot = if (slot == MealSlot.BREAKFAST) all.filter { it.fitsBreakfast } else all
        return if (cuisine == null) bySlot else bySlot.filter { it.cuisine == cuisine }
    }

    fun probabilities(pool: List<Recipe>, lowRatings: Map<String, Int>): Map<String, Double> {
        if (pool.isEmpty()) return emptyMap()
        val weights = pool.map { weightOf(it, lowRatings[it.id] ?: 0) }
        val total = weights.sum()
        return pool.indices.associate { pool[it].id to weights[it] / total }
    }

    fun draw(
        pool: List<Recipe>,
        lowRatings: Map<String, Int>,
        excludeId: String? = null
    ): DrawOutcome? {
        if (pool.isEmpty()) return null
        val candidates = pool.filter { it.id != excludeId }.ifEmpty { pool }
        val weights = candidates.map { weightOf(it, lowRatings[it.id] ?: 0) }
        val total = weights.sum()
        var r = Random.nextDouble() * total
        var picked = candidates.lastIndex
        for (i in candidates.indices) {
            r -= weights[i]
            if (r <= 0.0) {
                picked = i
                break
            }
        }
        val chosen = candidates[picked]
        val tierTotal = candidates.indices
            .filter { candidates[it].rarity == chosen.rarity }
            .sumOf { weights[it] }
        return DrawOutcome(
            recipe = chosen,
            probability = weights[picked] / total,
            tierProbability = tierTotal / total
        )
    }

    /** 档位分布，用于标定与自检 */
    fun tierShare(pool: List<Recipe>, lowRatings: Map<String, Int> = emptyMap()): Map<Rarity, Double> {
        val probs = probabilities(pool, lowRatings)
        return Rarity.entries.associateWith { r ->
            pool.filter { it.rarity == r }.sumOf { probs[it.id] ?: 0.0 }
        }
    }
}
