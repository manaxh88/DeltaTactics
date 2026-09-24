package com.delta.tactics.presentation.loadout

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.viewmodel.compose.viewModel
import com.delta.tactics.domain.model.*
import com.delta.tactics.presentation.common.AsyncItemImage

// 现代极简战术调色板
private val BgLightGray = Color(0xFFF8FAFC)
private val CardWhite = Color(0xFFFFFFFF)
private val TextPrimaryDark = Color(0xFF0F172A)
private val TextSecondaryGray = Color(0xFF64748B)
private val TextTertiaryLight = Color(0xFF94A3B8)
private val TacticalOrange = Color(0xFFFF5500)
private val TacticalDark = Color(0xFF1E293B)
private val AccentGreen = Color(0xFF10B981)
private val AccentGreenSoft = Color(0xFFECFDF5)
private val AccentGold = Color(0xFFD97706)
private val AccentGoldSoft = Color(0xFFFEF3C7)

@Composable
fun CardLoadoutScreen(
    onBack: () -> Unit,
    viewModel: CardLoadoutViewModel = viewModel()
) {
    BackHandler {
        onBack()
    }

    val loadoutData by viewModel.loadoutData.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val selectedTierIndex by viewModel.selectedTierIndex.collectAsState()
    val selectedPlanType by viewModel.selectedPlanType.collectAsState()

    val currentTier = remember(loadoutData, selectedTierIndex) {
        if (loadoutData.tiers.isNotEmpty() && selectedTierIndex in loadoutData.tiers.indices) {
            loadoutData.tiers[selectedTierIndex]
        } else null
    }

    val filteredPlans = remember(currentTier, selectedPlanType) {
        if (currentTier == null) emptyList()
        else if (selectedPlanType == null) currentTier.plans
        else currentTier.plans.filter { it.name == selectedPlanType }
    }

    Scaffold(
        containerColor = BgLightGray,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LoadoutTopBar(
                title = "鼠鼠卡战备",
                subtitle = "低成本进图 · 假账套利速查",
                updateTime = loadoutData.updateTime,
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh(force = true) },
                onBack = onBack
            )
        }
    ) { paddingValues ->
        val navBarsBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 130.dp + navBarsBottomPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 门槛档位切换大卡片行 (11W / 18W / 55W / 60W / 78W)
            item {
                TierSelectorCarousel(
                    tiers = loadoutData.tiers,
                    selectedIndex = selectedTierIndex,
                    onSelectTier = { viewModel.selectTier(it) }
                )
            }

            // 2. 当前门槛概述与地图要求 Banner
            currentTier?.let { tier ->
                item {
                    TierSummaryBanner(tier = tier)
                }

                // 3. 流派方案分类胶囊 (全部 / 枪械优先 / 均衡套装 / 胸挂优先)
                item {
                    PlanTypeChips(
                        plans = tier.plans,
                        selectedType = selectedPlanType,
                        onSelectType = { viewModel.selectPlanType(it) }
                    )
                }

                // 4. 方案卡片列表
                items(filteredPlans, key = { "${it.name}_${it.price}_${it.jz}" }) { plan ->
                    LoadoutPlanCard(plan = plan, thresholdValue = tier.thresholdValue)
                }
            }

            // 5. 战术技巧与卡战备原理说明卡片
            item {
                CardLoadoutTacticalTipsCard()
            }
        }
    }
}

/** 顶栏 */
@Composable
private fun LoadoutTopBar(
    title: String,
    subtitle: String,
    updateTime: String,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 正圆返回键
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(CardWhite)
                    .border(1.2.dp, Color(0xFFE2E8F0), CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = TextPrimaryDark,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextSecondaryGray
                )
            }
        }

        // 更新时间与实时刷新按钮
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF1F5F9))
                .clickable { onRefresh() }
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(10.dp),
                    strokeWidth = 1.5.dp,
                    color = TacticalOrange
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "正在同步...",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = TacticalOrange
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "刷新",
                    tint = TextSecondaryGray,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (updateTime.isNotBlank()) "更新 $updateTime" else "点击刷新",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondaryGray
                )
            }
        }
    }
}

/** 5 档门槛选择行 */
@Composable
private fun TierSelectorCarousel(
    tiers: List<CardLoadoutTier>,
    selectedIndex: Int,
    onSelectTier: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            tiers.forEachIndexed { index, tier ->
                val isSelected = selectedIndex == index
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) TacticalDark else CardWhite
                        )
                        .border(
                            1.dp,
                            if (isSelected) TacticalDark else Color(0xFFE2E8F0),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelectTier(index) }
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = tier.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isSelected) TacticalOrange else TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = tier.maps.split("—").firstOrNull() ?: tier.maps,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) Color(0xFFCBD5E1) else TextSecondaryGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/** 当前档位概述 Banner */
@Composable
private fun TierSummaryBanner(tier: CardLoadoutTier) {
    // 找出该档位中立省最多的方案
    val bestSavingsPlan = tier.plans.minByOrNull { it.cz } // cz 越小越负，省得越多
    val bestSavings = if (bestSavingsPlan != null) kotlin.math.abs(bestSavingsPlan.cz) else 0L

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(16.dp)),
        color = CardWhite,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TacticalDark)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${tier.name} 门槛",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = tier.maps,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark
                    )
                }

                if (bestSavings > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentGreenSoft)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "最高立省 ${formatWan(bestSavings)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "目标入场战备要求 ≥ ${formatNumber(tier.thresholdValue)} 哈夫币，通过精选高战备配件可大幅降低实际购买花费。",
                fontSize = 12.sp,
                color = TextSecondaryGray,
                lineHeight = 17.sp
            )
        }
    }
}

