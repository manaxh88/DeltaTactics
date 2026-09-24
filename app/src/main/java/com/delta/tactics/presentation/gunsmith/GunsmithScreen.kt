package com.delta.tactics.presentation.gunsmith

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delta.tactics.core.ui.theme.*
import com.delta.tactics.data.repository.GunsmithRepository
import com.delta.tactics.domain.model.GunsmithBuild
import com.delta.tactics.presentation.common.AsyncItemImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 排序选项
 */
enum class GunsmithSortType(val label: String) {
    DEFAULT("🔥 热门推荐"),
    PRICE_ASC("💰 经济造价"),
    PRICE_DESC("💎 顶级满改"),
    PRO_TEAM("👑 战队精选"),
    ACCESSORIES("🔧 核心配件")
}

/**
 * 顶级独立改枪页面 (GunsmithScreen)
 * 底栏第 4 项专属页面，具备全服 188 套腾讯官方方案库、真实满改 3D 大图、多维分类、造价排序与秒级一键复制导入
 */
@Composable
fun GunsmithScreen(
    onBack: (() -> Unit)? = null,
    onShowToast: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { GunsmithRepository(context) }
    var allBuilds by remember { mutableStateOf(repository.getBuilds()) }
    var isRefreshing by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()

    val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarsBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var selectedCategory by remember { mutableStateOf("全部") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedSort by remember { mutableStateOf(GunsmithSortType.DEFAULT) }
    var selectedHotTag by remember { mutableStateOf<String?>(null) }

    val categories = remember {
        listOf("全部", "突击步枪", "冲锋枪", "狙击步枪", "射手步枪", "轻机枪/霰弹", "手枪/特种")
    }

    val hotTags = remember {
        listOf("全部", "M4A1", "M7", "K416", "AS Val", "AWM", "勇士", "MP5", "93R", "无后座", "跑刀流")
    }

    // 后台初次挂载时静默检查官方最新方案
    LaunchedEffect(Unit) {
        val updated = repository.fetchOfficialBuilds(force = false)
        if (updated.isNotEmpty()) {
            allBuilds = updated
        }
    }

    // 处理后退事件
    BackHandler(enabled = searchQuery.isNotEmpty() || selectedCategory != "全部") {
        if (searchQuery.isNotEmpty()) {
            searchQuery = ""
            selectedHotTag = null
        } else if (selectedCategory != "全部") {
            selectedCategory = "全部"
        }
    }

    // 统计各分类方案数
    val categoryCounts = remember(allBuilds) {
        val counts = mutableMapOf<String, Int>()
        counts["全部"] = allBuilds.size
        for (cat in listOf("突击步枪", "冲锋枪", "狙击步枪", "射手步枪", "轻机枪/霰弹", "手枪/特种")) {
            counts[cat] = allBuilds.count { it.category == cat }
        }
        counts
    }

    // 筛选与排序
    val filteredBuilds = remember(allBuilds, selectedCategory, searchQuery, selectedSort) {
        val q = searchQuery.trim().lowercase()
        var result = allBuilds.filter { build ->
            val matchCategory = if (selectedCategory == "全部") true else build.category == selectedCategory
            val matchSearch = if (q.isBlank()) true else {
                build.gunName.lowercase().contains(q) ||
                build.roleName.lowercase().contains(q) ||
                build.author.lowercase().contains(q) ||
                build.caliber.lowercase().contains(q) ||
                build.specs.lowercase().contains(q) ||
                build.pros.any { it.lowercase().contains(q) } ||
                build.keyAccessories.any { it.lowercase().contains(q) }
            }
            matchCategory && matchSearch
        }

        result = when (selectedSort) {
            GunsmithSortType.DEFAULT -> result
            GunsmithSortType.PRICE_ASC -> result.sortedBy { b: GunsmithBuild -> if (b.price > 0) b.price else Long.MAX_VALUE }
            GunsmithSortType.PRICE_DESC -> result.sortedByDescending { it.price }
            GunsmithSortType.PRO_TEAM -> result.sortedByDescending {
                val a = it.author.lowercase()
                a.contains("estar") || a.contains("jdg") || a.contains("lgd") ||
                a.contains("ag") || a.contains("狼队") || a.contains("wbg") ||
                a.contains("情久") || a.contains("天霸") || a.contains("官方")
            }
            GunsmithSortType.ACCESSORIES -> result.sortedByDescending { it.keyAccessories.size }
        }

        result
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackgroundLight),
        contentPadding = PaddingValues(
            top = statusBarTopPadding + 12.dp,
            bottom = 130.dp + navBarsBottomPadding,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. 顶栏标题与统计徽标 + 刷新按钮
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "枪械工坊 • 战术改枪码",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(TacticalOrange.copy(alpha = 0.12f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${filteredBuilds.size} 套方案",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TacticalOrange
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "腾讯官方认证 · 真实满改 3D 外观 · 游戏内一键识别导入",
                        fontSize = 12.sp,
                        color = TextSecondaryGray
                    )
                }

                IconButton(
                    onClick = {
                        if (isRefreshing) return@IconButton
                        isRefreshing = true
                        coroutineScope.launch {
                            val updated = repository.fetchOfficialBuilds(force = true)
                            if (updated.isNotEmpty()) {
                                allBuilds = updated
                            }
                            isRefreshing = false
                            onShowToast("已从腾讯社区同步最新官方方案库")
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CardWhite)
                        .border(1.dp, CardBorderSubtle, CircleShape)
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
                            contentDescription = "刷新方案",
                            tint = TextPrimaryDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 2. 搜索框
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    selectedHotTag = null
                },
                placeholder = {
                    Text(
                        text = "搜索枪械、方案名、选手昵称 (如 M4, M7, eStar)",
                        fontSize = 13.sp,
                        color = TextSecondaryGray
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索",
                        tint = TextSecondaryGray,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            selectedHotTag = null
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "清空",
                                tint = TextSecondaryGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TacticalOrange,
                    unfocusedBorderColor = CardBorderSubtle,
                    focusedContainerColor = CardWhite,
                    unfocusedContainerColor = CardWhite
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )
        }

        // 3. 热搜快捷标签 (Hot Tags)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                hotTags.forEach { tag ->
                    val isTagActive = if (tag == "全部") {
                        searchQuery.isEmpty() && selectedHotTag == null
                    } else {
                        selectedHotTag == tag || searchQuery == tag
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isTagActive) TacticalOrange.copy(alpha = 0.12f) else CardWhite)
                            .border(
                                1.dp,
                                if (isTagActive) TacticalOrange else CardBorderSubtle,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                if (tag == "全部") {
                                    searchQuery = ""
                                    selectedHotTag = null
                                } else {
                                    searchQuery = tag
                                    selectedHotTag = tag
                                    selectedCategory = "全部"
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 12.sp,
                            fontWeight = if (isTagActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (isTagActive) TacticalOrange else TextSecondaryGray
                        )
                    }
                }
            }
        }

        // 4. 武器大类选项卡 (带方案数量徽标)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = cat == selectedCategory
                    val count = categoryCounts[cat] ?: 0
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) TacticalDark else CardWhite)
                            .border(
                                1.dp,
                                if (isSelected) TacticalDark else CardBorderSubtle,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                selectedCategory = cat
                                if (selectedHotTag != null) {
                                    selectedHotTag = null
                                    searchQuery = ""
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextPrimaryDark
                            )
                            if (count > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$count",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) TacticalOrange else TextSecondaryGray
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. 排序策略栏 (热门推荐 / 经济造价 / 顶级满改)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "方案列表 (${filteredBuilds.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondaryGray,
                    modifier = Modifier.padding(end = 6.dp)
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GunsmithSortType.values().forEach { sort ->
                        val isSortSelected = sort == selectedSort
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSortSelected) Color(0xFFE2E8F0) else Color.Transparent)
                                .clickable { selectedSort = sort }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = sort.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSortSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSortSelected) TextPrimaryDark else TextSecondaryGray
                            )
                        }
                    }
                }
            }
        }

        // 6. 方案列表或空态
        if (filteredBuilds.isEmpty()) {
            item {
                Surface(
                    color = CardWhite,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .border(1.dp, CardBorderSubtle, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "未找到相关的改枪方案",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "可尝试切换枪械类别或点击上方快捷热搜标签",
                            fontSize = 12.sp,
                            color = TextSecondaryGray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                searchQuery = ""
                                selectedCategory = "全部"
                                selectedHotTag = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TacticalDark),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("重置所有筛选", fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            items(filteredBuilds, key = { it.id }) { build ->
                GunsmithBuildDetailCard(
                    build = build,
                    onCopyCode = {
                        clipboardManager.setText(AnnotatedString(build.buildCode))
                        onShowToast("已复制 [${build.gunName}] 改枪码，启动游戏即可自动导入")
                    }
                )
            }
        }
    }
}

/**
 * 完整高质感改枪方案详情卡片
 */
@Composable
fun GunsmithBuildDetailCard(
    build: GunsmithBuild,
    onCopyCode: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isCopied by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderLight, RoundedCornerShape(20.dp)),
        color = CardWhite,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // 1. 枪械官方高清透底大图展示区 (真实满改 3D 外观图)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(142.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF8FAFC)),
                contentAlignment = Alignment.Center
            ) {
                AsyncItemImage(
                    url = build.imageUrl.ifBlank { build.gunBasePic },
                    contentDescription = build.gunName,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentScale = ContentScale.Fit,
                    fallback = {
                        Text(
                            text = build.gunName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                    }
                )

                // 左上角分类口径徽标
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.94f))
                        .border(0.6.dp, CardBorderSubtle, RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${build.category} • ${build.caliber}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondaryGray
                    )
                }

                // 右上角预估造价标签
                if (build.price > 0) {
                    val wan = build.price / 10000
                    val qian = (build.price % 10000) / 1000
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(TacticalDark.copy(alpha = 0.90f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "造价: $wan.${qian}万",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }
                } else {
                    val topBadge = build.pros.firstOrNull() ?: "战术满改"
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(TacticalDark.copy(alpha = 0.90f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = topBadge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }
                }

                // 左下角创作者徽章
                if (build.author.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.94f))
                            .border(0.6.dp, CardBorderSubtle, RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "👑 ${build.author}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. 枪械方案名称与定位
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = build.gunName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = build.roleName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TacticalOrange,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 核心参数规格
                val validSpecs = if (build.specs.isNotBlank()) {
                    if (build.specs.contains("•")) {
                        build.specs.substringAfter("•").trim()
                    } else if (build.specs.contains("造价")) {
                        build.pros.firstOrNull() ?: "实战满改"
                    } else {
                        build.specs
                    }
                } else {
                    build.pros.firstOrNull() ?: "实战满改"
                }
                if (validSpecs.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(BadgeGrayBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = validSpecs,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondaryGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. 方案打法描述
            if (build.description.isNotBlank()) {
                Text(
                    text = build.description,
                    fontSize = 12.sp,
                    color = TextSecondaryGray,
                    lineHeight = 17.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 4. 优势标签
            if (build.pros.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    build.pros.forEach { pro ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AccentGreenSoft)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = pro,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AccentGreen
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 5. 核心推荐配件胶囊
            if (build.keyAccessories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(0.6.dp, CardBorderSubtle, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "核心改件:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = build.keyAccessories.joinToString(" • "),
                        fontSize = 11.sp,
                        color = TextSecondaryGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 6. 改枪码与一键复制栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "改枪码:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondaryGray
                    )
                    Text(
                        text = build.buildCode,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = {
                        isCopied = true
                        onCopyCode()
                        coroutineScope.launch {
                            delay(2000)
                            isCopied = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCopied) AccentGreen else TacticalDark
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = Color.White
                        )
                        Text(
                            text = if (isCopied) "已复制" else "一键复制",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
