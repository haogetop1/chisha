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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chisha.app.data.CuttingTips
import com.chisha.app.data.PickingTips
import com.chisha.app.domain.Recipe
import com.chisha.app.ui.components.ExtraSection
import com.chisha.app.ui.components.FailTable
import com.chisha.app.ui.components.HeatTimeline
import com.chisha.app.ui.components.SeasoningTable
import com.chisha.app.ui.components.RarityBadge
import com.chisha.app.ui.components.RecipeArtwork
import com.chisha.app.ui.components.SectionTitle
import com.chisha.app.ui.theme.InkPrimary
import com.chisha.app.ui.theme.InkSecondary
import com.chisha.app.ui.theme.InkTertiary
import com.chisha.app.ui.theme.Ochre
import com.chisha.app.ui.theme.WarmOutline
import com.chisha.app.ui.theme.WarmSurface

@Composable
fun RecipeScreen(
    recipe: Recipe,
    lastRating: Int?,
    onStart: () -> Unit,
    readOnly: Boolean = false
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        RecipeArtwork(
            recipe = recipe,
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
        )
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(recipe.name, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
            Spacer(Modifier.width(10.dp))
            RarityBadge(recipe.rarity)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "${recipe.cuisine.label} · ${recipe.difficulty.let { if (it == 1) "简单" else if (it == 3) "复杂" else "中等" }} · 约 ${recipe.timeMin} 分钟 · ${recipe.servings} 人份",
            fontSize = 13.sp,
            color = InkSecondary
        )
        if (lastRating != null) {
            Spacer(Modifier.height(6.dp))
            Text("我上次给这道菜打了 $lastRating 星", fontSize = 12.sp, color = Ochre)
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("食材准备")
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(WarmSurface)
                .padding(vertical = 6.dp)
        ) {
            recipe.ingredients.forEachIndexed { i, ing ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(ing.name, fontSize = 14.sp, color = InkPrimary, fontWeight = FontWeight.Medium)
                        if (ing.note.isNotBlank()) {
                            Text(ing.note, fontSize = 11.sp, color = InkTertiary, lineHeight = 16.sp)
                        }
                        if (ing.prep.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "处理：" + ing.prep,
                                fontSize = 11.sp,
                                color = Ochre.copy(alpha = 0.85f),
                                lineHeight = 16.sp
                            )
                        }
                    }
                    Text(ing.amount, fontSize = 13.sp, color = Ochre, fontWeight = FontWeight.Medium)
                }
                if (i != recipe.ingredients.lastIndex) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .height(1.dp)
                            .background(WarmOutline.copy(alpha = 0.5f))
                    )
                }
            }
        }

        // 「在超市怎么挑」：菜谱自带的 pick 优先，没有就去通用挑菜库匹配
        val picks = remember(recipe.id) {
            recipe.ingredients.mapNotNull { ing ->
                val tip = ing.pick.ifBlank { PickingTips.forIngredient(ing.name) ?: "" }
                if (tip.isBlank()) null else ing.name to tip
            }
        }
        if (picks.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            SectionTitle("在超市怎么挑")
            Spacer(Modifier.height(4.dp))
            Text(
                "照着这些挑，菜就成了一半。认不出的直接问摊主，比自己瞎拿靠谱。",
                fontSize = 11.5.sp,
                color = InkTertiary,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(10.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(WarmSurface)
                    .padding(vertical = 6.dp)
            ) {
                picks.forEachIndexed { i, (name, tip) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0xFFE4EFE9))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                name,
                                fontSize = 11.5.sp,
                                color = Color(0xFF2F6B52),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                        Spacer(Modifier.width(9.dp))
                        Text(
                            tip,
                            fontSize = 12.5.sp,
                            lineHeight = 19.sp,
                            color = InkSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (i != picks.lastIndex) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp)
                                .height(1.dp)
                                .background(WarmOutline.copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }

        // 「食材怎么切」：菜谱自带的 cut 优先，没有就去通用切菜库匹配
        val cuts = remember(recipe.id) {
            recipe.ingredients.mapNotNull { ing ->
                val tip = ing.cut.ifBlank { CuttingTips.forIngredient(ing.name) ?: "" }
                if (tip.isBlank()) null else ing.name to tip
            }
        }
        if (cuts.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            SectionTitle("食材怎么切")
            Spacer(Modifier.height(4.dp))
            Text(
                "刀要快、下刀要利落。来回锯会把纤维拉断，出水更多、口感也差。",
                fontSize = 11.5.sp,
                color = InkTertiary,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(10.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(WarmSurface)
                    .padding(vertical = 6.dp)
            ) {
                cuts.forEachIndexed { i, (name, tip) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0xFFFBEBDC))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                name,
                                fontSize = 11.5.sp,
                                color = Ochre,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                        Spacer(Modifier.width(9.dp))
                        Text(
                            tip,
                            fontSize = 12.5.sp,
                            lineHeight = 19.sp,
                            color = InkSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (i != cuts.lastIndex) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp)
                                .height(1.dp)
                                .background(WarmOutline.copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }

        // 「调味料什么时候放」：按时间先后排好的投放表
        if (recipe.seasoning.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            SectionTitle("调味料什么时候放")
            Spacer(Modifier.height(4.dp))
            Text(
                "按时间先后排好了。放错顺序是家常菜最常见的翻车点——尤其盐、醋、味精这三样。",
                fontSize = 11.5.sp,
                color = InkTertiary,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(WarmSurface)
                    .padding(vertical = 6.dp)
            ) {
                SeasoningTable(recipe)
            }
        }

        // v2 加强版菜谱才有的两块：炒菜流程 / 翻车对照表
        if (recipe.heat.isNotEmpty()) {
            ExtraSection(
                title = "炒菜流程",
                subtitle = "按时间顺序走一遍：每一步该做什么、多大火、多久、怎么判断到位。左边圆点的颜色就是火的大小。"
            ) {
                HeatTimeline(recipe)
            }
        }
        if (recipe.fails.isNotEmpty()) {
            ExtraSection(
                title = "翻车对照表",
                subtitle = "出问题了直接扫「症状」，找对应的「补救」。"
            ) {
                FailTable(recipe)
            }
        }

        if (recipe.commonFails.isNotBlank()) {
            Spacer(Modifier.height(18.dp))
            SectionTitle("容易翻车的地方")
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFFF1EE))
                    .padding(14.dp)
            ) {
                Text(recipe.commonFails, fontSize = 13.5.sp, lineHeight = 22.sp, color = InkSecondary)
            }
        }

        if (recipe.tips.isNotBlank()) {
            Spacer(Modifier.height(18.dp))
            SectionTitle("一句提醒")
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFDF3E4))
                    .padding(14.dp)
            ) {
                Text(recipe.tips, fontSize = 14.sp, color = InkSecondary, lineHeight = 22.sp)
            }
        }

        Spacer(Modifier.height(26.dp))
        if (readOnly) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF7F2EB))
                    .padding(16.dp)
            ) {
                Text(
                    "这是图鉴预览。想动手做，从首页「今天的三顿」里抽到它才算数。",
                    fontSize = 13.sp,
                    color = InkTertiary,
                    lineHeight = 20.sp
                )
            }
        } else {
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ochre)
            ) {
                Text("开始做菜", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}
