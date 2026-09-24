package com.delta.tactics.presentation.cipher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.delta.tactics.core.ui.theme.AppBackgroundLight
import com.delta.tactics.core.ui.theme.CardWhite
import com.delta.tactics.core.ui.theme.TacticalOrange
import com.delta.tactics.core.ui.theme.TextPrimaryDark
import com.delta.tactics.core.ui.theme.TextSecondaryGray

@Composable
fun DecryptCenterScreen(
    viewModel: CipherRoomViewModel,
    onShowToast: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val navBarsBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    var selectedMode by remember { mutableIntStateOf(0) } // 0: 摩斯电码解密, 1: 每日密码速查

    // 摩斯电码状态
    var morseInput by remember { mutableStateOf("") }
    var decodedResult by remember { mutableStateOf("") }
    val morseDict = remember {
        mapOf(
            ".----" to "1",
            "..---" to "2",
            "...--" to "3",
            "....-" to "4",
            "....." to "5",
            "-...." to "6",
            "--..." to "7",
            "---.." to "8",
            "----." to "9",
            "-----" to "0"
        )
    }

    val dailyPasswords by viewModel.dailyPasswords.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackgroundLight),
        contentPadding = PaddingValues(
            top = statusBarTopPadding + 16.dp,
            bottom = 110.dp + navBarsBottomPadding,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. 顶部分段切换栏 (摩斯电码解密 / 每日密码速查)
        item {
            Surface(
                color = Color(0xFFE2E8F0),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    listOf("摩斯电码智能解密", "每日地图密码").forEachIndexed { idx, title ->
                        val isSelected = selectedMode == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) CardWhite else Color.Transparent)
                                .clickable { selectedMode = idx }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) TextPrimaryDark else TextSecondaryGray
                            )
                        }
                    }
                }
            }
        }

        if (selectedMode == 0) {
            // 摩斯电码解密面板
            item {
                Surface(
                    color = CardWhite,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = TacticalOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "局内门锁摩斯解密器",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "站门前听声音：短音为滴(•)，长音为嗒(-)。每5音自动解码1位数字",
                            fontSize = 11.sp,
                            color = TextSecondaryGray
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // 实时电码与解码结果框
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (morseInput.isEmpty()) "点击下方按键输入电码" else morseInput,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (morseInput.isEmpty()) TextSecondaryGray else TextPrimaryDark,
                                    letterSpacing = 2.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "解码结果: ",
                                        fontSize = 13.sp,
                                        color = TextSecondaryGray
                                    )
                                    Text(
                                        text = decodedResult.ifEmpty { "----" },
                                        fontSize = 30.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 4.sp,
                                        color = if (decodedResult.isNotEmpty()) TacticalOrange else TextSecondaryGray
                                    )
                                    if (decodedResult.isNotEmpty()) {
                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(decodedResult))
                                                onShowToast("已复制密码: $decodedResult")
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "复制密码",
                                                tint = TacticalOrange,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 输入按键
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    morseInput += "•"
                                    if (morseInput.length % 5 == 0) {
                                        val chunk = morseInput.takeLast(5).replace("•", ".").replace("—", "-")
                                        morseDict[chunk]?.let { decodedResult += it }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("短音 • 滴", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    morseInput += "—"
                                    if (morseInput.length % 5 == 0) {
                                        val chunk = morseInput.takeLast(5).replace("•", ".").replace("—", "-")
                                        morseDict[chunk]?.let { decodedResult += it }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF334155),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("长音 — 嗒", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (morseInput.isNotEmpty()) {
                                        val wasChunkEnd = morseInput.length % 5 == 0
                                        morseInput = morseInput.dropLast(1)
                                        if (wasChunkEnd && decodedResult.isNotEmpty()) {
                                            decodedResult = decodedResult.dropLast(1)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF1F5F9),
                                    contentColor = TextPrimaryDark
                                )
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("回退", fontSize = 13.sp)
                            }

                            Button(
                                onClick = {
                                    morseInput = ""
                                    decodedResult = ""
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFEE2E2),
                                    contentColor = Color(0xFFDC2626)
                                )
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("清空重录", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        } else {
            // 每日地图密码状态提示与强制刷新栏
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "今日密码 (本地免流已保存)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondaryGray
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.syncDailyPasswords(force = true) { success ->
                                    onShowToast(if (success) "每日密码已同步至最新" else "同步失败，请检查网络")
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = TacticalOrange
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = TacticalOrange
                            )
                        }
                        Text(
                            text = if (isSyncing) "同步中..." else "强制刷新",
                            fontSize = 12.sp,
                            color = TacticalOrange,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 每日地图密码卡片列表
            items(dailyPasswords, key = { it.mapName }) { item ->
                Surface(
                    color = CardWhite,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
                        .clickable {
                            clipboardManager.setText(AnnotatedString(item.code))
                            onShowToast("已复制 [${item.mapName}] 密码: ${item.code}")
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFFF7ED)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = null,
                                    tint = TacticalOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = item.mapName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "每日 0 点自动轮换",
                                    fontSize = 11.sp,
                                    color = TextSecondaryGray
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = item.code,
                                fontSize = 22.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = TacticalOrange
                            )
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = TextSecondaryGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
