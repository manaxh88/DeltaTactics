package com.delta.tactics.presentation.gunsmith

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delta.tactics.data.repository.GunsmithRepository
import com.delta.tactics.domain.model.GunsmithBuild
import com.delta.tactics.presentation.common.AsyncItemImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 战术配色
private val CardWhite = Color(0xFFFFFFFF)
private val TextPrimaryDark = Color(0xFF0F172A)
private val TextSecondaryGray = Color(0xFF64748B)
private val TacticalDark = Color(0xFF1E293B)
private val AccentGreen = Color(0xFF10B981)
private val AccentGreenSoft = Color(0xFFECFDF5)
private val TacticalOrange = Color(0xFFFF5500)
private val GunCardBg = Color(0xFFF8FAFC)
private val GoldenCrown = Color(0xFFFFB800)

/**
 * 热门改枪配装 (抄作业) 抽屉
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotGunsmithBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onCopyCode: (gun: String, code: String) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { GunsmithRepository(context) }
    var allBuilds by remember { mutableStateOf(repository.getBuilds()) }
    var selectedCategory by remember { mutableStateOf("全部") }
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val categories = remember {
        listOf("全部", "突击步枪", "冲锋枪", "狙击步枪", "射手步枪", "轻机枪/霰弹", "手枪/特种")
    }

    // 后台静默刷新最新官方热门方案
    LaunchedEffect(Unit) {
        val updated = repository.fetchOfficialBuilds(force = false)
        if (updated.isNotEmpty()) {
            allBuilds = updated
        }
    }

    // 过滤方案列表
    val filteredBuilds = remember(allBuilds, selectedCategory, searchQuery) {
        val q = searchQuery.trim().lowercase()
        allBuilds.filter { build ->
            val matchCategory = if (selectedCategory == "全部") true else build.category == selectedCategory
            val matchSearch = if (q.isBlank()) true else {
                build.gunName.lowercase().contains(q) ||
                build.roleName.lowercase().contains(q) ||
                build.author.lowercase().contains(q) ||
                build.keyAccessories.any { it.lowercase().contains(q) }
            }
            matchCategory && matchSearch
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(bottom = 16.dp)
        ) {
            // 1. 顶栏标题与关闭按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = null,
                        tint = TacticalOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "改枪配装库 • 真实满改图",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(TacticalOrange.copy(alpha = 0.12f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "${filteredBuilds.size}套方案",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TacticalOrange
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "直连腾讯官方社区，提供真实满改3D外观与合法改枪码",
                            fontSize = 11.sp,
                            color = TextSecondaryGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = TextSecondaryGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. 搜索框
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "搜索枪械名称、方案名、选手昵称 (如 M4, M7, 勇士)",
                        fontSize = 12.sp,
                        color = TextSecondaryGray
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索",
                        tint = TextSecondaryGray,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "清空",
                                tint = TextSecondaryGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TacticalOrange,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedContainerColor = Color(0xFFF8FAFC),
                    unfocusedContainerColor = Color(0xFFF8FAFC)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(48.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. 武器分类过滤胶囊
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = cat == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) TacticalDark else Color(0xFFF1F5F9))
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextPrimaryDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. 方案列表
            if (filteredBuilds.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "未找到匹配的改枪方案",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondaryGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "可尝试输入枪械英文/中文简称或切换分类",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredBuilds, key = { it.id }) { build ->
                        GunsmithBuildDetailCard(
                            build = build,
                            onCopyCode = { onCopyCode(build.gunName, build.buildCode) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 单把枪械详细配装卡片（含满改大图、作者、造价、配件树、改枪码复制）
 */
@Composable
private fun GunsmithBuildDetailCard(
    build: GunsmithBuild,
    onCopyCode: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp)),
        color = CardWhite,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // 1. 枪械官方高清透底大图展示区（展示真实满改外观）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(GunCardBg),
                contentAlignment = Alignment.Center
            ) {
                AsyncItemImage(
                    url = build.imageUrl.ifBlank { build.gunBasePic },
                    contentDescription = build.gunName,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
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
                        .background(Color.White.copy(alpha = 0.92f))
                        .border(0.6.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
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
                            .background(TacticalDark.copy(alpha = 0.88f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "造价: $wan.${qian}万",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }
                } else {
                    val topBadge = build.pros.firstOrNull() ?: "战术方案"
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(TacticalDark.copy(alpha = 0.88f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
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
                            .background(Color.White.copy(alpha = 0.92f))
                            .border(0.6.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = build.roleName,
                        fontSize = 12.sp,
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
                        build.pros.firstOrNull() ?: "实战方案"
                    } else {
                        build.specs
                    }
                } else {
                    build.pros.firstOrNull() ?: "实战方案"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
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

            Spacer(modifier = Modifier.height(8.dp))

            // 3. 方案打法描述
            Text(
                text = build.description,
                fontSize = 12.sp,
                color = TextSecondaryGray,
                lineHeight = 17.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4. 优势标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                build.pros.forEach { pro ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentGreenSoft)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
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

            // 5. 核心推荐配件胶囊
            if (build.keyAccessories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF8FAFC))
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
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
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
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(build.buildCode))
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
