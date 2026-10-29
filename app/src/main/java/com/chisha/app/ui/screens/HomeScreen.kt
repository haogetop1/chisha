package com.chisha.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chisha.app.domain.MealSlot
import com.chisha.app.domain.XpEngine
import com.chisha.app.ui.AppUiState
import com.chisha.app.ui.components.SlotChip
import com.chisha.app.ui.components.XpBar
import com.chisha.app.ui.theme.InkPrimary
import com.chisha.app.ui.theme.InkSecondary
import com.chisha.app.ui.theme.InkTertiary
import com.chisha.app.ui.theme.Ochre
import com.chisha.app.ui.theme.WarmOutline
import com.chisha.app.ui.theme.WarmSurface
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun HomeScreen(
    state: AppUiState,
    onPickSlot: (MealSlot) -> Unit,
    onOpenCollection: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val (into, need) = XpEngine.progress(state.profile.totalXp)
    val hour = LocalTime.now().hour
    val greeting = when {
        hour < 10 -> "早上好"
        hour < 14 -> "中午好"
        hour < 18 -> "下午好"
        else -> "晚上好"
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(Modifier.height(14.dp)) }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "$greeting，今天吃啥",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkPrimary
                    )
                    Text(
                        LocalDate.now().let { "${it.monthValue} 月 ${it.dayOfMonth} 日 · 今日还有 ${3 - state.usedSlots.size} 次下厨" },
                        fontSize = 13.sp,
                        color = InkTertiary
                    )
                    val todayTag = state.todayFestival ?: state.todayTerm
                    if (todayTag != null) {
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFCE6D6))
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(
                                if (state.todayFestival != null) "今日 $todayTag · 做一道应景的" else "今日节气 $todayTag",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Ochre
                            )
                        }
                    }
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(WarmSurface)
                        .clickable { onOpenProfile() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("Lv.${state.profile.level}", fontWeight = FontWeight.Bold, color = Ochre, fontSize = 15.sp)
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = WarmSurface),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    XpBar(level = state.profile.level, into = into, need = need)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MiniStat("累计下厨", "${state.allLogs.size}")
                        MiniStat("图鉴", "${state.collectionCount}/${state.recipes.size}", onClick = onOpenCollection)
                        MiniStat("成就", "${state.unlockedCount}/${state.achievementDefs.size}", onClick = onOpenProfile)
                        MiniStat("今日经验", "+${state.todayXp}")
                    }
                }
            }
        }

        item {
            Text(
                "今天的三顿",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                color = InkPrimary
            )
        }

        items(MealSlot.entries.size) { idx ->
            val slot = MealSlot.entries[idx]
            val used = slot in state.usedSlots
            val log = state.todayLogs.firstOrNull { it.mealSlot == slot.key }
            SlotCard(
                slot = slot,
                used = used,
                recipeName = log?.recipeName,
                xp = log?.xpEarned,
                enabled = !used,
                onClick = { if (!used) onPickSlot(slot) }
            )
        }

        if (state.usedSlots.size == 3) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFF6E3CB), Color(0xFFFDF3E4))))
                        .padding(16.dp)
                ) {
                    Column {
                        Text("今天三餐全勤", fontWeight = FontWeight.Bold, color = Ochre)
                        Text(
                            "三个名额都用完了，明天再来。今日共获得 ${state.todayXp} 经验。",
                            fontSize = 13.sp,
                            color = InkSecondary
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SlotCard(
    slot: MealSlot,
    used: Boolean,
    recipeName: String?,
    xp: Int?,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (used) Color(0xFFF7F2EB) else WarmSurface
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (used) Color(0xFFEBE3D8) else Color(0xFFFCE6D6)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    slot.label.take(1),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (used) InkTertiary else Ochre
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(slot.label, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = InkPrimary)
                Text(
                    when {
                        used && recipeName != null -> "$recipeName · +$xp 经验"
                        used -> "已完成"
                        else -> "还没决定，点一下开始抽"
                    },
                    fontSize = 13.sp,
                    color = InkTertiary
                )
            }
            if (!used) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Ochre)
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text("抽一道", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    ) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = InkPrimary)
        Text(label, fontSize = 11.sp, color = InkTertiary)
    }
}

@Composable
fun Divider1() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(WarmOutline)
    )
}
