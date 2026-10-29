package com.chisha.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.chisha.app.ui.theme.InkPrimary
import com.chisha.app.ui.theme.InkSecondary
import com.chisha.app.ui.theme.InkTertiary
import com.chisha.app.ui.theme.WarmOutline
import com.chisha.app.ui.theme.WarmSurface

/** 火候配色：越大火越红，越小火越青 */
private fun fireColor(fire: String): Color = when {
    fire.contains("大火") || fire.contains("旺火") || fire.contains("猛") -> Color(0xFFC0392B)
    fire.contains("中火") -> Color(0xFFD98026)
    fire.contains("小火") || fire.contains("微火") -> Color(0xFF3F8F7B)
    fire.contains("关火") || fire.contains("余温") -> Color(0xFF8A7A6A)
    else -> Color(0xFF8A7A6A)
}

/** 炒菜流程：做菜时抬头就能对上「现在该多大火、看什么信号」 */
@Composable
fun HeatTimeline(recipe: Recipe) {
    if (recipe.heat.isEmpty()) return
    Column(Modifier.fillMaxWidth()) {
        recipe.heat.forEachIndexed { i, h ->
            Row(Modifier.fillMaxWidth()) {
                // 左侧时间轴：圆点 + 竖线
                Column(
                    Modifier
                        .width(20.dp)
                        .padding(top = 13.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(
                        Modifier
                            .width(9.dp)
                            .height(9.dp)
                            .clip(RoundedCornerShape(50))
                            .background(fireColor(h.fire))
                    )
                    if (i != recipe.heat.lastIndex) {
                        Spacer(Modifier.height(2.dp))
                        Box(
                            Modifier
                                .width(2.dp)
                                .height(84.dp)
                                .background(WarmOutline.copy(alpha = 0.7f))
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            h.stage,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = InkPrimary,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FireChip(h.fire)
                            if (h.time.isNotBlank()) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    h.time,
                                    fontSize = 11.5.sp,
                                    color = InkTertiary
                                )
                            }
                        }
                    }
                    if (h.detail.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            h.detail,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = InkPrimary
                        )
                    }
                    if (h.signal.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                "判断",
                                fontSize = 10.5.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF3F8F7B))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                h.signal,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = InkSecondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun FireChip(fire: String) {
    if (fire.isBlank()) return
    val c = fireColor(fire)
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(c.copy(alpha = 0.14f))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(fire, fontSize = 11.sp, color = c, fontWeight = FontWeight.Medium)
    }
}

/** 翻车对照表：症状 → 补救，比一段散文更容易在出问题时扫到 */
@Composable
fun FailTable(recipe: Recipe) {
    if (recipe.fails.isEmpty()) return
    Column(Modifier.fillMaxWidth()) {
        recipe.fails.forEachIndexed { i, f ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFF3EE))
                    .padding(12.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            "症状",
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFB3402A))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            f.symptom,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = InkPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(7.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            "补救",
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF3F8F7B))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            f.fix,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = InkSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            if (i != recipe.fails.lastIndex) Spacer(Modifier.height(8.dp))
        }
    }
}

/** 调味料投放表：按时间先后列出「什么时候放 → 放什么 → 为什么这时候放」 */
@Composable
fun SeasoningTable(recipe: Recipe) {
    if (recipe.seasoning.isEmpty()) return
    Column(Modifier.fillMaxWidth()) {
        recipe.seasoning.forEachIndexed { i, s ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFFF3E4CF))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        s.time,
                        fontSize = 11.sp,
                        color = Color(0xFF8A5A22),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        s.name,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = InkPrimary
                    )
                    if (s.why.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            s.why,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = InkSecondary
                        )
                    }
                }
            }
            if (i != recipe.seasoning.lastIndex) {
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

/** 菜谱页用的小节头 + 卡片容器 */
@Composable
fun ExtraSection(title: String, subtitle: String = "", content: @Composable () -> Unit) {
    Spacer(Modifier.height(18.dp))
    SectionTitle(title)
    if (subtitle.isNotBlank()) {
        Spacer(Modifier.height(4.dp))
        Text(subtitle, fontSize = 11.5.sp, color = InkTertiary, lineHeight = 17.sp)
    }
    Spacer(Modifier.height(10.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WarmSurface)
            .padding(14.dp)
    ) {
        content()
    }
}
