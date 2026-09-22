package com.delta.tactics.presentation.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PrecisionManufacturing
import com.delta.tactics.presentation.tasks.SeasonTasksScreen
import com.delta.tactics.presentation.loadout.CardLoadoutScreen
import com.delta.tactics.presentation.navigation.LiquidGlassBottomBar
import com.delta.tactics.presentation.navigation.LiquidNavItem
import com.delta.tactics.presentation.profile.ProfileScreen
import com.delta.tactics.presentation.cipher.DecryptCenterScreen
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import com.delta.tactics.data.repository.AppUpdateRepository
import com.delta.tactics.data.repository.ProfitRepository
import com.delta.tactics.domain.model.AppUpdateInfo
import com.delta.tactics.domain.model.UpdateDownloadState
import com.delta.tactics.presentation.profit.CraftProfitBottomSheet
import com.delta.tactics.presentation.profit.BulletProfitBottomSheet
import com.delta.tactics.presentation.update.UpdateDialog
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Sync
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delta.tactics.R
import com.delta.tactics.core.ui.theme.*
import com.delta.tactics.domain.model.DailyMapPassword
import com.delta.tactics.presentation.cipher.CipherRoomViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
    viewModel: CipherRoomViewModel,
    modifier: Modifier = Modifier
) {
    val dailyPasswords by viewModel.dailyPasswords.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val profitRepository = remember { ProfitRepository(context) }
    val craftRecipes = remember { profitRepository.getCraftRecipes() }
    val bulletPacks = remember { profitRepository.getBulletPacks() }
    val appUpdateRepository = remember { AppUpdateRepository(context) }

    var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var downloadState by remember { mutableStateOf<UpdateDownloadState>(UpdateDownloadState.Idle) }
    var downloadedApkFile by remember { mutableStateOf<java.io.File?>(null) }

    // 冷启动 2.5 秒后在后台低优先级静默检查新版本
    LaunchedEffect(Unit) {
        delay(2500)
        val result = appUpdateRepository.checkUpdate(currentVersionCode = 15)
        if (result.isSuccess) {
            val info = result.getOrNull()
            if (info != null && info.hasUpdate) {
                updateInfo = info
                showUpdateDialog = true
            }
        }
    }

    val listState = rememberLazyListState()
    var currentNavTab by remember { mutableIntStateOf(0) }
    var showCraftProfitSheet by remember { mutableStateOf(false) }
    var showBulletProfitSheet by remember { mutableStateOf(false) }
    var showWeaponCompareSheet by remember { mutableStateOf(false) }
    var showKeyRoomsSheet by remember { mutableStateOf(false) }
    val craftSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val bulletSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val weaponSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val keyRoomsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val navItems = remember {
        listOf(
            LiquidNavItem("首页", Icons.Default.Home),
            LiquidNavItem("卡战备", Icons.Default.MonetizationOn),
            LiquidNavItem("任务", Icons.Default.Assignment),
            LiquidNavItem("解密", Icons.Default.Lock),
            LiquidNavItem("我的", Icons.Default.Person)
        )
    }

    BackHandler(enabled = currentNavTab != 0) {
        currentNavTab = 0
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackgroundLight)
    ) {
        val navBarsBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        when (currentNavTab) {
            0 -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 110.dp + navBarsBottomPadding)
                ) {
                    // 1. 顶部问候栏 (猫猫头像+问候语+纯圆搜索按钮，去掉通知)
                    item {
                        TopGreetingHeader(
                            onAvatarClick = { currentNavTab = 4 },
                            onSearchClick = {
                                coroutineScope.launch { snackbarHostState.showSnackbar("开启战术全局搜索") }
                            }
                        )
                    }

            // 2. 每日密码板块 (置顶纯展示：仅地图名与密码，去掉点位与跳转)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SectionHeader(
                    title = "每日密码"
                )
            }
            item {
                DailyMapPasswordsGrid(
                    passwords = dailyPasswords
                )
            }

            // 3. 4 格快捷功能金刚区 (改枪配装 / 制造利润 / 子弹收益 / 钥匙房)
            item {
                Spacer(modifier = Modifier.height(10.dp))
                QuickNavGridSection(
                    onItemClick = { title ->
                        when (title) {
                            "改枪配装" -> {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(4)
                                    snackbarHostState.showSnackbar("已定位至热门改枪配装")
                                }
                            }
                            "制造利润" -> showCraftProfitSheet = true
                            "子弹收益" -> showBulletProfitSheet = true
                            "武器对比" -> showWeaponCompareSheet = true
                            "钥匙房" -> showKeyRoomsSheet = true
                            else -> coroutineScope.launch { snackbarHostState.showSnackbar("进入「$title」") }
                        }
                    }
                )
            }

            // 4. 热门配装板块 (一键复制改枪码)
            item {
                Spacer(modifier = Modifier.height(6.dp))
                SectionHeader(
                    title = "热门配装",
                    actionText = "武器对比",
                    onMoreClick = {
                        showWeaponCompareSheet = true
                    }
                )
            }
            item {
                HotGunsmithRow(
                    onCardClick = { gun, code ->
                        clipboardManager.setText(AnnotatedString(code))
                        coroutineScope.launch { snackbarHostState.showSnackbar("已复制 [$gun] 改枪码: $code") }
                    }
                )
            }

            // 5. 战术资讯板块 (参考图 1:1 外层白色圆角卡片，内部列表项带缩略图与箭头)
            item {
                Spacer(modifier = Modifier.height(10.dp))
                SectionHeader(
                    title = "战术资讯",
                    actionText = "全部",
                    onMoreClick = {
                        coroutineScope.launch { snackbarHostState.showSnackbar("查看全部战术资讯与最新改动") }
                    }
                )
            }
            item {
                TacticalNewsContainer {
                    TacticalNewsRow(
                        title = "零号大坝 • 撤离点架枪点位图解",
                        showDivider = true
                    )
                    TacticalNewsRow(
                        title = "新干员「蜂医」技能解析与配装思路",
                        showDivider = false
                    )
                }
            }
        }
    }
    1 -> {
                CardLoadoutScreen(
                    onBack = { currentNavTab = 0 }
                )
            }
            2 -> {
                SeasonTasksScreen(
                    onBack = { currentNavTab = 0 }
                )
            }
            3 -> {
                DecryptCenterScreen(
                    viewModel = viewModel,
                    onShowToast = { msg ->
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
            4 -> {
                ProfileScreen(
                    isCheckingUpdate = isCheckingUpdate,
                    onCheckUpdate = {
                        if (isCheckingUpdate) return@ProfileScreen
                        isCheckingUpdate = true
                        coroutineScope.launch {
                            val res = appUpdateRepository.checkUpdate(currentVersionCode = 15)
                            isCheckingUpdate = false
                            if (res.isSuccess) {
                                val info = res.getOrNull()
                                if (info != null && info.hasUpdate) {
                                    updateInfo = info
                                    showUpdateDialog = true
                                } else {
                                    android.widget.Toast.makeText(context, "当前已是最新版本 (v2.8.5)", android.widget.Toast.LENGTH_SHORT).show()
                                    snackbarHostState.showSnackbar("当前已是最新版本 (v2.8.5)")
                                }
                            } else {
                                android.widget.Toast.makeText(context, "检查更新失败，请检查网络连接", android.widget.Toast.LENGTH_SHORT).show()
                                snackbarHostState.showSnackbar("检查更新失败，请检查网络连接")
                            }
                        }
                    }
                )
            }
        }

        // 常驻液态玻璃样式底部导航栏 (全页面常驻)
        LiquidGlassBottomBar(
            items = navItems,
            selectedTab = currentNavTab,
            onTabSelected = { tab ->
                currentNavTab = tab
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // 浮动提示条，位于胶囊底栏上方
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp + navBarsBottomPadding)
        )

        // 特勤处工作台制造利润抽屉
        if (showCraftProfitSheet) {
            CraftProfitBottomSheet(
                sheetState = craftSheetState,
                recipes = craftRecipes,
                onDismissRequest = { showCraftProfitSheet = false }
            )
        }

        // 高级子弹自选包收益抽屉
        if (showBulletProfitSheet) {
            BulletProfitBottomSheet(
                sheetState = bulletSheetState,
                packs = bulletPacks,
                onDismissRequest = { showBulletProfitSheet = false }
            )
        }

        // 战术武器对比抽屉 (金刚区快捷功能)
        if (showWeaponCompareSheet) {
            WeaponCompareBottomSheet(
                sheetState = weaponSheetState,
                onDismissRequest = { showWeaponCompareSheet = false }
            )
        }

        // 高价值钥匙房速查抽屉 (金刚区快捷功能)
        if (showKeyRoomsSheet) {
            KeyRoomsBottomSheet(
                sheetState = keyRoomsSheetState,
                onDismissRequest = { showKeyRoomsSheet = false }
            )
        }

        // 在线版本更新弹窗
        if (showUpdateDialog && updateInfo != null) {
            UpdateDialog(
                updateInfo = updateInfo!!,
                downloadState = downloadState,
                onStartDownload = {
                    coroutineScope.launch {
                        downloadState = UpdateDownloadState.Downloading(0f, 0L, 1L)
                        val result = appUpdateRepository.downloadApk(updateInfo!!.apkUrl) { progress, cur, total ->
                            downloadState = UpdateDownloadState.Downloading(progress, cur, total)
                        }
                        if (result.isSuccess) {
                            val file = result.getOrNull()
                            if (file != null) {
                                downloadedApkFile = file
                                downloadState = UpdateDownloadState.Success(file)
                                appUpdateRepository.installApk(file)
                            }
                        } else {
                            val err = result.exceptionOrNull()?.message ?: "下载中断"
                            downloadState = UpdateDownloadState.Error(err)
                        }
                    }
                },
                onInstall = {
                    downloadedApkFile?.let { file ->
                        appUpdateRepository.installApk(file)
                    }
                },
                onBrowserDownload = {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo!!.browserUrl))
                    context.startActivity(browserIntent)
                },
                onDismiss = {
                    showUpdateDialog = false
                    downloadState = UpdateDownloadState.Idle
                }
            )
        }
    }
}

