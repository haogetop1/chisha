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

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chisha.app.domain.PhotoStore
import com.chisha.app.ui.AppUiState
import com.chisha.app.ui.theme.InkPrimary
import com.chisha.app.ui.theme.InkSecondary
import com.chisha.app.ui.theme.InkTertiary
import com.chisha.app.ui.theme.Ochre
import com.chisha.app.ui.theme.WarmSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

@Composable
fun HistoryScreen(state: AppUiState) {
    val logs = state.allLogs
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(Modifier.height(14.dp))
            Text("下厨记录", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
            Text(
                "一共做了 ${logs.size} 次，累计 ${logs.sumOf { it.xpEarned }} 经验",
                fontSize = 13.sp,
                color = InkTertiary
            )
            Spacer(Modifier.height(6.dp))
        }

        if (logs.isEmpty()) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("还没有记录，去做第一道菜吧", color = InkTertiary, fontSize = 14.sp)
                }
            }
        }

        items(logs) { log ->
            val rating = state.ratings.firstOrNull { it.recipeId == log.recipeId }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(WarmSurface)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF3EADF)),
                    contentAlignment = Alignment.Center
                ) {
                    val name = log.photoPath
                    if (name != null) {
                        val bmp = rememberPhoto(name)
                        if (bmp != null) {
                            Image(
                                bitmap = bmp,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("图", fontSize = 12.sp, color = InkTertiary)
                        }
                    } else {
                        Text("无图", fontSize = 11.sp, color = InkTertiary)
                    }
                }
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(log.recipeName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = InkPrimary)
                    Text(
                        "${formatTime(log.cookedAt)} · ${slotLabel(log.mealSlot)} · +${log.xpEarned} 经验",
                        fontSize = 11.5.sp,
                        color = InkTertiary
                    )
                    if (rating != null) {
                        Text(
                            "★".repeat(rating.stars) + "☆".repeat(5 - rating.stars),
                            fontSize = 12.sp,
                            color = Color(0xFFE0A93C)
                        )
                    }
                }
                Text(log.cuisine, fontSize = 11.sp, color = InkSecondary)
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
fun rememberPhoto(name: String): ImageBitmap? {
    val context = LocalContext.current
    val bmp by produceState<ImageBitmap?>(initialValue = null, key1 = name) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val f = PhotoStore.fileFor(context, name)
                if (!f.exists()) return@runCatching null
                BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap()
            }.getOrNull()
        }
    }
    return bmp
}

private fun formatTime(millis: Long): String {
    val t = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    return "${t.monthValue}/${t.dayOfMonth} ${String.format("%02d:%02d", t.hour, t.minute)}"
}

private fun slotLabel(key: String): String = when (key) {
    "breakfast" -> "早餐"
    "lunch" -> "午餐"
    else -> "晚餐"
}
