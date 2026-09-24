package com.delta.tactics.presentation.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.delta.tactics.core.ui.theme.*

/**
 * 桌面小组件添加引导弹窗
 * 完美解决国产机型 (小米HyperOS/MIUI、华为鸿蒙、OPPO、vivo等) 默认拦截 requestPinAppWidget 导致点击无反应的问题
 */
@Composable
fun WidgetGuideDialog(
    pinRequested: Boolean = true,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardWhite,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 顶栏图标与标题
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TacticalOrangeSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = null,
                            tint = TacticalOrange,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "添加桌面每日密码小组件",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "4x2 全景卡片 · 点击一键复制密码",
                            fontSize = 12.sp,
                            color = TextSecondaryGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 系统安全机制说明卡片
                Surface(
                    color = Color(0xFFFFF7ED),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFEA580C),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "已向系统桌面发起添加请求！\n因部分安卓机型（小米澎湃OS/MIUI、华为鸿蒙、OPPO、vivo等）默认禁止第三方应用自动生成桌面部件，若未弹出系统确认框，请按下述方法手动添加：",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = Color(0xFF9A3412)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 手动添加 3 步操作引导
                Text(
                    text = "📋 手机桌面手动添加步骤（通用秒成）：",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )

                Spacer(modifier = Modifier.height(10.dp))

                StepItem(
                    step = "1",
                    title = "返回手机主屏幕",
                    desc = "在桌面空白处【长按】或【双指捏合】唤出桌面设置菜单"
                )

                Spacer(modifier = Modifier.height(8.dp))

                StepItem(
                    step = "2",
                    title = "点击「小组件」或「微件」",
                    desc = "在底部菜单找到【小组件】/【微件】/【桌面插件】/【卡片】入口"
                )

                Spacer(modifier = Modifier.height(8.dp))

                StepItem(
                    step = "3",
                    title = "找到「三角洲助手」并添加",
                    desc = "在列表中找到本应用，长按或点击【每日密码速查 (4×2)】放置到桌面"
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 操作按钮组
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 主按钮：立即返回桌面（方便长按）
                    Button(
                        onClick = {
                            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_HOME)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(homeIntent)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TacticalOrange,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "返回手机桌面去添加",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 辅助按钮：去系统设置检查快捷方式权限
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextPrimaryDark
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "检查应用桌面快捷方式权限",
                            fontSize = 12.sp
                        )
                    }

                    // 关闭
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "我知道了",
                            fontSize = 12.sp,
                            color = TextSecondaryGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepItem(
    step: String,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8FAFC))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(TacticalDark),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = TextSecondaryGray
            )
        }
    }
}