/** 1. 顶部问候栏 (严格正圆搜索按钮，已移除通知) */
@Composable
private fun TopGreetingHeader(
    onAvatarClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 圆形猫猫头像 (用户指定图标，点击可切换至我的)
            Image(
                painter = painterResource(id = R.drawable.cat_avatar),
                contentDescription = "用户头像",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .clickable { onAvatarClick() }
            )

            Column {
                Text(
                    text = "下午好，指挥官",
                    fontSize = 12.sp,
                    color = TextSecondaryGray,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "三角洲助手",
                    fontSize = 20.sp,
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 右侧正圆纯白搜索按钮 (移除通知，使用纯正圆形边框规避阴影多边形走形)
        CircleHeaderButton(
            icon = Icons.Default.Search,
            contentDesc = "搜索",
            onClick = onSearchClick
        )
    }
}

@Composable
private fun CircleHeaderButton(
    icon: ImageVector,
    contentDesc: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(CardWhite, CircleShape)
            .border(1.2.dp, Color(0xFFE2E8F0), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = TextPrimaryDark,
            modifier = Modifier.size(20.dp)
        )
    }
}


/** 3. 4 格快捷功能金刚区 (四大战术工具，无重复冲突) */
@Composable
private fun QuickNavGridSection(
    onItemClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickNavSquareCard(
            title = "改枪配装",
            icon = Icons.Default.Adjust,
            modifier = Modifier.weight(1f),
            onClick = { onItemClick("改枪配装") }
        )
        QuickNavSquareCard(
            title = "制造利润",
            icon = Icons.Default.PrecisionManufacturing,
            modifier = Modifier.weight(1f),
            onClick = { onItemClick("制造利润") }
        )
        QuickNavSquareCard(
            title = "子弹收益",
            icon = Icons.Default.MilitaryTech,
            modifier = Modifier.weight(1f),
            onClick = { onItemClick("子弹收益") }
        )
        QuickNavSquareCard(
            title = "钥匙房",
            icon = Icons.Default.VpnKey,
            modifier = Modifier.weight(1f),
            onClick = { onItemClick("钥匙房") }
        )
    }
}

