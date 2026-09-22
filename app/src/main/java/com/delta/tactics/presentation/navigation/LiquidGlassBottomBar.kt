package com.delta.tactics.presentation.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delta.tactics.core.ui.theme.TacticalOrange

/**
 * 导航项数据模型
 */
data class LiquidNavItem(
    val title: String,
    val icon: ImageVector,
    val activeIcon: ImageVector? = null,
    val badge: Int? = null
)

/**
 * 液态玻璃质感常驻底部导航栏 (Liquid Glass Bottom Bar)
 *
 * 特性：
 * 1. 拟态玻璃多层半透明渐变（Glassmorphic translucent gradient layers）
 * 2. 顶部 1px 镜面反射高光线（Specular Highlight）
 * 3. 棱镜微折射渐变描边（Refractive border）
 * 4. 弹性物理水滴位移动画（Fluid spring droplet indicator）
 * 5. 选中项流体光晕（Radial Orange Glow）与弹性缩放
 */
@Composable
fun LiquidGlassBottomBar(
    items: List<LiquidNavItem>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    barHeight: Dp = 66.dp,
    activeColor: Color = Color(0xFF0F172A),
    inactiveColor: Color = Color(0xFF64748B)
) {
    val density = LocalDensity.current
    var currentSelectedIndex by remember { mutableIntStateOf(selectedTab) }

    if (currentSelectedIndex != selectedTab) {
        currentSelectedIndex = selectedTab
    }

    // 记录各 Tab 项在父容器中的中心水平坐标和宽度
    val itemCenterXs = remember { mutableStateMapOf<Int, Float>() }
    val itemWidths = remember { mutableStateMapOf<Int, Float>() }

    val targetCenterX = remember(currentSelectedIndex, itemCenterXs.toMap()) {
        itemCenterXs[currentSelectedIndex] ?: 0f
    }

    // 水滴指示器水平中心 X 坐标的弹性动画 (阻尼 0.72 带来有机流体回弹质感)
    val animatedCenterX by animateFloatAsState(
        targetValue = targetCenterX,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "liquid_blob_x"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        // 外层柔和弥散阴影与高透白玉质感容器
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(33.dp),
                    spotColor = Color(0x2B0F172A),
                    ambientColor = Color(0x0F0F172A)
                )
                .clip(RoundedCornerShape(33.dp))
                // 纯正高雅的白色质感底色 (遮蔽底层文字重叠，告别杂乱)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFF9FAFB)
                        )
                    )
                )
                // 极简微灰棱镜描边
                .border(
                    BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(33.dp)
                )
        ) {
            // 顶部 1px 镜面受光反光线 (Specular Glass Top Rim)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 24.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White,
                                Color.White,
                                Color.Transparent
                            )
                        )
                    )
            )

            // 液态流体水滴胶囊指示器 (无外描边、纯净温润浅灰胶囊，顺滑滑移)
            if (animatedCenterX > 0f || currentSelectedIndex == 0) {
                val blobWidth = 50.dp
                val blobHeight = 48.dp
                val blobWidthPx = with(density) { blobWidth.toPx() }
                val blobLeftPx = animatedCenterX - (blobWidthPx / 2f)

                if (blobLeftPx >= 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = with(density) { blobLeftPx.toDp() })
                            .width(blobWidth)
                            .height(blobHeight)
                            .clip(RoundedCornerShape(16.dp))
                            // 无描边纯净流体背景
                            .background(Color(0xFFF1F5F9))
                    )
                }
            }

            // Tab 图标与标签列表 (两侧预留 8dp 内边距，避免端项碰撞圆角)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = index == currentSelectedIndex

                    LiquidNavItemView(
                        item = item,
                        isSelected = isSelected,
                        activeColor = activeColor,
                        inactiveColor = inactiveColor,
                        onClick = {
                            if (currentSelectedIndex != index) {
                                currentSelectedIndex = index
                                onTabSelected(index)
                            }
                        },
                        onPositioned = { centerX, width ->
                            itemCenterXs[index] = centerX
                            itemWidths[index] = width
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

@Composable
private fun LiquidNavItemView(
    item: LiquidNavItem,
    isSelected: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit,
    onPositioned: (centerX: Float, width: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // 仅针对图标进行细腻弹性微缩放，文字保持稳定清晰
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.70f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "nav_icon_scale"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        animationSpec = tween(durationMillis = 200),
        label = "nav_item_color"
    )

    val labelColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        animationSpec = tween(durationMillis = 200),
        label = "nav_label_color"
    )

    Box(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .onGloballyPositioned { coordinates ->
                val posX = coordinates.positionInParent().x
                val width = coordinates.size.width.toFloat()
                onPositioned(posX + (width / 2f), width)
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.scale(iconScale)
            ) {
                Icon(
                    imageVector = if (isSelected && item.activeIcon != null) item.activeIcon else item.icon,
                    contentDescription = item.title,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )

                // 红点徽章通知 (如果有)
                if (item.badge != null && item.badge > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-4).dp)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF3B30)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (item.badge > 99) "99+" else item.badge.toString(),
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = item.title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = labelColor,
                letterSpacing = 0.2.sp
            )
        }
    }
}
