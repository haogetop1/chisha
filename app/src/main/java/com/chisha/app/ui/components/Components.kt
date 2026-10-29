package com.chisha.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chisha.app.data.rememberDishPhoto
import com.chisha.app.domain.MealSlot
import com.chisha.app.domain.Rarity
import com.chisha.app.domain.Recipe
import com.chisha.app.ui.theme.InkSecondary
import com.chisha.app.ui.theme.InkTertiary
import com.chisha.app.ui.theme.Ochre
import com.chisha.app.ui.theme.WarmOutline
import com.chisha.app.ui.theme.WarmSurface

/** 稀有度徽章：家常/大菜/稀有三个标签，标签永不变，只有权重会被评分拉低 */
@Composable
fun RarityBadge(rarity: Rarity, modifier: Modifier = Modifier) {
    val (bg, fg) = when (rarity) {
        Rarity.COMMON -> Color(0xFFF1F0EE) to Color(0xFF8A8A8A)
        Rarity.SIGNATURE -> Color(0xFFF1EBFA) to rarity.color
        Rarity.RARE -> Color(0xFFFDF3D8) to Color(0xFF9A6B00)
    }
    Box(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            rarity.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = fg
        )
    }
}

/** 菜品视觉：菜系色渐变 + 菜名首字，稀有菜加金色描边 */
@Composable
fun RecipeArtwork(
    recipe: Recipe,
    modifier: Modifier = Modifier,
    corner: Int = 18,
    showName: Boolean = false
) {
    val base = recipe.cuisine.color
    val shape = RoundedCornerShape(corner.dp)
    val photo = rememberDishPhoto(recipe.id)
    Box(
        modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(base, base.copy(alpha = 0.72f), base.copy(alpha = 0.5f))
                )
            )
            .then(
                if (recipe.rarity == Rarity.RARE) {
                    Modifier.border(2.dp, Color(0xFFE8C36A), shape)
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (photo != null) {
            // 有成品图：铺满裁切，底部压一层渐变保证菜名看得清
            Image(
                bitmap = photo,
                contentDescription = recipe.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                recipe.name.take(1),
                fontSize = 60.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.92f)
            )
        }
        if (showName || photo != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Transparent, Color(0x99000000))
                        )
                    ),
                contentAlignment = Alignment.BottomStart
            ) {
                Text(
                    recipe.name,
                    modifier = Modifier.padding(10.dp),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

/** 等级经验条 */
@Composable
fun XpBar(
    level: Int,
    into: Int,
    need: Int,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    val target = if (need <= 0) 1f else (into.toFloat() / need).coerceIn(0f, 1f)
    val anim by animateFloatAsState(target, tween(600), label = "xp")
    Column(modifier) {
        if (showLabel) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Lv.$level ${com.chisha.app.domain.XpEngine.title(level)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (level >= 10) "已满级" else "$into / $need",
                    fontSize = 12.sp,
                    color = InkTertiary
                )
            }
            Spacer(Modifier.height(6.dp))
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFF0E4D6))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(anim)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFE0A03C), Ochre)
                        )
                    )
            )
        }
    }
}

@Composable
fun SlotChip(slot: MealSlot, used: Boolean, modifier: Modifier = Modifier) {
    val bg = if (used) Color(0xFFEFEAE3) else WarmSurface
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, WarmOutline, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            if (used) "${slot.label} · 已完成" else slot.label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (used) InkTertiary else InkSecondary
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
}
