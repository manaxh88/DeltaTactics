package com.delta.tactics.presentation.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
 * 白色拟态液态玻璃常驻底栏 (White Liquid Glass Bottom Bar)
 *
 * 核心设计：
 * 1. 拟态玻璃多层半透明质感（Glassmorphic translucent gradient layers, 85%~92% 通透乳白）
 * 2. 顶部 1.5px 镜面反射高光弧（Specular Highlight Rim）
 * 3. 棱镜微折射渐变描边（Refractive glass border）
 * 4. 绝对严格对称的水滴流体胶囊（100% 同容器同系坐标，彻底消除左右偏心与不对称）
 * 5. 凸透镜水滴拟态（水滴浮起微阴影 + 表面张力水珠边缘 + 顶部微高光弧）
 * 6. 黑色高对比度字体与图标（深黑 #0F172A，清晰优雅）
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

    // 记录各 Tab 项在相同容器中的精确中心水平坐标
    val itemCenterXs = remember { mutableStateMapOf<Int, Dp>() }

    val targetCenterX = remember(currentSelectedIndex, itemCenterXs.toMap()) {
        itemCenterXs[currentSelectedIndex] ?: 0.dp
    }

    // 水滴指示器水平中心 X 坐标的弹性物理动画 (阻尼 0.72 带来水滴拉伸与平滑停靠质感)
    val animatedCenterX by animateDpAsState(
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
        // 外层柔和弥散阴影与白色半透明液态玻璃容器
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(33.dp),
                    spotColor = Color(0x380F172A),
                    ambientColor = Color(0x180F172A)
                )
                .clip(RoundedCornerShape(33.dp))
                // 白色半透明液态玻璃多层渐变 (85%~92% 通透乳白毛玻璃质感)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.92f),
                            Color(0xFFF8FAFC).copy(alpha = 0.85f),
                            Color.White.copy(alpha = 0.89f)
                        )
                    )
                )
                // 玻璃边缘折射描边 (顶部纯白光照，底部浅灰自然收敛)
                .border(
                    BorderStroke(
                        width = 1.2.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White,
                                Color.White.copy(alpha = 0.70f),
                                Color(0xFFCBD5E1).copy(alpha = 0.45f)
                            )
                        )
                    ),
                    shape = RoundedCornerShape(33.dp)
                )
        ) {
            // 顶部 1.5px 镜面受光反光线 (Specular Glass Top Rim)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 24.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.45f),
                                Color.White,
                                Color.White.copy(alpha = 0.45f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // 【关键对称架构】Tab 核心容器（水滴指示器与 Tab 列表处于同一个直接父容器内，坐标 100% 绝对一致，严格消除偏移不对称）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                // 液态流体水滴凸透镜指示器 (严格居中对齐选中项)
                if (animatedCenterX > 0.dp) {
                    val blobWidth = 54.dp
                    val blobHeight = 46.dp
                    val blobLeft = animatedCenterX - (blobWidth / 2f)

                    if (blobLeft >= 0.dp) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = blobLeft)
                                .width(blobWidth)
                                .height(blobHeight)
                                // 液态水滴凸起立体阴影
                                .shadow(
                                    elevation = 4.dp,
                                    shape = RoundedCornerShape(23.dp),
                                    spotColor = Color(0x280F172A),
                                    ambientColor = Color(0x100F172A)
                                )
                                .clip(RoundedCornerShape(23.dp))
                                // 水滴凸透镜通透渐变
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.96f),
                                            Color(0xFFF8FAFC).copy(alpha = 0.88f),
                                            Color(0xFFE2E8F0).copy(alpha = 0.72f)
                                        )
                                    )
                                )
                                // 水滴表面张力水珠晶莹边缘
                                .border(
                                    BorderStroke(
                                        width = 1.dp,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White,
                                                Color.White.copy(alpha = 0.75f),
                                                Color(0xFFCBD5E1).copy(alpha = 0.40f)
                                            )
                                        )
                                    ),
                                    shape = RoundedCornerShape(23.dp)
                                )
                        ) {
                            // 水滴顶部 1px 晶莹微弧高光
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .align(Alignment.TopCenter)
                                    .padding(horizontal = 10.dp)
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.White,
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }

                // Tab 图标与标签列表（在同级容器中铺满，子项中心坐标直传水滴）
                Row(
                    modifier = Modifier.fillMaxSize(),
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
                            onPositioned = { centerDp ->
                                itemCenterXs[index] = centerDp
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

@Composable
private fun LiquidNavItemView(
    item: LiquidNavItem,
    isSelected: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit,
    onPositioned: (centerDp: Dp) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    // 仅针对图标进行细腻弹性微缩放，文字保持稳定清晰
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.10f else 1.0f,
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
                val centerDp = with(density) { (posX + (width / 2f)).toDp() }
                onPositioned(centerDp)
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
