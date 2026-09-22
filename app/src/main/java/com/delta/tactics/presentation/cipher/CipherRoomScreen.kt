package com.delta.tactics.presentation.cipher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delta.tactics.core.ui.theme.TacticalBlack
import com.delta.tactics.core.ui.theme.TacticalBorder
import com.delta.tactics.core.ui.theme.TacticalCyan
import com.delta.tactics.core.ui.theme.TacticalGold
import com.delta.tactics.core.ui.theme.TacticalGreen
import com.delta.tactics.core.ui.theme.TacticalOrange
import com.delta.tactics.core.ui.theme.TacticalOrangeContainer
import com.delta.tactics.core.ui.theme.TacticalRed
import com.delta.tactics.core.ui.theme.TacticalSurface
import com.delta.tactics.core.ui.theme.TacticalSurfaceVariant
import com.delta.tactics.core.ui.theme.TextPrimary
import com.delta.tactics.core.ui.theme.TextSecondary
import com.delta.tactics.core.ui.theme.TextTertiary
import com.delta.tactics.domain.model.CipherRoom
import com.delta.tactics.domain.model.TacticalMap
import kotlinx.coroutines.launch

@Composable
fun CipherRoomScreen(
    viewModel: CipherRoomViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = TacticalBlack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TacticalTopHeader()
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 搜索输入框
            SearchBarSection(
                query = uiState.searchQuery,
                onQueryChanged = viewModel::onSearchQueryChanged,
                onClearQuery = viewModel::clearSearch
            )

            // 地图分类过滤栏
            MapFilterChipsRow(
                selectedMap = uiState.selectedMap,
                onMapSelected = viewModel::onMapSelected
            )

            // 统计栏
            TacticalStatusBar(
                count = uiState.totalCount,
                selectedMap = uiState.selectedMap
            )

            // 列表内容
            if (uiState.rooms.isEmpty()) {
                EmptyStateView(query = uiState.searchQuery)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                ) {
                    items(
                        items = uiState.rooms,
                        key = { it.id }
                    ) { room ->
                        CipherRoomCard(
                            room = room,
                            onCopyCode = { code, roomName ->
                                clipboardManager.setText(AnnotatedString(code))
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("已复制 [$roomName] 密码: $code")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TacticalTopHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TacticalSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(TacticalGreen)
            )
            Text(
                text = "ONLINE // TACTICAL CIPHER HUB",
                style = MaterialTheme.typography.labelSmall,
                color = TacticalGreen
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "三角洲战术助手",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Box(
                modifier = Modifier
                    .border(1.dp, TacticalOrange, RoundedCornerShape(4.dp))
                    .background(TacticalOrangeContainer)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "密码房速查",
                    style = MaterialTheme.typography.labelSmall,
                    color = TacticalOrange
                )
            }
        }
    }
}

@Composable
private fun SearchBarSection(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = {
            Text(
                text = "输入点位、密码、掉落物资搜索...",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索",
                tint = TacticalOrange
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = onClearQuery) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "清空",
                        tint = TextSecondary
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = TacticalSurfaceVariant,
            unfocusedContainerColor = TacticalSurfaceVariant,
            focusedBorderColor = TacticalOrange,
            unfocusedBorderColor = TacticalBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        )
    )
}

@Composable
private fun MapFilterChipsRow(
    selectedMap: TacticalMap,
    onMapSelected: (TacticalMap) -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TacticalMap.entries.forEach { map ->
            val isSelected = map == selectedMap
            FilterChip(
                selected = isSelected,
                onClick = { onMapSelected(map) },
                label = {
                    Text(
                        text = map.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = TacticalSurface,
                    labelColor = TextSecondary,
                    selectedContainerColor = TacticalOrangeContainer,
                    selectedLabelColor = TacticalOrange
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = TacticalBorder,
                    selectedBorderColor = TacticalOrange,
                    borderWidth = 1.dp
                ),
                shape = RoundedCornerShape(6.dp)
            )
        }
    }
}

@Composable
private fun TacticalStatusBar(
    count: Int,
    selectedMap: TacticalMap
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "地图范围: ${selectedMap.displayName} [${selectedMap.englishName}]",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
        )
        Text(
            text = "收录密码房: $count 处",
            style = MaterialTheme.typography.labelSmall,
            color = TacticalCyan
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CipherRoomCard(
    room: CipherRoom,
    onCopyCode: (code: String, name: String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    val mapColor = when (room.map) {
        TacticalMap.ZERO_DAM -> TacticalCyan
        TacticalMap.LONGBOW_VALLEY -> TacticalGreen
        TacticalMap.BARKASH -> TacticalOrange
        TacticalMap.SPACE_CITY -> TacticalGold
        else -> TacticalCyan
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TacticalBorder, RoundedCornerShape(8.dp))
            .animateContentSize(animationSpec = tween(200)),
        color = TacticalSurface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // 顶部信息：所属地图徽章 + 危险等级
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 地图标签
                Box(
                    modifier = Modifier
                        .border(1.dp, mapColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .background(mapColor.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = room.map.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = mapColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 危险度指示器
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = if (room.dangerLevel >= 5) TacticalRed else TacticalGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (room.dangerLevel >= 5) "高危交战区" else "中度威胁",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (room.dangerLevel >= 5) TacticalRed else TacticalGold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 房间名称
            Text(
                text = room.roomName,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 核心密码展示框与一键复制按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TacticalBlack, RoundedCornerShape(6.dp))
                    .border(1.dp, TacticalBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "DOOR ACCESS CODE",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        fontSize = 10.sp
                    )
                    Text(
                        text = room.code,
                        style = MaterialTheme.typography.headlineLarge,
                        fontFamily = FontFamily.Monospace,
                        color = TacticalOrange,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    )
                }

                Button(
                    onClick = { onCopyCode(room.code, room.roomName) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TacticalOrange,
                        contentColor = TacticalBlack
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "复制密码",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "复制",
                        style = MaterialTheme.typography.labelLarge,
                        color = TacticalBlack
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 点位路线描述
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = room.locationDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 高价值物资标签
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                room.lootHighlights.forEach { loot ->
                    Box(
                        modifier = Modifier
                            .background(TacticalSurfaceVariant, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = loot,
                            style = MaterialTheme.typography.labelSmall,
                            color = TacticalCyan
                        )
                    }
                }
            }

            // 展开折叠战术指南
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isExpanded) "收起战术提示" else "查看破门及清角战术提示",
                    style = MaterialTheme.typography.labelSmall,
                    color = TacticalOrange
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TacticalOrange,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .background(TacticalBlack.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .border(1.dp, TacticalBorder.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "💡 战术建议：${room.tacticalTips}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateView(query: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (query.isNotBlank()) "未匹配到与「$query」相关的密码房" else "暂无该地图密码房数据",
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "尝试切换地图分类或搜索其它关键词（如：行政楼、保险箱、大仓）",
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary
        )
    }
}
