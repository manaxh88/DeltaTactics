package com.delta.tactics.presentation.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * 导航项数据模型
 */
data class LiquidNavItem(
    val title: String,
    val icon: ImageVector,
    val activeIcon: ImageVector? = null,
    val route: String = "",
    val badge: Int? = null
) {
    constructor(title: String, icon: ImageVector, activeIcon: ImageVector? = null) : this(
        title = title,
        icon = icon,
        activeIcon = activeIcon,
        route = "",
        badge = null
    )
}

typealias NavItem = LiquidNavItem

/**
 * 纯正苹果风格液态玻璃常驻底栏 (Apple iOS 18 / VisionOS Liquid Glass Dock)
 *
 * 核心苹果拟态特性：
 * 1. 悬浮胶囊底座 (Floating Capsule Dock)：高度 64dp，圆角 32dp 全胶囊药丸造型，悬浮于手势条之上。
 * 2. 苹果超透乳白水晶玻璃质感 (Ultra-Thin Translucent Glass)：多阶高透白纯净渐变，通透光泽，不脏不灰。
 * 3. 顶部 1px 极细镜面反射光弧 (Specular Highlight Line)：微曲镜面反光，真实模拟环境光投射。
 * 4. 苹果液态药丸滑块 (Apple Liquid Pill Indicator)：58dp 宽、48dp 高的柔润药丸滑块，自带微凸阴影与表面张力光泽。
 * 5. 液体张力弹性拉伸物理动效 (Fluid Surface Tension Physics)：Tab 切换时，滑块沿水平轴轻微拉伸 (scaleX=1.06)，落位时有机回弹 (Spring 0.76)。
 * 6. 严密 100% 数学绝对对称：各 Tab 基于父容器 1/N 均匀等分，滑块精准居中锚定，零偏移零误差。
 * 7. 图标与文字并存 (Apple SF Style Typography)：高对比度深石墨黑选中态 + 苹果次级灰未选中态。
 */
