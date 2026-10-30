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

import android.content.Context
import org.json.JSONArray

/**
 * 成就定义。所有成就最终都归结为「某个计数器的值达到 target」，
 * 因此 100 个成就只需要一份 JSON，不需要 100 段判定代码。
 */
data class AchievementDef(
    val id: String,
    val name: String,
    val desc: String,
    val group: String,
    val tier: String,
    val hidden: Boolean,
    val key: String,
    val target: Int
) {
    val displayName: String get() = if (hidden) "???" else name

    companion object {
        fun load(context: Context): List<AchievementDef> {
            val text = context.assets.open("achievements.json").bufferedReader().use { it.readText() }
            return parse(text)
        }

        fun parse(text: String): List<AchievementDef> {
            val arr = JSONArray(text)
            return (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                AchievementDef(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    desc = o.getString("desc"),
                    group = o.getString("group"),
                    tier = o.optString("tier", "bronze"),
                    hidden = o.optBoolean("hidden", false),
                    key = o.getString("key"),
                    target = o.optInt("target", 1)
                )
            }
        }
    }
}

object AchievementEngine {

    data class Evaluation(
        val newlyUnlocked: List<AchievementDef>,
        val progress: Map<String, Int>
    )

    /** 只计算未解锁的成就，已解锁的不再重复判定 */
    fun evaluate(
        defs: List<AchievementDef>,
        snapshot: StatSnapshot,
        unlockedIds: Set<String>
    ): Evaluation {
        val newly = ArrayList<AchievementDef>()
        val progress = HashMap<String, Int>()
        for (def in defs) {
            val value = snapshot[def.key]
            progress[def.id] = value.coerceAtMost(def.target)
            if (def.id !in unlockedIds && value >= def.target) newly.add(def)
        }
        return Evaluation(newly, progress)
    }

    fun groupLabel(group: String): String = when (group) {
        "cook" -> "下厨次数"
        "streak" -> "连续天数"
        "cuisine" -> "菜系精通"
        "term" -> "二十四节气"
        "festival" -> "中国节日"
        "rarity" -> "稀有手气"
        "collection" -> "图鉴收集"
        "level" -> "等级称号"
        "rating" -> "评价评论"
        "photo" -> "照片记录"
        "time" -> "时间全勤"
        "fun" -> "趣味与隐藏"
        else -> group
    }

    val groupOrder = listOf(
        "cook", "streak", "cuisine", "term", "festival", "rarity",
        "collection", "level", "rating", "photo", "time", "fun"
    )
}
