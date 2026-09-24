package com.delta.tactics.presentation.profit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delta.tactics.core.ui.theme.*
import com.delta.tactics.domain.model.BulletPack
import com.delta.tactics.domain.model.BulletPackItem

import androidx.compose.ui.layout.ContentScale
import com.delta.tactics.presentation.common.AsyncItemImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulletProfitBottomSheet(
    sheetState: SheetState,
    packs: List<BulletPack>,
    updateTimeText: String = "",
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onDismissRequest: () -> Unit
) {
    var selectedPackIndex by remember { mutableIntStateOf(0) }

    val currentPack = remember(packs, selectedPackIndex) {
        if (packs.isNotEmpty() && selectedPackIndex in packs.indices) {
            packs[selectedPackIndex]
        } else null
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
            // 顶行标题与关闭按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "子弹自选包收益榜",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (updateTimeText.isNotBlank()) updateTimeText else "活动与邮件赠送自选包 · 选取拍卖行变现收益最大化的子弹",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryGray
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 刷新按钮
                    IconButton(
                        onClick = { if (!isRefreshing) onRefresh() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = TacticalOrange
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "刷新",
                                tint = TextPrimaryDark,
                                modifier = Modifier.size(18.dp)
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

            // 自选包档位胶囊切换栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                packs.forEachIndexed { index, pack ->
                    val isSelected = selectedPackIndex == index
                    val tabTitle = when {
                        pack.packName.contains("通行证") -> "通行证自选"
                        pack.packName.contains("3级") -> "3级自选包"
                        pack.packName.contains("4级") -> "4级自选包"
                        pack.packName.contains("5级") -> "5级自选包"
                        else -> pack.packName.replace("子弹自选包", "自选包")
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) PillNavBackground else IconCircleBg)
                            .clickable { selectedPackIndex = index }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabTitle,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextPrimaryDark,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            currentPack?.let { pack ->
                // 推荐最优解高亮 Banner
                Surface(
                    color = Color(0xFFF0FDF4),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (pack.imageUrl.isNotBlank()) {
                                AsyncItemImage(
                                    url = pack.imageUrl,
                                    contentDescription = pack.packName,
                                    modifier = Modifier.size(24.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF16A34A))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "首选推荐",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = pack.recommendedBullet,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }

                        Text(
                            text = "单包折合 ${formatWan(pack.recommendedValue)}",
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 子弹价值倒排列表
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pack.bullets, key = { it.name }) { bullet ->
                        BulletOptionRow(bullet = bullet)
                    }
                }
            }
        }
    }
}

@Composable
private fun BulletOptionRow(bullet: BulletPackItem) {
    val gradeColor = when (bullet.grade) {
        6 -> Color(0xFFDC2626) // 红色
        5 -> Color(0xFFD97706) // 金色
        4 -> Color(0xFF9333EA) // 紫色
        3 -> Color(0xFF2563EB) // 蓝色
        else -> if (bullet.isBest) Color(0xFF16A34A) else Color(0xFF94A3B8)
    }

    Surface(
        color = CardWhite,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (bullet.isBest) Color(0xFF86EFAC) else Color(0xFFF1F5F9),
                RoundedCornerShape(14.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // 排名
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (bullet.isBest) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${bullet.rank}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (bullet.isBest) Color(0xFF16A34A) else TextSecondaryGray
                    )
                }

                // 子弹官方 3D 渲染图 (带品质边框)
                if (bullet.imageUrl.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, gradeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncItemImage(
                            url = bullet.imageUrl,
                            contentDescription = bullet.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            fallback = {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = gradeColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (bullet.isBest) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if (bullet.isBest) Color(0xFF16A34A) else TacticalOrange,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = bullet.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark
                        )
                        if (bullet.isBest) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFDCFCE7))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "收益最高",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16A34A)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${bullet.count} 发 · 单价 ${bullet.unitPrice} 币",
                        fontSize = 11.sp,
                        color = TextSecondaryGray
                    )
                }
            }

            // 总折合价值
            Text(
                text = "${formatNumber(bullet.totalValue)} 币",
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (bullet.isBest) Color(0xFF16A34A) else TextPrimaryDark
            )
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
