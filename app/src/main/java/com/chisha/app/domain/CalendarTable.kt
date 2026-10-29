package com.chisha.app.domain

import android.content.Context
import org.json.JSONObject

/**
 * 节气与节日查表。
 *
 * 为什么是查表而不是运行时换算：
 * 节气每年日期不同（立春可能在 2 月 3/4/5 日），农历节日还要做农历换算，
 * 而且「立春早于春节」是真实存在的年份（如 2026 年立春 2 月 4 日、春节 2 月 17 日）。
 * 任何固定日期写法都会错位，所以在构建期用 tools/gen_calendar.py 把
 * 2025~2045 年每一天的节气与节日算好，运行时只做一次字典查询。
 */
object CalendarTable {

    private const val MAX_YEAR = 2045

    val ALL_TERMS = listOf(
        "立春", "雨水", "惊蛰", "春分", "清明", "谷雨",
        "立夏", "小满", "芒种", "夏至", "小暑", "大暑",
        "立秋", "处暑", "白露", "秋分", "寒露", "霜降",
        "立冬", "小雪", "大雪", "冬至", "小寒", "大寒"
    )

    val ALL_FESTIVALS = listOf(
        "元旦", "春节", "元宵节", "端午节", "七夕节", "中秋节",
        "重阳节", "腊八节", "小年", "除夕", "劳动节", "国庆节"
    )

    @Volatile
    private var loaded = false

    private var termMap: Map<String, String> = emptyMap()
    private var festivalMap: Map<String, String> = emptyMap()

    fun load(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            runCatching {
                val text = context.assets.open("calendar.json")
                    .bufferedReader().use { it.readText() }
                val root = JSONObject(text)
                termMap = flatten(root.optJSONObject("terms"))
                festivalMap = flatten(root.optJSONObject("festivals"))
            }
            loaded = true
        }
    }

    private fun flatten(o: JSONObject?): Map<String, String> {
        if (o == null) return emptyMap()
        val m = HashMap<String, String>(o.length())
        o.keys().forEach { k -> m[k] = o.optString(k) }
        return m
    }

    fun termOf(dayKey: String): String? = termMap[dayKey]

    fun festivalOf(dayKey: String): String? = festivalMap[dayKey]

    fun isCovered(dayKey: String): Boolean {
        val year = dayKey.take(4).toIntOrNull() ?: return false
        return year in 2025..MAX_YEAR
    }

    /** 首页用：今天是哪个节气 / 节日 */
    fun todayInfo(dayKey: String): Pair<String?, String?> =
        termOf(dayKey) to festivalOf(dayKey)
}
