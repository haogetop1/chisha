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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chisha.app.data.rememberDishPhoto
import com.chisha.app.domain.Cuisine
import com.chisha.app.domain.Recipe
import com.chisha.app.ui.AppUiState
import com.chisha.app.ui.theme.InkPrimary
import com.chisha.app.ui.theme.InkSecondary
import com.chisha.app.ui.theme.InkTertiary
import com.chisha.app.ui.theme.Ochre
import com.chisha.app.ui.theme.WarmSurface

@Composable
fun CollectionScreen(state: AppUiState, onOpen: (Recipe) -> Unit) {
    var filter by remember { mutableStateOf<Cuisine?>(null) }
    val done = remember(state.allLogs) { state.allLogs.map { it.recipeId }.toSet() }
    val list = if (filter == null) state.recipes else state.recipes.filter { it.cuisine == filter }
    val ratio = if (state.recipes.isEmpty()) 0f else done.size.toFloat() / state.recipes.size

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(14.dp))
            Text("菜谱图鉴", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
            Text(
                "已收集 ${done.size} / ${state.recipes.size} 道 · ${(ratio * 100).toInt()}%",
                fontSize = 13.sp,
                color = InkTertiary
            )
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFFF0E4D6))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(ratio)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Ochre)
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChipSmall("全部", filter == null) { filter = null }
            FilterChipSmall("川", filter == Cuisine.CHUAN) { filter = Cuisine.CHUAN }
            FilterChipSmall("粤", filter == Cuisine.YUE) { filter = Cuisine.YUE }
            FilterChipSmall("湘", filter == Cuisine.XIANG) { filter = Cuisine.XIANG }
            FilterChipSmall("家常", filter == Cuisine.HOME) { filter = Cuisine.HOME }
            FilterChipSmall("早餐", filter == Cuisine.BREAKFAST) { filter = Cuisine.BREAKFAST }
        }
        Spacer(Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(list) { recipe ->
                val isDone = recipe.id in done
                val photo = rememberDishPhoto(recipe.id)
                Column(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDone) WarmSurface else Color(0xFFF7F2EB))
                        .clickable { onOpen(recipe) }
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(9.dp))
                    ) {
                        if (photo != null) {
                            Image(
                                bitmap = photo,
                                contentDescription = recipe.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val c = recipe.cuisine.color
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(listOf(c, c.copy(alpha = 0.65f)))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    recipe.name.take(1),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.92f)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        recipe.name,
                        fontSize = 12.sp,
                        fontWeight = if (isDone) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isDone) InkPrimary else InkTertiary,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        when {
                            isDone -> "已做"
                            recipe.rarity.key == "rare" -> "稀有"
                            else -> recipe.cuisine.label.take(2)
                        },
                        fontSize = 10.sp,
                        color = when {
                            isDone -> Ochre
                            recipe.rarity.key == "rare" -> Color(0xFF9A6B00)
                            else -> InkTertiary
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChipSmall(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) Ochre else WarmSurface)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            fontSize = 12.sp,
            color = if (selected) Color.White else InkSecondary
        )
    }
}
