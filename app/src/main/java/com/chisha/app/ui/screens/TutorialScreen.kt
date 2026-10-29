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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chisha.app.domain.Recipe
import com.chisha.app.ui.components.ExtraSection
import com.chisha.app.ui.components.HeatTimeline
import com.chisha.app.ui.theme.InkPrimary
import com.chisha.app.ui.theme.InkSecondary
import com.chisha.app.ui.theme.InkTertiary
import com.chisha.app.ui.theme.Ochre
import com.chisha.app.ui.theme.WarmSurface

/** 教程：单页长文，六段顺序读到底，不做分步打断 */
@Composable
fun TutorialScreen(recipe: Recipe, onFinish: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text(recipe.name, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
        Text(
            "六步走完 · 约 ${recipe.timeMin} 分钟 · ${recipe.servings} 人份",
            fontSize = 13.sp,
            color = InkTertiary
        )
        Spacer(Modifier.height(18.dp))

        // 火候轴放最前面：真做起来的时候，抬头第一眼要看的就是火多大
        if (recipe.heat.isNotEmpty()) {
            ExtraSection(
                title = "炒菜流程",
                subtitle = "按时间顺序走，每一步的火候和判断点都标好了，别凭感觉翻锅。"
            ) {
                HeatTimeline(recipe)
            }
            Spacer(Modifier.height(22.dp))
        }

        recipe.steps.ordered.forEachIndexed { i, (title, body) ->
            Row(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color(0xFFFCE6D6)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${i + 1}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ochre
                    )
                }
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = InkPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        body,
                        fontSize = 14.5.sp,
                        lineHeight = 25.sp,
                        color = InkSecondary
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        if (recipe.commonFails.isNotBlank()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFFF1EE))
                    .padding(14.dp)
            ) {
                Column {
                    Text("容易翻车的地方", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFFB3402A))
                    Spacer(Modifier.height(6.dp))
                    Text(recipe.commonFails, fontSize = 13.5.sp, lineHeight = 22.sp, color = InkSecondary)
                }
            }
            Spacer(Modifier.height(18.dp))
        }

        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(WarmSurface)
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("上桌之前", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = InkPrimary)
                Text("先尝一口再决定要不要补盐，热菜在锅里偏淡，盛出来刚好。", fontSize = 13.sp, color = InkSecondary)
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ochre)
        ) {
            Text("我做好了", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(28.dp))
    }
}
