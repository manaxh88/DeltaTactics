package com.delta.tactics.presentation.tasks

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.delta.tactics.core.ui.theme.*
import com.delta.tactics.domain.model.SeasonTask
import com.delta.tactics.domain.model.TaskReward

@Composable
fun SeasonTasksScreen(
    onBack: () -> Unit,
    viewModel: SeasonTasksViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AppBackgroundLight,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SeasonTasksTopBar(
                title = "S11 赛季任务",
                subtitle = "群星 • 全服赛季任务速查与进度打卡",
                onBack = onBack,
                onResetClick = { showResetDialog = true }
            )
        }
    ) { paddingValues ->
        val navBarsBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 110.dp + navBarsBottomPadding)
        ) {
            // 1. 赛季总览与进度打卡大白卡
            item {
                Spacer(modifier = Modifier.height(10.dp))
                SeasonProgressCard(
                    completed = uiState.totalCompletedCount,
                    total = uiState.totalQuestsCount,
                    progressPercent = uiState.overallProgressPercent,
                    filterUncompletedOnly = uiState.filterUncompletedOnly,
                    onToggleFilterUncompleted = { viewModel.toggleFilterUncompleted() }
                )
            }

            // 2. 搜索框
            item {
                Spacer(modifier = Modifier.height(12.dp))
                SeasonSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.setSearchQuery(it) },
                    onClear = { viewModel.setSearchQuery("") }
                )
            }

            // 3. 阶段分段药丸切换栏 (第一阶段: 沉舟, 第二阶段: 星火, 第三阶段: 面具, 最终阶段: 众生)
            if (uiState.searchQuery.isBlank()) {
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    SeasonPhaseSegmentedBar(
                        phases = uiState.seasonData?.phases ?: emptyList(),
                        selectedIndex = uiState.selectedPhaseIndex,
                        onSelectPhase = { viewModel.selectPhase(it) }
                    )
                }

                // 4. 阶段内支线分组标签
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    SeasonGroupChipsRow(
                        groups = uiState.currentPhaseGroups,
                        selectedGroupId = uiState.selectedGroupId,
                        allCount = uiState.currentPhase?.allQuestIds?.size ?: 0,
                        onSelectGroup = { viewModel.selectGroup(it) }
                    )
                }
            }

            // 5. 任务统计小栏
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uiState.searchQuery.isNotBlank()) "搜索结果 (${uiState.displayedTasks.size})"
                        else "${uiState.currentPhase?.stageShortName ?: "当前"}任务清单 (${uiState.displayedTasks.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    if (uiState.searchQuery.isBlank()) {
                        val (pComp, pTotal) = uiState.currentPhaseProgress
                        Text(
                            text = "本阶段进度: $pComp / $pTotal",
                            fontSize = 12.sp,
                            color = TextSecondaryGray
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 6. 任务卡片列表
            if (uiState.displayedTasks.isEmpty()) {
                item {
                    EmptyTasksPlaceholder()
                }
            } else {
                items(uiState.displayedTasks, key = { it.questId }) { task ->
                    SeasonTaskCard(
                        task = task,
                        onToggleCompleted = { viewModel.toggleQuestCompleted(task.questId) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("重置任务进度？") },
            text = { Text("清空所有已勾选的赛季任务完成状态。此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetAllProgress()
                        showResetDialog = false
                    }
                ) {
                    Text("确认重置", color = AccentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("取消", color = TextSecondaryGray)
                }
            }
        )
    }
}

/** 顶栏 */
@Composable
private fun SeasonTasksTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    onResetClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 正圆返回按钮
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
                    fontSize = 18.sp,
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

        // 清空重置进度小图标按钮
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(CardWhite)
                .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                .clickable { onResetClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "重置进度",
                tint = TextSecondaryGray,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** 进度卡片 */
@Composable
private fun SeasonProgressCard(
    completed: Int,
    total: Int,
    progressPercent: Float,
    filterUncompletedOnly: Boolean,
    onToggleFilterUncompleted: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(20.dp)),
        color = CardWhite,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                            .background(PillNavBackground)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "S11 群星",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "赛季任务全服进度",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }

                Text(
                    text = "$completed / $total",
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (completed > 0) AccentGreen else TextSecondaryGray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 进度条
            LinearProgressIndicator(
                progress = { progressPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = AccentGreen,
                trackColor = Color(0xFFF1F5F9)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "完成率: ${(progressPercent * 100).toInt()}% • 打勾记录自动持久保存",
                    fontSize = 12.sp,
                    color = TextSecondaryGray
                )

                // 仅看未完成过滤胶囊
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (filterUncompletedOnly) TacticalOrangeSoft else IconCircleBg)
                        .border(
                            1.dp,
                            if (filterUncompletedOnly) TacticalOrange else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onToggleFilterUncompleted() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (filterUncompletedOnly) "✓ 仅看未完成" else "仅看未完成",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (filterUncompletedOnly) TacticalOrange else TextSecondaryGray
                    )
                }
            }
        }
    }
}