@Composable
private fun QuickNavSquareCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(18.dp)),
        color = CardWhite,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = TextPrimaryDark,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                color = TextSecondaryGray,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** 标题栏：“热门配装” / “战术资讯”，右侧“全部 >” (严格 1:1 还原参考图) */
@Composable
private fun SectionHeader(
    title: String,
    actionText: String? = null,
    onMoreClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            color = TextPrimaryDark,
            fontWeight = FontWeight.Bold
        )
        if (actionText != null && onMoreClick != null) {
            Row(
                modifier = Modifier.clickable { onMoreClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionText,
                    fontSize = 12.sp,
                    color = TextSecondaryGray
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondaryGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/** 4. 热门配装板块 (双列枪械卡片，严格 1:1 还原参考图) */
@Composable
private fun HotGunsmithRow(
    onCardClick: (gun: String, code: String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GunBuildCard(
            gunName = "M4A1 • 稳定压制",
            specs = "后坐力 -18% • 射程 42m",
            gunTag = "M4A1",
            buildCode = "M4A1-6824-TAC",
            modifier = Modifier.weight(1f),
            onClick = onCardClick
        )
        GunBuildCard(
            gunName = "AX-50 • 远程点名",
            specs = "开镜 0.32s • 射程 96m",
            gunTag = "AX-50",
            buildCode = "AX50-9041-HOT",
            modifier = Modifier.weight(1f),
            onClick = onCardClick
        )
    }
}

@Composable
private fun GunBuildCard(
    gunName: String,
    specs: String,
    gunTag: String,
    buildCode: String,
    modifier: Modifier = Modifier,
    onClick: (gun: String, code: String) -> Unit
) {
    Surface(
        onClick = { onClick(gunName, buildCode) },
        modifier = modifier.border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(18.dp)),
        color = CardWhite,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // 灰底枪械渲染方块 (严格 1:1 还原参考图灰底卡片)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFEEEEEE)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = gunTag,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFAAAAAA)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = gunName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = specs,
                fontSize = 11.sp,
                color = TextSecondaryGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

/** 5. 每日地图密码网格 (采用与热门配装完全一致的高质感卡片排布) */
@Composable
private fun DailyMapPasswordsGrid(
    passwords: List<DailyMapPassword>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val chunked = remember(passwords) { passwords.chunked(2) }
        for (row in chunked) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (item in row) {
                    MapPasswordCard(
                        mapPassword = item,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MapPasswordCard(
    mapPassword: DailyMapPassword,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(18.dp)),
        color = CardWhite,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 地图预览小灰块，居中优雅大号展示 4 位密码
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF3F4F6)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mapPassword.code,
                    fontSize = 26.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimaryDark,
                    letterSpacing = 3.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = mapPassword.mapName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

/** 6. 战术资讯大白卡片容器 (严格 1:1 还原参考图) */
@Composable
private fun TacticalNewsContainer(
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(20.dp)),
        color = CardWhite,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            content()
        }
    }
}

@Composable
private fun TacticalNewsRow(
    title: String,
    showDivider: Boolean
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 参考图左侧圆角缩略图
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE5E7EB)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                fontSize = 14.sp,
                color = TextPrimaryDark,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFC0C4CC),
                modifier = Modifier.size(18.dp)
            )
        }

        if (showDivider) {
            HorizontalDivider(
                color = Color(0xFFF4F5F7),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }
    }
}


