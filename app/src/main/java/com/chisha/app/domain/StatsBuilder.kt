package com.chisha.app.domain

import com.chisha.app.data.AppDatabase
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields

/**
 * 把整个数据库折叠成一张计数器表，成就判定只读这张表。
 * 100 个成就因此不需要 100 段 if-else。
 */
object StatsBuilder {

    fun dayKey(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toString()

    suspend fun lowRatingCounts(db: AppDatabase): Map<String, Int> =
        db.ratingDao().all()
            .filter { it.stars <= 2 }
            .groupingBy { it.recipeId }
            .eachCount()

    suspend fun build(db: AppDatabase): StatSnapshot {
        val cooks = db.cookDao().all().sortedBy { it.cookedAt }
        val ratings = db.ratingDao().all()
        val profile = db.profileDao().get()
        val c = HashMap<String, Int>()

        c["totalCooks"] = cooks.size
        c["level"] = profile?.level ?: 1
        c["photos"] = cooks.count { !it.photoPath.isNullOrBlank() }
        c["comments"] = ratings.count { it.comment.isNotBlank() }
        c["ratings"] = ratings.size
        c["distinctRated"] = ratings.map { it.recipeId }.distinct().size
        c["collection"] = cooks.map { it.recipeId }.distinct().size

        cooks.groupingBy { "cuisine.${it.cuisine}" }.eachCount().forEach { (k, v) -> c[k] = v }
        cooks.groupingBy { "rarity.${it.rarity}" }.eachCount().forEach { (k, v) -> c[k] = v }
        cooks.groupingBy { "slot.${it.mealSlot}" }.eachCount().forEach { (k, v) -> c[k] = v }
        ratings.groupingBy { "star.${it.stars}" }.eachCount().forEach { (k, v) -> c[k] = v }

        // ---- 节气与节日（按做饭当天的日期查表）----
        val termHit = HashSet<String>()
        val festivalHit = HashSet<String>()
        cooks.forEach { log ->
            CalendarTable.termOf(log.dayKey)?.let { t ->
                c["term.$t"] = (c["term.$t"] ?: 0) + 1
                termHit.add(t)
            }
            CalendarTable.festivalOf(log.dayKey)?.let { f ->
                c["festival.$f"] = (c["festival.$f"] ?: 0) + 1
                festivalHit.add(f)
            }
        }
        c["term.all"] = termHit.size
        c["festival.all"] = festivalHit.size

        // ---- 连续天数 ----
        val days = cooks.map { it.dayKey }.distinct().sorted()
        c["streak.best"] = bestStreak(days)
        c["streak.current"] = currentStreak(days)

        // ---- 按天聚合 ----
        val byDay = cooks.groupBy { it.dayKey }
        if (byDay.values.any { d -> d.map { it.mealSlot }.distinct().size >= 3 }) c["flag.day3slots"] = 1
        if (byDay.values.any { d -> d.map { it.cuisine }.distinct().size >= 3 }) c["flag.day3cuisines"] = 1
        if (byDay.values.any { d -> d.count { it.rarity == Rarity.RARE.key } >= 3 }) c["flag.day3rare"] = 1
        if (byDay.values.any { d -> d.count { !it.photoPath.isNullOrBlank() } >= 3 }) c["flag.day3photos"] = 1
        if (byDay.entries.any { (day, d) ->
                runCatching { LocalDate.parse(day).dayOfWeek }.getOrNull() in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) && d.size >= 2
            }
        ) c["flag.weekend2"] = 1

        // ---- 时间点 ----
        val zone = ZoneId.systemDefault()
        if (cooks.any { Instant.ofEpochMilli(it.cookedAt).atZone(zone).hour < 6 }) c["flag.before6"] = 1
        if (cooks.any { Instant.ofEpochMilli(it.cookedAt).atZone(zone).hour >= 23 }) c["flag.after23"] = 1

        // ---- 连续有图天数 ----
        val photoDays = cooks.filter { !it.photoPath.isNullOrBlank() }.map { it.dayKey }.distinct().sorted()
        if (bestStreak(photoDays) >= 7) c["flag.photo7"] = 1

        // ---- 单次行为 ----
        if (cooks.any { it.rerolls >= 10 }) c["flag.reroll10"] = 1
        if (maxNoRerollRun(cooks) >= 10) c["flag.noReroll10"] = 1
        if (cooks.any { it.timeMin <= 15 }) c["flag.quick15"] = 1
        if (cooks.any { it.timeMin >= 120 }) c["flag.long120"] = 1
        if (cooks.any { it.photoSource == "gallery" }) c["flag.gallery"] = 1
        if (cooks.any { it.recipeName.contains("全鱼") }) c["flag.wholeFish"] = 1
        // 抽中并完成单道概率千分之一以下的菜
        if (cooks.any { it.drawProbability in 0.00001..0.0010 }) c["flag.rarestPull"] = 1

        // ---- 按周聚合 ----
        val weekField = WeekFields.ISO
        val byWeek = cooks.groupBy {
            val d = runCatching { LocalDate.parse(it.dayKey) }.getOrNull() ?: LocalDate.MIN
            "${d.get(weekField.weekBasedYear())}-W${d.get(weekField.weekOfWeekBasedYear())}"
        }
        if (byWeek.values.any { w -> w.map { it.dayKey }.distinct().count { day ->
                byDay[day].orEmpty().map { it.mealSlot }.distinct().size >= 3
            } >= 3 }
        ) c["flag.fullWeek3"] = 1
        if (byWeek.values.any { w -> w.count { it.difficulty >= 3 } >= 3 }) c["flag.week3hard"] = 1

        // ---- 八大菜系各来一道 ----
        val eight = listOf("川菜", "粤菜", "湘菜", "鲁菜", "苏菜", "浙菜", "闽菜", "徽菜")
        if (eight.all { (c["cuisine.$it"] ?: 0) > 0 }) c["flag.all8"] = 1

        // ---- 一天升两级 / 满级之后 ----
        var acc = 0
        val levelsPerDay = HashMap<String, Int>()
        var maxLevelAt = -1
        var cooksAfterMax = 0
        cooks.forEachIndexed { idx, log ->
            val before = XpEngine.levelFor(acc)
            acc += log.xpEarned
            val after = XpEngine.levelFor(acc)
            if (after > before) {
                levelsPerDay[log.dayKey] = (levelsPerDay[log.dayKey] ?: 0) + (after - before)
            }
            if (maxLevelAt < 0 && acc >= XpEngine.totalXpToReach(XpEngine.MAX_LEVEL)) maxLevelAt = idx
            if (maxLevelAt >= 0 && idx > maxLevelAt) cooksAfterMax++
        }
        if (levelsPerDay.values.any { it >= 2 }) c["flag.day2levels"] = 1
        c["cooksAfterMax"] = cooksAfterMax

        // ---- 悔改：先 1 星后 5 星 ----
        val byRecipe = ratings.groupBy { it.recipeId }
        if (byRecipe.values.any { list ->
                val sorted = list.sortedBy { it.createdAt }
                val firstOne = sorted.indexOfFirst { it.stars == 1 }
                firstOne >= 0 && sorted.drop(firstOne).any { it.stars == 5 }
            }
        ) c["flag.regret"] = 1

        // ---- 评论字数 ----
        if (ratings.any { it.comment.length >= 50 }) c["flag.longComment"] = 1
        // 同一道菜改评 3 次
        if (byRecipe.values.any { it.size >= 3 }) c["flag.rerate3"] = 1

        return StatSnapshot(c)
    }

