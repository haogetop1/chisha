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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.chisha.app.domain.Cuisine
import com.chisha.app.domain.MealSlot
import com.chisha.app.ui.theme.InkPrimary
import com.chisha.app.ui.theme.InkSecondary
import com.chisha.app.ui.theme.InkTertiary
import com.chisha.app.ui.theme.Ochre
import com.chisha.app.ui.theme.WarmSurface

@Composable
fun CuisineScreen(
    slot: MealSlot,
    onPick: (Cuisine?) -> Unit,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(14.dp))
        Text("今天想吃哪个菜系？", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
        Text(
            "为${slot.label}挑一道菜 · 共 365 道，随时可以换",
            fontSize = 13.sp,
            color = InkTertiary
        )
        Spacer(Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Box(
                    Modifier
                        .height(74.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(Ochre, Color(0xFFE08A4B))))
                        .clickable { onPick(null) }
                        .padding(14.dp)
                ) {
                    Column(Modifier.align(Alignment.CenterStart)) {
                        Text("不知道吃什么", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("全库 365 道随机", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                    }
                }
            }
            Cuisine.entries.forEach { cuisine ->
                item {
                    CuisineTile(cuisine) { onPick(cuisine) }
                }
            }
        }
    }
}

@Composable
private fun CuisineTile(cuisine: Cuisine, onClick: () -> Unit) {
    Box(
        Modifier
            .height(74.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WarmSurface)
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(cuisine.color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(cuisine.label.take(1), color = cuisine.color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.size(10.dp))
            Column {
                Text(cuisine.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = InkPrimary)
                Text("${cuisine.total} 道", fontSize = 11.sp, color = InkSecondary)
            }
        }
    }
}