/** 搜索栏 */
@Composable
private fun SeasonSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(16.dp)),
        color = CardWhite,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = TextSecondaryGray,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "搜索任务名、目标或武器 (如: 金枪客, QJB201)",
                        fontSize = 13.sp,
                        color = TextTertiaryLight
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )
            if (query.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "清除",
                    tint = TextSecondaryGray,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onClear() }
                )
            }
        }
    }
}

/** 4 大阶段药丸切换栏 */
@Composable
private fun SeasonPhaseSegmentedBar(
    phases: List<com.delta.tactics.domain.model.SeasonPhase>,
    selectedIndex: Int,
    onSelectPhase: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        phases.forEachIndexed { index, phase ->
            val isSelected = selectedIndex == index
            val count = phase.allQuestIds.size
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) PillNavBackground else CardWhite)
                    .border(
                        1.dp,
                        if (isSelected) PillNavBackground else Color(0xFFE2E8F0),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelectPhase(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = phase.stageShortName,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "$count 题",
                        fontSize = 10.sp,
                        color = if (isSelected) Color(0xFFCBD5E1) else TextSecondaryGray
                    )
                }
            }
        }
    }
}

/** 支线分组滑动胶囊 */
@Composable
private fun SeasonGroupChipsRow(
    groups: List<com.delta.tactics.domain.model.SeasonTaskGroup>,
    selectedGroupId: Long?,
    allCount: Int,
    onSelectGroup: (Long?) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 全部
        item {
            val isSelected = selectedGroupId == null
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                    .clickable { onSelectGroup(null) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "全部 ($allCount)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) Color.White else TextPrimaryDark
                )
            }
        }

        items(groups) { group ->
            val isSelected = selectedGroupId == group.groupId
            val shortName = if (group.name.length > 8) group.name.take(8) + "…" else group.name
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                    .clickable { onSelectGroup(group.groupId) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "$shortName (${group.questIds.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) Color.White else TextPrimaryDark
                )
            }
        }
    }
}

/** 任务卡片 */
@Composable
private fun SeasonTaskCard(
    task: SeasonTask,
    onToggleCompleted: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(18.dp)),
        color = CardWhite,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // 顶行：等级限制 + 任务名 + 完成标记按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (task.acceptRequiredLevel > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Lv.${task.acceptRequiredLevel}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondaryGray
                            )
                        }
                    }

                    Text(
                        text = task.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (task.isCompleted) TextSecondaryGray else TextPrimaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 完成打卡胶囊按钮
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (task.isCompleted) AccentGreenSoft else Color(0xFFF8FAFC))
                        .border(
                            1.dp,
                            if (task.isCompleted) AccentGreen else Color(0xFFE2E8F0),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onToggleCompleted() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (task.isCompleted) AccentGreen else TextSecondaryGray,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (task.isCompleted) "已完成" else "标记完成",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (task.isCompleted) AccentGreen else TextSecondaryGray
                        )
                    }
                }
            }

            // 任务目标清单
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                task.objectives.forEach { obj ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (task.isCompleted) TextSecondaryGray else TacticalOrange)
                        )
                        Text(
                            text = obj,
                            fontSize = 13.sp,
                            color = if (task.isCompleted) TextSecondaryGray else TextPrimaryDark,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // 任务简报展开
            if (task.desc.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (expanded) "收起简报" else "展开任务简报",
                        fontSize = 11.sp,
                        color = TextTertiaryLight
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextTertiaryLight,
                        modifier = Modifier.size(16.dp)
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF8FAFC))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = task.desc,
                            fontSize = 11.sp,
                            color = TextSecondaryGray,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // 丰厚奖励胶囊列表
            if (task.rewards.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "任务奖励",
                    fontSize = 11.sp,
                    color = TextSecondaryGray,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    task.rewards.forEach { reward ->
                        RewardBadge(reward = reward)
                    }
                }
            }
        }
    }
}

/** 奖励徽章 */
@Composable
private fun RewardBadge(reward: TaskReward) {
    val isGold = reward.isImportant || reward.grade >= 5
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isGold) TacticalGoldContainer else Color(0xFFF1F5F9))
            .border(
                1.dp,
                if (isGold) TacticalGold else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isGold) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = TacticalGold,
                    modifier = Modifier.size(12.dp)
                )
            }
            val amountText = if (reward.amount > 1) " x${reward.amount}" else ""
            Text(
                text = "${reward.name}$amountText",
                fontSize = 11.sp,
                fontWeight = if (isGold) FontWeight.Bold else FontWeight.Normal,
                color = if (isGold) TacticalGold else TextPrimaryDark
            )
        }
    }
}

/** 空搜索结果 */
@Composable
private fun EmptyTasksPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                tint = TextTertiaryLight,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "未找到相关赛季任务",
                fontSize = 14.sp,
                color = TextSecondaryGray
            )
        }
    }
}
