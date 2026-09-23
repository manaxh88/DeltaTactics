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
import androidx.compose.material.icons.filled.Bolt
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
import com.delta.tactics.core.ui.theme.*
import com.delta.tactics.domain.model.BulletPack
import com.delta.tactics.domain.model.BulletPackItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulletProfitBottomSheet(
    sheetState: SheetState,
    packs: List<BulletPack>,
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
            // 顶行标题
            Text(
                text = "子弹自选包收益榜",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "活动与邮件赠送自选包 · 选取拍卖行变现收益最大化的子弹",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryGray
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 自选包档位胶囊切换栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                packs.forEachIndexed { index, pack ->
                    val isSelected = selectedPackIndex == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) PillNavBackground else IconCircleBg)
                            .clickable { selectedPackIndex = index }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pack.packName.replace("子弹自选包", "自选包"),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextPrimaryDark
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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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

                // 子弹图标徽章
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (bullet.isBest) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = if (bullet.isBest) Color(0xFF16A34A) else TacticalOrange,
                        modifier = Modifier.size(15.dp)
                    )
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
