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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chisha.app.domain.Rarity
import com.chisha.app.ui.AppUiState
import com.chisha.app.ui.components.RarityBadge
import com.chisha.app.ui.components.RecipeArtwork
import com.chisha.app.ui.theme.InkPrimary
import com.chisha.app.ui.theme.InkSecondary
import com.chisha.app.ui.theme.InkTertiary
import com.chisha.app.ui.theme.Ochre
import com.chisha.app.ui.theme.WarmSurface
import kotlinx.coroutines.delay

private fun pct(p: Double): String {
    val v = p * 100
    return when {
        v >= 1 -> String.format("%.2f%%", v)
        v >= 0.1 -> String.format("%.3f%%", v)
        else -> String.format("%.4f%%", v)
    }
}

@Composable
fun DrawScreen(
    state: AppUiState,
    onReroll: () -> Unit,
    onConfirm: () -> Unit,
    onAnimated: () -> Unit
) {
    val session = state.session ?: return
    val recipe = session.outcome.recipe
    val recipeNames = state.recipes.map { it.name }

    val reel = remember(session.seq) {
        val sample = recipeNames.shuffled().take(22)
        sample + recipe.name
    }
    // 动画已播过就直接显示结果，避免从菜谱页返回时重播成「又抽一次」的错觉
    var index by remember(session.seq) { mutableIntStateOf(if (session.animated) reel.lastIndex else 0) }
    var rolling by remember(session.seq) { mutableStateOf(!session.animated) }
    var showRare by remember(session.seq) { mutableStateOf(false) }

    LaunchedEffect(session.seq) {
        if (session.animated) return@LaunchedEffect
        rolling = true
        var d = 45L
        for (i in reel.indices) {
            index = i
            delay(d)
            d = (d * 1.16).toLong().coerceAtMost(200L)
        }
        rolling = false
        if (recipe.rarity == Rarity.RARE) showRare = true
        onAnimated()
    }

    val flip by animateFloatAsState(
        targetValue = if (rolling) 0f else 0f,
        animationSpec = tween(300),
        label = "flip"
    )

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(10.dp))
        Text(
            "${session.slot.label} · ${session.cuisine?.label ?: "全库随机"}",
            fontSize = 13.sp,
            color = InkTertiary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (rolling) "正在挑…" else "今天就做这道",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = InkPrimary
        )

        Spacer(Modifier.height(28.dp))

        Box(
            Modifier
                .size(210.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFFDF1E2), Color(0xFFF7E2C9))))
                .rotate(if (rolling) 0f else flip),
            contentAlignment = Alignment.Center
        ) {
            if (rolling) {
                Text(
                    reel.getOrElse(index) { recipe.name },
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ochre.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(20.dp)
                )
            } else {
                RecipeArtwork(
                    recipe = recipe,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .padding(14.dp)
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        if (!rolling) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    recipe.name,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkPrimary
                )
                Spacer(Modifier.size(10.dp))
                RarityBadge(recipe.rarity)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${recipe.cuisine.label} · ${recipe.difficulty.let { if (it == 1) "简单" else if (it == 3) "复杂" else "中等" }} · 约 ${recipe.timeMin} 分钟 · ${recipe.servings} 人份",
                fontSize = 13.sp,
                color = InkSecondary
            )
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(WarmSurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    "本池 ${session.poolSize} 道 · 这道菜被抽中的概率 ${pct(session.outcome.probability)}",
                    fontSize = 12.sp,
                    color = InkTertiary
                )
            }
        }

        Spacer(Modifier.weight(1f))

        if (!rolling) {
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ochre)
            ) {
                Text("就做这个，看教程", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onReroll,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    if (session.rerolls == 0) "再换一个" else "再换一个（已换 ${session.rerolls} 次）",
                    color = InkSecondary
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }

    if (showRare) {
        AlertDialog(
            onDismissRequest = { showRare = false },
            confirmButton = {
                TextButton(onClick = { showRare = false }) {
                    Text("收下这份运气", color = Ochre, fontWeight = FontWeight.SemiBold)
                }
            },
            title = {
                Text("恭喜抽中稀有菜谱", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "「${recipe.name}」",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ochre
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "这道菜被抽中的概率仅为 ${pct(session.outcome.probability)}。\n全库 365 道里只有 ${state.recipes.count { it.rarity == Rarity.RARE }} 道属于稀有档，" +
                            "每一次抽取落在稀有档的机会只有 ${pct(session.outcome.tierProbability)}。",
                        fontSize = 14.sp,
                        color = InkSecondary
                    )
                }
            }
        )
    }
}
