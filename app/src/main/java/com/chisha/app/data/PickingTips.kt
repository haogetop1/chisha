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

package com.chisha.app.data

import android.content.Context
import org.json.JSONArray

/**
 * 通用挑菜知识库：assets/picking.json 里是一批「食材关键词 -> 在超市怎么挑」。
 *
 * 菜谱里单味食材如果自己写了 pick，就用自己的；没写的来这儿按关键词匹配，
 * 这样 365 道菜哪怕还没逐道精修，也能给出有用的挑菜建议。
 */
object PickingTips {

    private var entries: List<Pair<String, String>> = emptyList()

    fun load(context: Context) {
        if (entries.isNotEmpty()) return
        val list = ArrayList<Pair<String, String>>()
        runCatching {
            val text = context.assets.open("picking.json").bufferedReader().use { it.readText() }
            val arr = JSONArray(text)
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val k = o.optString("k").trim()
                val t = o.optString("t").trim()
                if (k.isNotEmpty() && t.isNotEmpty()) list.add(k to t)
            }
        }
        // 长关键词优先，保证「五花肉」不会被「猪肉」抢先匹配掉
        list.sortByDescending { it.first.length }
        entries = list
    }

    /**
     * 单字关键词（鱼 / 鸡 / 油 / 糖 …）命中率广但容易误伤，
     * 所以名字里带「汁、酱、粉」这类明显是调味品的后缀时，不许用单字关键词去套
     * ——否则「鱼香汁」会被当成鱼。
     */
    private val SAUCE_MARK = listOf("汁", "酱", "粉", "精", "露", "油", "水")

    /** 按食材名找最贴切的一条挑菜建议；找不到返回 null */
    fun forIngredient(name: String): String? {
        if (name.isBlank()) return null
        val plain = name.replace(Regex("[（(][^）)]*[）)]"), "") // 去掉括号里的补充说明
        val isSauce = SAUCE_MARK.any { plain.contains(it) }
        for ((k, t) in entries) {
            if (!plain.contains(k)) continue
            if (isSauce && k.length == 1) continue
            return t
        }
        return null
    }

    val size: Int get() = entries.size
}