/** 首页 S11 赛季任务推荐大白卡 */
@Composable
private fun SeasonTaskBannerCard(
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
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
                        text = "赛季任务 · 全阶段速查",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "共116题",
                        fontSize = 12.sp,
                        color = TextSecondaryGray
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextSecondaryGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "全服4大阶段（沉舟·星火·面具·众生）• 金枪客改枪/安全箱/经验/哈夫币",
                fontSize = 12.sp,
                color = TextSecondaryGray,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("主线 沉舟", "战斗专家 金枪客", "狙击精英").forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 11.sp,
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/** 卡战备首页 Banner 卡片 */
@Composable
private fun CardLoadoutBannerCard(
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(18.dp)),
        color = CardWhite,
        shape = RoundedCornerShape(18.dp)
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
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E293B))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "省钱套利",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TacticalOrange
                        )
                    }
                    Text(
                        text = "鼠鼠卡战备 · 低成本进图",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "5大门槛",
                        fontSize = 12.sp,
                        color = TextSecondaryGray
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextSecondaryGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "11W / 18W / 55W / 60W / 78W • 枪械/均衡/胸挂三流派 • 最高单局立省 12.8W",
                fontSize = 12.sp,
                color = TextSecondaryGray,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("11W 机密", "18W 航天", "78W 潮汐监狱").forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 11.sp,
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}