/** 流派分类横向胶囊 */
@Composable
private fun PlanTypeChips(
    plans: List<CardLoadoutPlan>,
    selectedType: String?,
    onSelectType: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val isAll = selectedType == null
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (isAll) TacticalDark else Color(0xFFF1F5F9))
                .clickable { onSelectType(null) }
                .padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Text(
                text = "全部方案 (${plans.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isAll) Color.White else TextPrimaryDark
            )
        }

        plans.forEach { plan ->
            val isSelected = selectedType == plan.name
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) TacticalDark else Color(0xFFF1F5F9))
                    .clickable { onSelectType(plan.name) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = plan.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) Color.White else TextPrimaryDark
                )
            }
        }
    }
}

/** 配装方案详情卡片 */
@Composable
private fun LoadoutPlanCard(
    plan: CardLoadoutPlan,
    thresholdValue: Long
) {
    val treeNodes = remember(plan) { plan.data.toTreeNodes() }
    val savings = kotlin.math.abs(plan.cz)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(18.dp)),
        color = CardWhite,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 顶行：流派名称 + 核心三指标
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    plan.name.contains("枪械") -> Color(0xFFEFF6FF)
                                    plan.name.contains("胸挂") -> Color(0xFFFEF3C7)
                                    else -> Color(0xFFF0FDF4)
                                }
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = plan.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                plan.name.contains("枪械") -> Color(0xFF1D4ED8)
                                plan.name.contains("胸挂") -> Color(0xFFB45309)
                                else -> Color(0xFF15803D)
                            }
                        )
                    }

                    if (plan.jz >= thresholdValue) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AccentGreenSoft)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "✓ 达标",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGreen
                            )
                        }
                    }
                }

                // 假账立省金额
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentGreenSoft)
                        .border(1.dp, AccentGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "立省",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = AccentGreen
                        )
                        Text(
                            text = formatWan(savings),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = AccentGreen
                        )
                    }
                }
            }

            val mainGun = remember(plan) {
                plan.data.firstOrNull { (it.type.startsWith("枪") || it.type.startsWith("手枪")) && !it.type.contains("-") }
            }
            val context = LocalContext.current
            val clipboardManager = LocalClipboardManager.current

            // 方案主武器图片卡片与一键方案复制 (支持避难所抄作业)
            if (mainGun != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(0.8.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 64.dp, height = 38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .border(0.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (mainGun.pic.isNotBlank()) {
                                AsyncItemImage(
                                    url = mainGun.pic,
                                    contentDescription = mainGun.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = TextSecondaryGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = mainGun.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(TacticalDark)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "推荐核心",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "单枪花费: ${formatWan(mainGun.price)}",
                                fontSize = 11.sp,
                                color = TextSecondaryGray
                            )
                        }
                    }

                    // 一键复制避难所卡战备方案 / 改枪清单
                    Surface(
                        onClick = {
                            val copyText = buildString {
                                appendLine("【三角洲行动 • 卡战备抄作业方案】")
                                appendLine("方案: ${plan.name} (目标战备 ≥ ${thresholdValue / 10000}W)")
                                appendLine("主武器: ${mainGun.name}")
                                appendLine("实际花费: ${plan.price} 币 | 系统战备: ${plan.jz} 币 (立省 ${savings} 币)")
                                appendLine("装配明细:")
                                plan.data.forEach { item ->
                                    appendLine("- ${item.name} [${item.type}] : ${item.price} 币")
                                }
                            }
                            clipboardManager.setText(AnnotatedString(copyText))
                            Toast.makeText(context, "已复制「${plan.name}」完整配装清单！", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = TacticalDark,
                        modifier = Modifier.height(30.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "复制方案",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 核心金额三栏卡片
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 实际花费
                Column {
                    Text(text = "实际花费", fontSize = 10.sp, color = TextSecondaryGray)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatWan(plan.price),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "${formatNumber(plan.price)} 币",
                        fontSize = 10.sp,
                        color = TextTertiaryLight
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(26.dp)
                        .background(Color(0xFFE2E8F0))
                )

                // 评估战备
                Column {
                    Text(text = "系统战备", fontSize = 10.sp, color = TextSecondaryGray)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatWan(plan.jz),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TacticalOrange
                    )
                    Text(
                        text = "${formatNumber(plan.jz)} 币",
                        fontSize = 10.sp,
                        color = TextTertiaryLight
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(26.dp)
                        .background(Color(0xFFE2E8F0))
                )

                // 差价/假账
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "假账差额", fontSize = 10.sp, color = TextSecondaryGray)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${if (plan.cz <= 0) "-" else "+"}${formatWan(savings)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (plan.cz <= 0) AccentGreen else Color(0xFFEF4444)
                    )
                    Text(
                        text = "战备虚高",
                        fontSize = 10.sp,
                        color = AccentGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // 配件树形层级清单
            Text(
                text = "推荐配置明细 (${plan.data.size} 件)",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondaryGray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                treeNodes.forEach { node ->
                    // 主体装备项
                    LoadoutItemRow(item = node.parent, isChild = false)

                    // 下挂配件树（如枪管、六倍镜、护木等）
                    node.children.forEach { childItem ->
                        LoadoutItemRow(item = childItem, isChild = true)
                    }
                }
            }
        }
    }
}

/** 单个配件行组件（支持下挂层级树） */
@Composable
private fun LoadoutItemRow(
    item: CardLoadoutItem,
    isChild: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isChild) 20.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 如果是子节点，绘制树形分支线指示符
        if (isChild) {
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "└─",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFCBD5E1)
                )
            }
        }

        // 装备卡条
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isChild) Color(0xFFF8FAFC) else Color(0xFFF1F5F9))
                .border(
                    0.8.dp,
                    if (isChild) Color(0xFFF1F5F9) else Color(0xFFE2E8F0),
                    RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                // 装备品质图片 / 分类图标
                Box(
                    modifier = Modifier
                        .size(if (isChild) 28.dp else 34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White)
                        .border(1.dp, getGradeBadgeColor(item.grade).copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.pic.isNotBlank()) {
                        AsyncItemImage(
                            url = item.pic,
                            contentDescription = item.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(2.dp),
                            contentScale = ContentScale.Fit,
                            fallback = {
                                Icon(
                                    imageVector = getItemCategoryIcon(item.type, item.name),
                                    contentDescription = null,
                                    tint = getGradeBadgeColor(item.grade),
                                    modifier = Modifier.size(if (isChild) 14.dp else 18.dp)
                                )
                            }
                        )
                    } else {
                        Icon(
                            imageVector = getItemCategoryIcon(item.type, item.name),
                            contentDescription = null,
                            tint = getGradeBadgeColor(item.grade),
                            modifier = Modifier.size(if (isChild) 14.dp else 18.dp)
                        )
                    }
                }

                // 装备名称
                Text(
                    text = item.name,
                    fontSize = if (isChild) 12.sp else 13.sp,
                    fontWeight = if (isChild) FontWeight.Normal else FontWeight.SemiBold,
                    color = TextPrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 价格与假账贡献标签
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 市场售价
                Text(
                    text = "${formatNumber(item.price)} 币",
                    fontSize = 11.sp,
                    color = TextSecondaryGray
                )

                // 单件假账差额 (jz < 0 表示战备虚高，省钱)
                if (item.jz != 0L) {
                    val isFavorable = item.jz < 0
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isFavorable) AccentGreenSoft else Color(0xFFFEE2E2))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${if (isFavorable) "" else "+"}${formatNumber(item.jz)}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFavorable) AccentGreen else Color(0xFFDC2626)
                        )
                    }
                }
            }
        }
    }
}

