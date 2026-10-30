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

package com.chisha.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chisha.app.domain.AchievementEngine
import com.chisha.app.domain.XpEngine
import com.chisha.app.ui.AppUiState
import com.chisha.app.ui.components.XpBar
import com.chisha.app.ui.theme.InkPrimary
import com.chisha.app.ui.theme.InkSecondary
import com.chisha.app.ui.theme.InkTertiary
import com.chisha.app.ui.theme.Ochre
import com.chisha.app.ui.theme.TierBronze
import com.chisha.app.ui.theme.TierGold
import com.chisha.app.ui.theme.TierSilver
import com.chisha.app.ui.theme.WarmSurface

@Composable
fun ProfileScreen(
    state: AppUiState,
    onSetNickname: (String) -> Unit
) {
    val (into, need) = XpEngine.progress(state.profile.totalXp)
    var editingName by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf(state.profile.nickname) }
    val byGroup = remember(state.achievementDefs) {
        state.achievementDefs.groupBy { it.group }
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFF0DC), Color(0xFFFFFBF5))))
                    .padding(18.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Lv.${state.profile.level}",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ochre
                        )
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                XpEngine.title(state.profile.level),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = InkPrimary
                            )
                            Text(
                                "本地存档 · ${state.profile.nickname}",
                                fontSize = 12.sp,
                                color = InkTertiary
                            )
                        }
                        TextButton(onClick = { editingName = !editingName }) {
                            Text(if (editingName) "取消" else "改名", fontSize = 12.sp, color = Ochre)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    XpBar(level = state.profile.level, into = into, need = need, showLabel = false)
                }
            }
        }

        if (editingName) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = nameDraft,
                        onValueChange = { if (it.length <= 12) nameDraft = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        label = { Text("昵称", fontSize = 12.sp) }
                    )
                    Spacer(Modifier.size(8.dp))
                    TextButton(onClick = {
                        onSetNickname(nameDraft)
                        editingName = false
                    }) { Text("保存", color = Ochre, fontWeight = FontWeight.SemiBold) }
                }
            }
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(WarmSurface)
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BigStat("下厨", "${state.allLogs.size}")
                BigStat("图鉴", "${state.collectionCount}")
                BigStat("成就", "${state.unlockedCount}/${state.achievementDefs.size}")
                BigStat("稀有", "${state.counters["rarity.rare"] ?: 0}")
            }
        }

        item {
            Text(
                "成就勋章墙",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = InkPrimary,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        AchievementEngine.groupOrder.forEach { group ->
            val defs = byGroup[group].orEmpty()
            if (defs.isEmpty()) return@forEach
            val unlockedInGroup = defs.count { state.achievementStates[it.id]?.unlockedAt != null }
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(WarmSurface)
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                AchievementEngine.groupLabel(group),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = InkPrimary
                            )
                            Text(
                                "$unlockedInGroup/${defs.size}",
                                fontSize = 12.sp,
                                color = if (unlockedInGroup == defs.size) Ochre else InkTertiary
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        defs.chunked(3).forEach { rowDefs ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowDefs.forEach { def ->
                                    val st = state.achievementStates[def.id]
                                    val unlocked = st?.unlockedAt != null
                                    Badge(
                                        name = def.displayName,
                                        desc = if (unlocked) def.desc else def.desc,
                                        unlocked = unlocked,
                                        tier = def.tier,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(3 - rowDefs.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun Badge(
    name: String,
    desc: String,
    unlocked: Boolean,
    tier: String,
    modifier: Modifier = Modifier
) {
    val tierColor = when (tier) {
        "gold" -> TierGold
        "silver" -> TierSilver
        else -> TierBronze
    }
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (unlocked) tierColor.copy(alpha = 0.10f) else Color(0xFFF7F2EB))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(if (unlocked) tierColor.copy(alpha = 0.22f) else Color(0xFFEDE6DC)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (unlocked) "✓" else "·",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (unlocked) tierColor else InkTertiary
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (unlocked) InkPrimary else InkTertiary,
            maxLines = 1
        )
        Text(
            desc,
            fontSize = 9.sp,
            color = InkTertiary,
            maxLines = 2,
            lineHeight = 12.sp
        )
    }
}

@Composable
private fun BigStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
        Text(label, fontSize = 11.sp, color = InkTertiary)
    }
}
