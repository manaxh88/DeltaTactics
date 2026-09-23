package com.delta.tactics.presentation.profit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import com.delta.tactics.core.ui.theme.*
import com.delta.tactics.domain.model.CraftBenchType
import com.delta.tactics.domain.model.CraftRecipe
import com.delta.tactics.domain.model.GunsmithBuildRepository
import com.delta.tactics.presentation.common.AsyncItemImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CraftProfitBottomSheet(
    sheetState: SheetState,
    recipes: List<CraftRecipe>,
    onDismissRequest: () -> Unit
) {
    var selectedBench by remember { mutableStateOf(CraftBenchType.ALL) }
    var sortByHourly by remember { mutableStateOf(false) } // false = 总利润, true = 时薪

    val filteredRecipes = remember(recipes, selectedBench, sortByHourly) {
        val list = if (selectedBench == CraftBenchType.ALL) recipes
        else recipes.filter { it.benchType == selectedBench }

        if (sortByHourly) list.sortedByDescending { it.hourlyProfit }
        else list.sortedByDescending { it.totalProfit }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // 顶行标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "特勤处制造利润排行",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "避难所工作台挂机点饭 · 纯净利润与时薪倒排",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryGray
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 排序切换药丸
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (!sortByHourly) PillNavBackground else Color.Transparent)
                                .clickable { sortByHourly = false }
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "总利润",
                                fontSize = 11.sp,
                                fontWeight = if (!sortByHourly) FontWeight.Bold else FontWeight.Normal,
                                color = if (!sortByHourly) Color.White else TextSecondaryGray
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (sortByHourly) PillNavBackground else Color.Transparent)
                                .clickable { sortByHourly = true }
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "时薪/h",
                                fontSize = 11.sp,
                                fontWeight = if (sortByHourly) FontWeight.Bold else FontWeight.Normal,
                                color = if (sortByHourly) Color.White else TextSecondaryGray
                            )
                        }
                    }

                    // 右上角圆形关闭按钮
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = TextPrimaryDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5 大工作台分类选择胶囊
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CraftBenchType.values().forEach { bench ->
                    val isSelected = selectedBench == bench
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) PillNavBackground else IconCircleBg)
                            .clickable { selectedBench = bench }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = bench.displayName,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextPrimaryDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 配方列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(filteredRecipes, key = { _, r -> r.id }) { index, recipe ->
                    RecipeProfitRow(
                        rank = index + 1,
                        recipe = recipe,
                        highlightHourly = sortByHourly
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipeProfitRow(
    rank: Int,
    recipe: CraftRecipe,
    highlightHourly: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        color = CardWhite,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // 排名徽章
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(
                                when (rank) {
                                    1 -> Color(0xFFFFD700).copy(alpha = 0.2f)
                                    2 -> Color(0xFFC0C0C0).copy(alpha = 0.2f)
                                    3 -> Color(0xFFCD7F32).copy(alpha = 0.2f)
                                    else -> Color(0xFFF1F5F9)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$rank",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (rank) {
                                1 -> Color(0xFFD97706)
                                2 -> Color(0xFF475569)
                                3 -> Color(0xFFB45309)
                                else -> TextSecondaryGray
                            }
                        )
                    }

                    // 武器透明高清图或工作台类型图标
                    val weaponImg = GunsmithBuildRepository.getWeaponImageUrl(recipe.name)
                    if (weaponImg.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .size(width = 46.dp, height = 28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White)
                                .border(0.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncItemImage(
                                url = weaponImg,
                                contentDescription = recipe.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (recipe.benchType) {
                                    CraftBenchType.ARMOR -> Icons.Default.Shield
                                    CraftBenchType.AMMO -> Icons.Default.Bolt
                                    CraftBenchType.MEDICAL -> Icons.Default.LocalHospital
                                    CraftBenchType.WEAPON -> Icons.Default.PrecisionManufacturing
                                    else -> Icons.Default.Construction
                                },
                                contentDescription = null,
                                tint = when (recipe.benchType) {
                                    CraftBenchType.ARMOR -> TacticalOrange
                                    CraftBenchType.AMMO -> Color(0xFFEAB308)
                                    CraftBenchType.MEDICAL -> Color(0xFF10B981)
                                    CraftBenchType.WEAPON -> Color(0xFF3B82F6)
                                    else -> TextSecondaryGray
                                },
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = recipe.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = recipe.benchName,
                                    fontSize = 10.sp,
                                    color = TextSecondaryGray
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "${recipe.durationHours}小时",
                                    fontSize = 10.sp,
                                    color = TextSecondaryGray
                                )
                            }
                        }
                    }
                }

                // 收益指标
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "+${formatWan(recipe.totalProfit)}",
                        fontSize = if (!highlightHourly) 15.sp else 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (!highlightHourly) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = Color(0xFF16A34A)
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "+${formatNumber(recipe.hourlyProfit)}/h",
                        fontSize = if (highlightHourly) 14.sp else 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (highlightHourly) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (highlightHourly) TacticalOrange else TextSecondaryGray
                    )
                }
            }

            // 展开材料清单
            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "所需原材料清单：",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondaryGray
                )
                Spacer(modifier = Modifier.height(4.dp))
                recipe.materials.forEach { mat ->
                    Text(
                        text = "• $mat",
                        fontSize = 11.sp,
                        color = TextPrimaryDark,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

private fun formatWan(num: Long): String {
    return if (num >= 10000) {
        val wan = num / 10000.0
        String.format("%.1fW", wan)
    } else {
        "$num"
    }
}

private fun formatNumber(num: Long): String {
    return java.text.NumberFormat.getIntegerInstance().format(num)
}