/** 底部战术科普与技巧卡片 */
@Composable
private fun CardLoadoutTacticalTipsCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(16.dp)),
        color = CardWhite,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = TacticalOrange,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "卡战备核心机制与技巧",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "1. 机密与绝密地图拥有严格的入场战备门槛，而系统战备评估值与拍卖行实时市价经常脱钩；\n" +
                        "2. 优先利用如 ACOG精准六倍镜、破损防暴头盔、特定大容量战术胸挂等‘虚高假账配件’，花费极低市价即可轻松凑够几十万战备；\n" +
                        "3. 进图后即使遭遇不幸或丢包撤离，因实际花费极低，单局战损降至最低，是跑刀摸金、做任务的高胜率首选策略！",
                fontSize = 12.sp,
                color = TextSecondaryGray,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "数据来源：三角洲数据帝 & 三角洲鼠鼠工具 (shushu.fan)",
                fontSize = 10.sp,
                color = TextTertiaryLight
            )
        }
    }
}

/** 工具：格式化为万 (W) */
private fun formatWan(amount: Long): String {
    return String.format(java.util.Locale.US, "%.1fW", amount / 10000.0)
}

/** 工具：格式化千分位数字 */
private fun formatNumber(number: Long): String {
    return java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(number)
}

/** 工具：根据装备类型匹配图标 */
private fun getItemCategoryIcon(type: String, name: String): ImageVector {
    return when {
        type.contains("瞄具") || name.contains("倍镜") -> Icons.Default.Visibility
        type.contains("枪管") || type.contains("护木") || type.contains("枪口") -> Icons.Default.Build
        type.contains("枪") -> Icons.Default.SportsScore
        type.contains("防具") || name.contains("防弹") || name.contains("头盔") -> Icons.Default.Shield
        type.contains("胸挂") || type.contains("弹挂") -> Icons.Default.MilitaryTech
        type.contains("包") || name.contains("背包") -> Icons.Default.Luggage
        else -> Icons.Default.Adjust
    }
}
