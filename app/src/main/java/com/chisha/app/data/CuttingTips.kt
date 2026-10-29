package com.chisha.app.data

import android.content.Context
import org.json.JSONArray

/**
 * 通用切菜知识库：assets/cutting.json 里是一批「食材关键词 -> 怎么切」。
 *
 * 菜谱里单味食材自己写了 cut 就用自己的，没写的来这儿按关键词匹配，
 * 这样 365 道菜哪怕还没逐道精修，也能给出刀法建议。
 */
object CuttingTips {

    private var entries: List<Pair<String, String>> = emptyList()

    fun load(context: Context) {
        if (entries.isNotEmpty()) return
        val list = ArrayList<Pair<String, String>>()
        runCatching {
            val text = context.assets.open("cutting.json").bufferedReader().use { it.readText() }
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
     * 调料、液体、粉面这类东西根本没有「怎么切」这回事。
     * 和挑菜库不同（调料是需要挑的），这里见到这些字直接不给建议——
     * 否则「蒸鱼豉油」会被当成鱼、「花椒面」会被当成面团。
     */
    private val NO_CUT_MARK = listOf(
        "汁", "酱", "粉", "精", "露", "水", "油", "酒", "醋", "糖", "盐", "面"
    )

    /** 按食材名找最贴切的切法；找不到返回 null */
    fun forIngredient(name: String): String? {
        if (name.isBlank()) return null
        val plain = name.replace(Regex("[（(][^）)]*[）)]"), "") // 去掉括号里的补充说明
        if (NO_CUT_MARK.any { plain.contains(it) }) return null
        return entries.firstOrNull { (k, _) -> plain.contains(k) }?.second
    }

    val size: Int get() = entries.size
}