/** 武器对比抽屉 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeaponCompareBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit
) {
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
            Text(
                text = "战术武器属性对比",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "主流突击步枪与冲锋枪弹道、伤害与操控横向对比",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            WeaponCompareCard(
                name = "M4A1 全面突击",
                caliber = "5.56×45mm",
                rpm = "800 RPM",
                damage = "34 肉伤",
                range = "55m 射程",
                recoil = "78 控制"
            )
            Spacer(modifier = Modifier.height(8.dp))
            WeaponCompareCard(
                name = "K416 稳定压制",
                caliber = "5.56×45mm",
                rpm = "850 RPM",
                damage = "32 肉伤",
                range = "50m 射程",
                recoil = "72 控制"
            )
            Spacer(modifier = Modifier.height(8.dp))
            WeaponCompareCard(
                name = "MP5 近战爆发",
                caliber = "9×19mm",
                rpm = "900 RPM",
                damage = "36 肉伤",
                range = "28m 射程",
                recoil = "85 控制"
            )
            Spacer(modifier = Modifier.height(8.dp))
            WeaponCompareCard(
                name = "勇士 高穿强袭",
                caliber = "9×19mm",
                rpm = "750 RPM",
                damage = "38 肉伤",
                range = "35m 射程",
                recoil = "69 控制"
            )
        }
    }
}

@Composable
private fun WeaponCompareCard(
    name: String,
    caliber: String,
    rpm: String,
    damage: String,
    range: String,
    recoil: String
) {
    Surface(
        color = IconCircleBg,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Text(
                    text = caliber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondaryGray
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(rpm, damage, range, recoil).forEach { stat ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stat,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimaryDark
                        )
                    }
                }
            }
        }
    }
}

/** 高价值钥匙房速查抽屉 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeyRoomsBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit
) {
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
            Text(
                text = "高价值钥匙房速查",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "各图核心钥匙房位置、钥匙市价与出金期望估值",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            KeyRoomCard(
                mapName = "航天基地",
                room = "核心区 • 离心机实验室",
                key = "离心机金卡",
                est = "期望 180W+",
                loot = "核心产出: 绝密航天数据、红卡、曼德尔砖"
            )
            Spacer(modifier = Modifier.height(8.dp))
            KeyRoomCard(
                mapName = "零号大坝",
                room = "行政主楼 • 2F 大办公室",
                key = "主楼钥匙",
                est = "期望 120W+",
                loot = "核心产出: 绝密蓝图、军工电脑箱、大金条"
            )
            Spacer(modifier = Modifier.height(8.dp))
            KeyRoomCard(
                mapName = "长弓溪谷",
                room = "雷达站 • 指挥中心密室",
                key = "雷达钥匙",
                est = "期望 95W+",
                loot = "核心产出: 军用热成像仪、CPU芯片、高级战术装备"
            )
            Spacer(modifier = Modifier.height(8.dp))
            KeyRoomCard(
                mapName = "巴克什",
                room = "集市暗室 • 贵重品保密室",
                key = "集市钥匙",
                est = "期望 70W+",
                loot = "核心产出: 贵重金条、保密终端箱"
            )
        }
    }
}

@Composable
private fun KeyRoomCard(
    mapName: String,
    room: String,
    key: String,
    est: String,
    loot: String
) {
    Surface(
        color = IconCircleBg,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E293B))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = mapName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TacticalOrange
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = room,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }
                Text(
                    text = est,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "所需钥匙: $key",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondaryGray
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = loot,
                fontSize = 11.sp,
                color = Color(0xFF4B5563)
            )
        }
    }
}

