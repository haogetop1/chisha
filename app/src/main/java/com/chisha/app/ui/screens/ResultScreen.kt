package com.chisha.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chisha.app.domain.XpEngine
import com.chisha.app.ui.CookResult
import com.chisha.app.ui.components.XpBar
import com.chisha.app.ui.theme.InkPrimary
import com.chisha.app.ui.theme.InkSecondary
import com.chisha.app.ui.theme.InkTertiary
import com.chisha.app.ui.theme.Ochre
import com.chisha.app.ui.theme.TierGold
import com.chisha.app.ui.theme.WarmSurface

@Composable
fun ResultScreen(
    result: CookResult,
    onRate: () -> Unit,
    onDone: () -> Unit
) {
    val (into, need) = XpEngine.progress(result.totalXp)
    val levelUp = result.levelAfter > result.levelBefore
    val progressTarget = if (need <= 0) 1f else (into.toFloat() / need).coerceIn(0f, 1f)
    val animated by animateFloatAsState(progressTarget, tween(900), label = "resultXp")

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(26.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFFFF3E2), Color(0xFFFFFBF6))))
                .padding(22.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("完成", fontSize = 13.sp, color = InkTertiary)
                Spacer(Modifier.height(6.dp))
                Text(
                    result.recipe.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "+${result.xpEarned}",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ochre
                )
                Text("经验", fontSize = 13.sp, color = InkTertiary)
            }
        }

        Spacer(Modifier.height(18.dp))
        XpBar(level = result.levelAfter, into = into, need = need, modifier = Modifier.fillMaxWidth())

        if (levelUp) {
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFF6DCA8), Color(0xFFFFF0D2))))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        "升级！Lv.${result.levelBefore} → Lv.${result.levelAfter}",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8A5A00),
                        fontSize = 16.sp
                    )
                    Text(
                        "新称号：${XpEngine.title(result.levelAfter)}",
                        fontSize = 13.sp,
                        color = InkSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(WarmSurface)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ResultLine("本次加成", buildList {
                if (result.isNewRecipe) add("首次做这道菜 +30")
                if (result.firstOfDay) add("今日首单 ×1.3")
                if (result.fullDay) add("三餐全勤 +50")
            }.ifEmpty { listOf("基础经验") }.joinToString(" · "))
            ResultLine("今日进度", "${result.slot.label}名额已用")
        }

        if (result.unlocked.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Text(
                "解锁 ${result.unlocked.size} 个成就",
                Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = InkPrimary
            )
            Spacer(Modifier.height(8.dp))
            result.unlocked.forEach { def ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (def.tier == "gold") Color(0xFFFFF6DF) else WarmSurface)
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (def.tier == "gold") TierGold.copy(alpha = 0.18f)
                                    else Ochre.copy(alpha = 0.12f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (def.tier == "gold") "金" else if (def.tier == "silver") "银" else "铜",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (def.tier == "gold") Color(0xFF9A6B00) else Ochre
                            )
                        }
                        Spacer(Modifier.size(12.dp))
                        Column {
                            Text(def.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = InkPrimary)
                            Text(def.desc, fontSize = 12.sp, color = InkTertiary)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRate,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ochre)
        ) {
            Text("给这道菜打个分", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("先不打分", color = InkSecondary)
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun ResultLine(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, fontSize = 12.sp, color = InkTertiary)
        Spacer(Modifier.size(12.dp))
        Text(
            value,
            fontSize = 12.sp,
            color = InkSecondary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}