    private fun bestStreak(sortedDays: List<String>): Int {
        if (sortedDays.isEmpty()) return 0
        var best = 1
        var run = 1
        for (i in 1 until sortedDays.size) {
            val prev = runCatching { LocalDate.parse(sortedDays[i - 1]) }.getOrNull()
            val cur = runCatching { LocalDate.parse(sortedDays[i]) }.getOrNull()
            if (prev != null && cur != null && prev.plusDays(1) == cur) {
                run++
                if (run > best) best = run
            } else {
                run = 1
            }
        }
        return best
    }

    private fun currentStreak(sortedDays: List<String>): Int {
        if (sortedDays.isEmpty()) return 0
        val today = LocalDate.now()
        val last = runCatching { LocalDate.parse(sortedDays.last()) }.getOrNull() ?: return 0
        if (last != today && last.plusDays(1) != today) return 0
        var run = 1
        for (i in sortedDays.size - 1 downTo 1) {
            val prev = runCatching { LocalDate.parse(sortedDays[i - 1]) }.getOrNull() ?: break
            val cur = runCatching { LocalDate.parse(sortedDays[i]) }.getOrNull() ?: break
            if (prev.plusDays(1) == cur) run++ else break
        }
        return run
    }

    private fun maxNoRerollRun(cooks: List<com.chisha.app.data.CookLogEntity>): Int {
        var best = 0
        var run = 0
        cooks.forEach {
            if (it.rerolls == 0) {
                run++
                if (run > best) best = run
            } else run = 0
        }
        return best
    }
}