@Composable
fun LiquidGlassBottomBar(
    items: List<LiquidNavItem>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    selectedColor: Color = Color(0xFF0F172A),
    unselectedColor: Color = Color(0xFF8E8E93),
    barHeight: Dp = 64.dp,
    cornerRadius: Dp = 32.dp
) {
    val density = LocalDensity.current
    val itemCount = items.size.coerceAtLeast(1)

    // 记录内部容器宽度，用于精准计算数学绝对对称坐标
    var containerWidthPx by remember { mutableFloatStateOf(0f) }

    // 滑块宽度与高度规格
    val pillWidthDp = 58.dp
    val pillHeightDp = 48.dp
    val pillWidthPx = with(density) { pillWidthDp.toPx() }

    // 当前选中项中心 X 像素坐标：(selectedTab + 0.5) * (containerWidth / itemCount)
    val itemWidthPx = if (containerWidthPx > 0f) containerWidthPx / itemCount else 0f
    val targetCenterX = if (itemWidthPx > 0f) {
        (selectedTab + 0.5f) * itemWidthPx
    } else {
        0f
    }

    // 苹果标志性阻尼弹性滑移动画 (Spring Physics)
    val animatedCenterX by animateFloatAsState(
        targetValue = targetCenterX,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "apple_liquid_pill_x"
    )

    // 移动过程中的液体表面张力拉伸动效 (Jelly Stretch)
    val isMoving = containerWidthPx > 0f && abs(animatedCenterX - targetCenterX) > with(density) { 3.dp.toPx() }
    val stretchScaleX by animateFloatAsState(
        targetValue = if (isMoving) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "liquid_stretch_x"
    )
    val stretchScaleY by animateFloatAsState(
        targetValue = if (isMoving) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "liquid_stretch_y"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 8.dp)
            .height(barHeight)
    ) {
        // 1. 苹果悬浮胶囊底座容器 (Floating Dock Capsule)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 22.dp,
                    shape = RoundedCornerShape(cornerRadius),
                    spotColor = Color(0x350F172A),
                    ambientColor = Color(0x180F172A)
                )
                .clip(RoundedCornerShape(cornerRadius))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor.copy(alpha = 0.56f),
                            Color(0xFFEAF3FF).copy(alpha = 0.34f),
                            Color(0xFFDDEBFA).copy(alpha = 0.42f)
                        )
                    )
                )
                .border(
                    width = 0.8.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.92f),
                            Color(0xFFB9D9F5).copy(alpha = 0.54f),
                            Color.White.copy(alpha = 0.72f)
                        )
                    ),
                    shape = RoundedCornerShape(cornerRadius)
                )
        ) {
            // Translucent material tint and a soft internal light bloom.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.Transparent,
                                Color(0xFF8CC8F5).copy(alpha = 0.10f)
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = 8.dp, y = (-4).dp)
                    .size(116.dp, 64.dp)
                    .blur(24.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color.White.copy(alpha = 0.72f),
                                Color(0xFFB8E3FF).copy(alpha = 0.26f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Thin specular highlight and a faint lower refraction rim.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.95f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val rim = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.22f),
                                Color.Transparent,
                                Color(0xFF84BCE8).copy(alpha = 0.20f)
                            )
                        )
                        onDrawWithContent {
                            drawContent()
                            drawRoundRect(
                                brush = rim,
                                style = Stroke(width = 1.2.dp.toPx()),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx())
                            )
                        }
                    }
            )

            // 内部工作区域 (滑块与 Tab 共享此绝对空间)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp)
                    .onGloballyPositioned { coordinates ->
                        containerWidthPx = coordinates.size.width.toFloat()
                    }
            ) {
                // 3. 苹果液态药丸滑块 (Apple Liquid Pill Indicator)
                if (containerWidthPx > 0f && animatedCenterX > 0f) {
                    val pillLeftPx = animatedCenterX - (pillWidthPx / 2f)
                    val pillLeftDp = with(density) { pillLeftPx.toDp() }

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = pillLeftDp)
                            .size(width = pillWidthDp, height = pillHeightDp)
                            .scale(scaleX = stretchScaleX, scaleY = stretchScaleY)
                            .shadow(
                                elevation = 7.dp,
                                shape = RoundedCornerShape(24.dp),
                                spotColor = Color(0x3A1D4770),
                                ambientColor = Color(0x1A1D4770)
                            )
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.78f),
                                        Color(0xFFD9EEFF).copy(alpha = 0.46f),
                                        Color(0xFFB7DBF5).copy(alpha = 0.40f)
                                    )
                                )
                            )
                            .border(
                                width = 0.8.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.95f),
                                        Color(0xFF8FC7F2).copy(alpha = 0.60f)
                                    )
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(18.dp)
                                .align(Alignment.TopCenter)
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.42f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                }

                // 4. Tab 按钮行 (1/N 严格数学等分，完美对齐滑块)
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, item ->
                        val isSelected = index == selectedTab

                        AppleTabItem(
                            item = item,
                            isSelected = isSelected,
                            selectedColor = selectedColor,
                            unselectedColor = unselectedColor,
                            onClick = {
                                if (selectedTab != index) {
                                    onTabSelected(index)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

/**
 * 苹果风格导航项组件 (图标 + 精致标签，严密居中排布)
 */
@Composable
private fun AppleTabItem(
    item: LiquidNavItem,
    isSelected: Boolean,
    selectedColor: Color,
    unselectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 弹性缩放动效
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.70f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "apple_icon_scale"
    )

    // 图标与文字颜色平滑过渡
    val activeColorAnimated by animateColorAsState(
        targetValue = if (isSelected) selectedColor else unselectedColor,
        animationSpec = tween(220),
        label = "apple_item_color"
    )

    val badgeText = remember(item.badge) {
        item.badge?.let { count ->
            if (count > 99) "99+" else count.toString()
        }
    }

    Box(
        modifier = modifier
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 图标层 (带弹性缩放与徽标支持)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.scale(iconScale)
            ) {
                Icon(
                    imageVector = if (isSelected && item.activeIcon != null) item.activeIcon else item.icon,
                    contentDescription = item.title,
                    tint = activeColorAnimated,
                    modifier = Modifier.size(22.dp)
                )

                // 苹果鲜红通知小红点 / 数字徽标
                badgeText?.let { text ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-4).dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF3B30)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = text,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.5.dp))

            // 标题文本 (苹果 SF Pro 字体排版风格)
            Text(
                text = item.title,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = activeColorAnimated,
                letterSpacing = (-0.1).sp
            )
        }
    }
}

/**
 * 兼容别名
 */
@Composable
fun LiquidGlassBottomNavBar(
    items: List<LiquidNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    selectedColor: Color = Color(0xFF0F172A),
    unselectedColor: Color = Color(0xFF8E8E93),
    barHeight: Dp = 64.dp,
    cornerRadius: Dp = 32.dp
) = LiquidGlassBottomBar(
    items = items,
    selectedTab = selectedIndex,
    onTabSelected = onItemSelected,
    modifier = modifier,
    backgroundColor = backgroundColor,
    selectedColor = selectedColor,
    unselectedColor = unselectedColor,
    barHeight = barHeight,
    cornerRadius = cornerRadius
)
