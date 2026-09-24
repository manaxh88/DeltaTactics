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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import com.delta.tactics.presentation.tasks.SeasonTasksScreen
import com.delta.tactics.presentation.loadout.CardLoadoutScreen
import com.delta.tactics.presentation.navigation.LiquidGlassBottomBar
import com.delta.tactics.presentation.navigation.LiquidNavItem
import com.delta.tactics.presentation.navigation.rememberGlassBackdrop
import com.delta.tactics.presentation.navigation.glassSource
import com.delta.tactics.presentation.profile.ProfileScreen
import com.delta.tactics.presentation.cipher.DecryptCenterScreen
import com.delta.tactics.presentation.cipher.DecryptCenterBottomSheet
import com.delta.tactics.presentation.gunsmith.GunsmithScreen
import com.delta.tactics.domain.model.TacticalNewsItem
import com.delta.tactics.presentation.news.TacticalNewsViewModel
import com.delta.tactics.presentation.news.TacticalNewsDetailBottomSheet
import com.delta.tactics.presentation.news.TacticalNewsListBottomSheet
import com.delta.tactics.core.ui.theme.*
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Newspaper
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.delta.tactics.R
import com.delta.tactics.core.ui.theme.*
import com.delta.tactics.domain.model.DailyMapPassword
import com.delta.tactics.domain.model.GunsmithBuildRepository
import com.delta.tactics.presentation.cipher.CipherRoomViewModel
import com.delta.tactics.presentation.common.AsyncItemImage
import com.delta.tactics.presentation.gunsmith.HotGunsmithBottomSheet
import com.delta.tactics.data.repository.GunsmithRepository
import com.delta.tactics.domain.model.KeyRoomCardItem
import com.delta.tactics.data.repository.KeyRoomRepository
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LocalTextStyle
import android.widget.Toast
import android.content.Context
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
    var craftRecipes by remember { mutableStateOf(profitRepository.getCraftRecipes()) }
    var bulletPacks by remember { mutableStateOf(profitRepository.getBulletPacks()) }
    var craftUpdateTime by remember { mutableStateOf(profitRepository.getCraftUpdateTime()) }
    var bulletUpdateTime by remember { mutableStateOf(profitRepository.getBulletUpdateTime()) }
    var isCraftRefreshing by remember { mutableStateOf(false) }
    var isBulletRefreshing by remember { mutableStateOf(false) }

    fun refreshCraftData(force: Boolean = true) {
        if (isCraftRefreshing) return
        isCraftRefreshing = true
        coroutineScope.launch {
            try {
                val updated = profitRepository.fetchCraftRecipes(force)
                craftRecipes = updated
                craftUpdateTime = profitRepository.getCraftUpdateTime()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isCraftRefreshing = false
            }
        }
    }

    fun refreshBulletData(force: Boolean = true) {
        if (isBulletRefreshing) return
        isBulletRefreshing = true
        coroutineScope.launch {
            try {
                val updated = profitRepository.fetchBulletPacks(force)
                bulletPacks = updated
                bulletUpdateTime = profitRepository.getBulletUpdateTime()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isBulletRefreshing = false
            }
        }
    }
    val appUpdateRepository = remember { AppUpdateRepository(context) }

    var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showWidgetGuideDialog by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var downloadState by remember { mutableStateOf<UpdateDownloadState>(UpdateDownloadState.Idle) }
    var downloadedApkFile by remember { mutableStateOf<java.io.File?>(null) }

    // 冷启动 2.5 秒后在后台低优先级静默检查新版本
    LaunchedEffect(Unit) {
        delay(2500)
        val result = appUpdateRepository.checkUpdate()
        if (result.isSuccess) {
            val info = result.getOrNull()
            if (info != null && info.hasUpdate) {
                updateInfo = info
                showUpdateDialog = true
            }
        }
    }

    val glassBackdrop = rememberGlassBackdrop()
    val listState = rememberLazyListState()
    var currentNavTab by remember { mutableIntStateOf(0) }
    var showCraftProfitSheet by remember { mutableStateOf(false) }
    var showBulletProfitSheet by remember { mutableStateOf(false) }
    var showWeaponCompareSheet by remember { mutableStateOf(false) }
    var showKeyRoomsSheet by remember { mutableStateOf(false) }
    var showHotGunsmithSheet by remember { mutableStateOf(false) }
    var showDecryptCenterSheet by remember { mutableStateOf(false) }
    val craftSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val bulletSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val weaponSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val keyRoomsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val hotGunsmithSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val decryptCenterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val newsViewModel: TacticalNewsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val newsList by newsViewModel.newsList.collectAsState()
    val isNewsLoading by newsViewModel.isLoading.collectAsState()
    val hasMoreNews by newsViewModel.hasMore.collectAsState()
    val selectedNewsDetail by newsViewModel.selectedDetail.collectAsState()
    val isNewsDetailLoading by newsViewModel.isLoadingDetail.collectAsState()
    val newsDetailError by newsViewModel.detailError.collectAsState()

    var showNewsListSheet by remember { mutableStateOf(false) }
    var showNewsDetailSheet by remember { mutableStateOf(false) }
    var activeBriefItem by remember { mutableStateOf<TacticalNewsItem?>(null) }
    val newsListSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val newsDetailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val navItems = remember {
        listOf(
            LiquidNavItem("首页", Icons.Outlined.Home, Icons.Filled.Home),
            LiquidNavItem("改枪", Icons.Outlined.Tune, Icons.Filled.Tune),
            LiquidNavItem("卡战备", Icons.Outlined.MonetizationOn, Icons.Filled.MonetizationOn),
            LiquidNavItem("任务", Icons.Outlined.Assignment, Icons.Filled.Assignment),
            LiquidNavItem("我的", Icons.Outlined.Person, Icons.Filled.Person)
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

        Box(Modifier.fillMaxSize().glassSource(glassBackdrop)) {
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
                    title = "每日密码",
                    actionText = "添加桌面组件",
                    onMoreClick = {
                        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && appWidgetManager.isRequestPinAppWidgetSupported) {
                            val myProvider = android.content.ComponentName(context, com.delta.tactics.presentation.widget.DailyPasswordWidgetProvider::class.java)
                            try {
                                appWidgetManager.requestPinAppWidget(myProvider, null, null)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        showWidgetGuideDialog = true
                    }
                )
            }
            item {
                DailyMapPasswordsGrid(
                    passwords = dailyPasswords
                )
            }

            // 3. 4 格快捷功能金刚区 (战术解密 / 制造利润 / 子弹收益 / 钥匙房)
            item {
                Spacer(modifier = Modifier.height(10.dp))
                QuickNavGridSection(
                    onItemClick = { title ->
                        when (title) {
                            "战术解密" -> showDecryptCenterSheet = true
                            "制造利润" -> {
                                showCraftProfitSheet = true
                                refreshCraftData(force = false)
                            }
                            "子弹收益" -> {
                                showBulletProfitSheet = true
                                refreshBulletData(force = false)
                            }
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
                    actionText = "全部",
                    onMoreClick = {
                        currentNavTab = 1
                    }
                )
            }
            item {
                HotGunsmithRow(
                    onCardClick = { gun, code ->
                        clipboardManager.setText(AnnotatedString(code))
                        coroutineScope.launch { snackbarHostState.showSnackbar("已复制 [$gun] 改枪码，打开《三角洲行动》即可自动识别导入") }
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
                        showNewsListSheet = true
                    }
                )
            }
            item {
                TacticalNewsContainer {
                    val displayItems = newsList.take(3)
                    displayItems.forEachIndexed { index, item ->
                        TacticalNewsRow(
                            item = item,
                            showDivider = index < displayItems.size - 1,
                            onClick = {
                                activeBriefItem = item
                                newsViewModel.selectArticle(item.threadId)
                                showNewsDetailSheet = true
                            }
                        )
                    }
                }
            }
        }
    }
            1 -> {
                GunsmithScreen(
                    onBack = { currentNavTab = 0 },
                    onShowToast = { msg ->
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
            2 -> {
                CardLoadoutScreen(
                    onBack = { currentNavTab = 0 }
                )
            }
            3 -> {
                SeasonTasksScreen(
                    onBack = { currentNavTab = 0 }
                )
            }
            4 -> {
                ProfileScreen(
                    isCheckingUpdate = isCheckingUpdate,
                    onCheckUpdate = {
                        if (isCheckingUpdate) return@ProfileScreen
                        isCheckingUpdate = true
                        coroutineScope.launch {
                            val res = appUpdateRepository.checkUpdate()
                            isCheckingUpdate = false
                            if (res.isSuccess) {
                                val info = res.getOrNull()
                                if (info != null && info.hasUpdate) {
                                    updateInfo = info
                                    showUpdateDialog = true
                                } else {
                                    val currentVer = appUpdateRepository.getInstalledVersionName()
                                    android.widget.Toast.makeText(context, "当前已是最新版本 (v$currentVer)", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                android.widget.Toast.makeText(context, "检查更新失败，请检查网络连接", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }

        }
        // A light veil keeps the gesture area readable while preserving the
        // background colors beneath the translucent glass dock.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(100.dp + navBarsBottomPadding)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            AppBackgroundLight.copy(alpha = 0.28f),
                            AppBackgroundLight.copy(alpha = 0.62f)
                        )
                    )
                )
        )

        // 常驻苹果风液态玻璃底栏 (Apple Liquid Glass Dock)
        LiquidGlassBottomBar(
            backdrop = glassBackdrop,
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
                updateTimeText = craftUpdateTime,
                isRefreshing = isCraftRefreshing,
                onRefresh = { refreshCraftData(force = true) },
                onDismissRequest = { showCraftProfitSheet = false }
            )
        }

        // 高级子弹自选包收益抽屉
        if (showBulletProfitSheet) {
            BulletProfitBottomSheet(
                sheetState = bulletSheetState,
                packs = bulletPacks,
                updateTimeText = bulletUpdateTime,
                isRefreshing = isBulletRefreshing,
                onRefresh = { refreshBulletData(force = true) },
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

        // 热门改枪配装·战术抄作业抽屉
        if (showHotGunsmithSheet) {
            HotGunsmithBottomSheet(
                sheetState = hotGunsmithSheetState,
                onDismissRequest = { showHotGunsmithSheet = false },
                onCopyCode = { gun, code ->
                    clipboardManager.setText(AnnotatedString(code))
                    android.widget.Toast.makeText(context, "已复制 [$gun] 改枪码，打开《三角洲行动》即可自动识别导入", android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 高价值钥匙房速查抽屉 (金刚区快捷功能)
        if (showKeyRoomsSheet) {
            KeyRoomsBottomSheet(
                sheetState = keyRoomsSheetState,
                onDismissRequest = { showKeyRoomsSheet = false }
            )
        }

        // 战术解密中心抽屉 (金刚区快捷功能)
        if (showDecryptCenterSheet) {
            DecryptCenterBottomSheet(
                sheetState = decryptCenterSheetState,
                viewModel = viewModel,
                onDismissRequest = { showDecryptCenterSheet = false },
                onShowToast = { msg ->
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 战术资讯完整列表抽屉
        if (showNewsListSheet) {
            TacticalNewsListBottomSheet(
                sheetState = newsListSheetState,
                newsList = newsList,
                isLoading = isNewsLoading,
                hasMore = hasMoreNews,
                onDismissRequest = { showNewsListSheet = false },
                onSelectArticle = { threadId ->
                    activeBriefItem = newsList.find { it.threadId == threadId }
                    newsViewModel.selectArticle(threadId)
                    showNewsDetailSheet = true
                },
                onLoadMore = { newsViewModel.loadMore() }
            )
        }

        // 战术资讯详情抽屉
        if (showNewsDetailSheet) {
            TacticalNewsDetailBottomSheet(
                sheetState = newsDetailSheetState,
                detail = selectedNewsDetail,
                briefItem = activeBriefItem,
                isLoading = isNewsDetailLoading,
                errorMessage = newsDetailError,
                onDismissRequest = {
                    showNewsDetailSheet = false
                    newsViewModel.clearSelectedArticle()
                },
                onRetry = {
                    activeBriefItem?.let { newsViewModel.selectArticle(it.threadId) }
                }
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

        // 桌面每日密码小组件添加引导弹窗
        if (showWidgetGuideDialog) {
            com.delta.tactics.presentation.widget.WidgetGuideDialog(
                onDismiss = { showWidgetGuideDialog = false }
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
            title = "战术解密",
            icon = Icons.Default.Lock,
            modifier = Modifier.weight(1f),
            onClick = { onItemClick("战术解密") }
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
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onMoreClick() }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
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

/** 4. 热门配装板块 (横向滚动武器卡片，高清透底大图 + 一键复制改枪码) */
@Composable
private fun HotGunsmithRow(
    onCardClick: (gun: String, code: String) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { GunsmithRepository(context) }
    var builds by remember { mutableStateOf(repository.getBuilds().take(20)) }

    LaunchedEffect(Unit) {
        val updated = repository.fetchOfficialBuilds(force = false)
        if (updated.isNotEmpty()) {
            builds = updated.take(20)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        builds.forEach { build ->
            GunBuildCard(
                gunName = build.gunName,
                roleName = build.roleName,
                author = build.author,
                specs = build.specs,
                gunTag = build.gunName.split(" ").firstOrNull() ?: "",
                buildCode = build.buildCode,
                imageUrl = build.imageUrl.ifBlank { build.gunBasePic },
                modifier = Modifier.width(184.dp),
                onClick = onCardClick
            )
        }
    }
}

@Composable
private fun GunBuildCard(
    gunName: String,
    roleName: String = "",
    author: String = "",
    specs: String,
    gunTag: String,
    buildCode: String,
    imageUrl: String = "",
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
            // 枪械官方透底高清渲染图卡片（展示真实满改外观）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF8FAFC)),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl.isNotBlank()) {
                    AsyncItemImage(
                        url = imageUrl,
                        contentDescription = gunName,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        contentScale = ContentScale.Fit,
                        fallback = {
                            Text(
                                text = gunTag,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFAAAAAA)
                            )
                        }
                    )
                } else {
                    Text(
                        text = gunTag,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFAAAAAA)
                    )
                }

                // 左上角枪械Tag徽标
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                        .border(0.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(5.dp))
                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        text = gunTag,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondaryGray
                    )
                }

                // 右下角创作者徽章
                if (author.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(5.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1E293B).copy(alpha = 0.85f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "👑 $author",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFFD700),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = gunName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (roleName.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = roleName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFF5500),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            val displaySpecs = specs.ifBlank { "实战调校" }
            Text(
                text = displaySpecs,
                fontSize = 10.5.sp,
                color = TextSecondaryGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))

            // 改枪码胶囊条 (带复制图标)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildCode,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TacticalDark,
                    letterSpacing = 0.3.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "复制改枪码",
                    tint = TextSecondaryGray,
                    modifier = Modifier.size(12.dp)
                )
            }
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
    item: TacticalNewsItem,
    showDivider: Boolean,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧缩略图
            if (item.coverUrl.isNotBlank()) {
                AsyncItemImage(
                    url = item.coverUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .size(width = 54.dp, height = 38.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(width = 54.dp, height = 38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFE5E7EB)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Newspaper,
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (item.createdAt.isNotBlank()) {
                        Text(
                            text = TacticalNewsViewModel.formatDate(item.createdAt),
                            fontSize = 11.sp,
                            color = TextSecondaryGray
                        )
                    }
                    Text(
                        text = "${TacticalNewsViewModel.formatCount(item.viewCount)} 浏览",
                        fontSize = 11.sp,
                        color = TextSecondaryGray
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
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

                Box(
                    modifier = Modifier
                        .size(width = 58.dp, height = 30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(0.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncItemImage(
                        url = GunsmithBuildRepository.getWeaponImageUrl("勇士"),
                        contentDescription = "勇士冲锋枪",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
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
            val weaponImgUrl = GunsmithBuildRepository.getWeaponImageUrl(name)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (weaponImgUrl.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .size(width = 60.dp, height = 34.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncItemImage(
                                url = weaponImgUrl,
                                contentDescription = name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    Column {
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
                }
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

/** 全地图高价值钥匙房速查抽屉 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeyRoomsBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { KeyRoomRepository(context) }
    var selectedMapId by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var allKeys by remember { mutableStateOf(repository.getAllKeys()) }
    val coroutineScope = rememberCoroutineScope()

    // 初始启动尝试静默更新钥匙行情
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val res = repository.syncKeyRoomsFromWeb(force = false)
            res.onSuccess {
                if (it.isNotEmpty()) {
                    allKeys = it
                }
            }
        }
    }

    val filteredKeys = remember(allKeys, selectedMapId, searchQuery) {
        allKeys.filter { item ->
            val matchMap = (selectedMapId == 0 || item.mapId == selectedMapId)
            val matchQuery = if (searchQuery.isBlank()) true else {
                item.name.contains(searchQuery, ignoreCase = true) ||
                        item.mapName.contains(searchQuery, ignoreCase = true) ||
                        item.lootDesc.contains(searchQuery, ignoreCase = true)
            }
            matchMap && matchQuery
        }
    }

    val mapOptions = remember(allKeys) {
        listOf(
            0 to "全部 (${allKeys.size})",
            1 to "零号大坝 (${allKeys.count { it.mapId == 1 }})",
            2 to "航天基地 (${allKeys.count { it.mapId == 2 }})",
            3 to "长弓溪谷 (${allKeys.count { it.mapId == 3 }})",
            4 to "巴克什 (${allKeys.count { it.mapId == 4 }})",
            5 to "潮汐监狱 (${allKeys.count { it.mapId == 5 }})",
            6 to "AZ3核电 (${allKeys.count { it.mapId == 6 }})"
        )
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
                .padding(bottom = 28.dp)
        ) {
            // 顶栏：标题与关闭按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "全地图钥匙房速查",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFEF3C7))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "80把真机钥匙",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "六大战术地图 · 实时交易行估值与核心出金预估",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryGray
                    )
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

            Spacer(modifier = Modifier.height(14.dp))

            // 地图分类滑动栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                mapOptions.forEach { (mid, label) ->
                    val isSelected = (selectedMapId == mid)
                    Surface(
                        onClick = { selectedMapId = mid },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) TacticalDark else Color(0xFFF1F5F9),
                        border = if (isSelected) null else BorderStroke(0.5.dp, Color(0xFFE2E8F0))
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextSecondaryGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 搜索过滤框
            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextSecondaryGray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 13.sp,
                            color = TextPrimaryDark
                        ),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "搜索钥匙名称或掉落（如: 总裁、总控、蓝图）",
                                    fontSize = 12.sp,
                                    color = TextTertiaryLight
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "清除",
                                tint = TextSecondaryGray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 钥匙卡列表
            if (filteredKeys.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "未找到相关钥匙，请尝试其他关键词",
                        fontSize = 13.sp,
                        color = TextSecondaryGray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredKeys, key = { it.id + it.name }) { keyItem ->
                        RealKeyRoomCard(
                            keyItem = keyItem,
                            onCopy = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("KeyName", keyItem.name)
                                cm.setPrimaryClip(clip)
                                Toast.makeText(context, "已复制钥匙名称: ${keyItem.name}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RealKeyRoomCard(
    keyItem: KeyRoomCardItem,
    onCopy: () -> Unit
) {
    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.8.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCopy() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 真实钥匙高清图
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .border(0.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (keyItem.imgUrl.isNotBlank()) {
                    AsyncItemImage(
                        url = keyItem.imgUrl,
                        contentDescription = keyItem.name,
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = TacticalOrange,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        // 地图小标
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E293B))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = keyItem.mapName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TacticalOrange
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        // 等级标签
                        val gradeColor = when (keyItem.grade) {
                            6 -> Color(0xFFE65100) // 金
                            5 -> Color(0xFF7C3AED) // 紫
                            4 -> Color(0xFF0284C7) // 蓝
                            else -> Color(0xFF10B981) // 绿
                        }
                        val gradeBg = when (keyItem.grade) {
                            6 -> Color(0xFFFFFBEB)
                            5 -> Color(0xFFF5F3FF)
                            4 -> Color(0xFFF0F9FF)
                            else -> Color(0xFFECFDF5)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(gradeBg)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${keyItem.grade}星",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = gradeColor
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = keyItem.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                    // 估值价格
                    Text(
                        text = keyItem.formattedPrice,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = keyItem.lootDesc,
                        fontSize = 11.sp,
                        color = Color(0xFF4B5563),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (keyItem.changeToday.isNotBlank()) {
                        val isUp = keyItem.changeToday.startsWith("+")
                        val isDown = keyItem.changeToday.startsWith("-")
                        val chgColor = when {
                            isUp -> Color(0xFFDC2626)
                            isDown -> Color(0xFF16A34A)
                            else -> TextSecondaryGray
                        }
                        Text(
                            text = "日涨跌 ${keyItem.changeToday}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = chgColor
                        )
                    }
                }
            }
        }
    }
}
