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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chisha.app.ui.theme.InkPrimary
import com.chisha.app.ui.theme.InkSecondary
import com.chisha.app.ui.theme.InkTertiary
import com.chisha.app.ui.theme.Ochre

@Composable
fun RatingScreen(
    recipeName: String,
    onSubmit: (stars: Int, comment: String) -> Unit,
    onSkip: () -> Unit
) {
    var stars by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Text("这道菜怎么样？", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
        Text(recipeName, fontSize = 13.sp, color = InkTertiary)
        Spacer(Modifier.height(26.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            (1..5).forEach { i ->
                Box(
                    Modifier
                        .padding(horizontal = 6.dp)
                        .size(44.dp)
                        .clickable { stars = i },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "★",
                        fontSize = 34.sp,
                        color = if (i <= stars) Color(0xFFE0A93C) else Color(0xFFE6DFD5)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            when (stars) {
                0 -> "点星星打分（可以跳过）"
                1 -> "1 星 · 不会想再做一次"
                2 -> "2 星 · 有点翻车"
                3 -> "3 星 · 还行"
                4 -> "4 星 · 不错，会再做"
                5 -> "5 星 · 太好吃了"
                else -> ""
            },
            Modifier.fillMaxWidth(),
            fontSize = 13.sp,
            color = InkSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(Modifier.height(22.dp))
        OutlinedTextField(
            value = comment,
            onValueChange = { if (it.length <= 300) comment = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            placeholder = { Text("记一笔：哪里咸了、火候差多少、下次怎么改…", fontSize = 13.sp) },
            shape = RoundedCornerShape(14.dp),
            label = { Text("评论", fontSize = 12.sp) }
        )

        Spacer(Modifier.height(8.dp))
        Text(
            "给 1-2 星会让这道菜以后更难被抽到（只影响它所在的菜系）",
            fontSize = 11.sp,
            color = InkTertiary
        )

        Spacer(Modifier.weight(1f))
        Button(
            onClick = { onSubmit(stars, comment) },
            enabled = stars > 0,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ochre)
        ) {
            Text("提交评价", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text("跳过", fontSize = 13.sp, color = InkTertiary)
        }
        Spacer(Modifier.height(16.dp))
    }
}
