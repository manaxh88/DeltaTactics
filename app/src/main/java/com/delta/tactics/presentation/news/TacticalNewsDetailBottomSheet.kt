package com.delta.tactics.presentation.news

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.delta.tactics.domain.model.TacticalNewsDetail
import com.delta.tactics.domain.model.TacticalNewsItem
import com.delta.tactics.presentation.common.AsyncItemImage

private val CardWhite = Color(0xFFFFFFFF)
private val TextPrimaryDark = Color(0xFF0F172A)
private val TextSecondaryGray = Color(0xFF64748B)
private val ThemeBlue = Color(0xFF2563EB)
private val ThemeBlueSoft = Color(0xFFEFF6FF)
private val RedAccent = Color(0xFFEF4444)

/**
 * 战术资讯详情弹窗抽屉
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TacticalNewsDetailBottomSheet(
    sheetState: SheetState,
    detail: TacticalNewsDetail?,
    briefItem: TacticalNewsItem?,
    isLoading: Boolean,
    errorMessage: String?,
    onDismissRequest: () -> Unit,
    onRetry: () -> Unit
) {
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
            // 顶栏：标题与关闭按钮
            val displayTitle = detail?.title ?: briefItem?.title ?: "战术资讯详情"
            val displayAuthor = detail?.authorName ?: briefItem?.author ?: "三角洲行动"
            val displayAvatar = detail?.authorAvatar ?: briefItem?.avatarUrl ?: ""
            val displayDate = briefItem?.createdAt ?: ""
            val displayViews = briefItem?.viewCount ?: 0
            val displayLikes = briefItem?.likedCount ?: 0

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = displayTitle,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        lineHeight = 24.sp,
                        modifier = Modifier.weight(1f)
                    )
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

                // 作者信息与发布时间、浏览点赞统计行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (displayAvatar.isNotBlank()) {
                            AsyncItemImage(
                                url = displayAvatar,
                                contentDescription = displayAuthor,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Text(
                            text = displayAuthor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(ThemeBlueSoft)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "官方",
                                fontSize = 10.sp,
                                color = ThemeBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (displayDate.isNotBlank()) {
                            Text(
                                text = TacticalNewsViewModel.formatDate(displayDate),
                                fontSize = 11.sp,
                                color = TextSecondaryGray
                            )
                        }
                    }

                    // 统计角标
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = TextSecondaryGray,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = TacticalNewsViewModel.formatCount(displayViews),
                                fontSize = 11.sp,
                                color = TextSecondaryGray
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = RedAccent,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = TacticalNewsViewModel.formatCount(displayLikes),
                                fontSize = 11.sp,
                                color = TextSecondaryGray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(
                color = Color(0xFFF1F5F9),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            // 正文区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = ThemeBlue,
                                strokeWidth = 2.5.dp
                            )
                            Text(
                                text = "正在同步官方富文本与战术图报...",
                                fontSize = 13.sp,
                                color = TextSecondaryGray
                            )
                        }
                    }
                    errorMessage != null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = errorMessage,
                                fontSize = 13.sp,
                                color = RedAccent
                            )
                            Button(
                                onClick = onRetry,
                                colors = ButtonDefaults.buttonColors(containerColor = ThemeBlue)
                            ) {
                                Text("重试加载", fontSize = 13.sp)
                            }
                        }
                    }
                    detail != null -> {
                        ArticleHtmlViewer(
                            htmlContent = detail.contentHtml,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

/**
 * 优雅内嵌 WebView 渲染 HTML 文章，支持图片自适应全宽、手势缩放与流畅排版
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ArticleHtmlViewer(
    htmlContent: String,
    modifier: Modifier = Modifier
) {
    val styledHtml = buildStyledHtml(htmlContent)

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isVerticalScrollBarEnabled = true
                isHorizontalScrollBarEnabled = false

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                    defaultTextEncodingName = "UTF-8"
                }
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(
                "https://www.shushu.fan/",
                styledHtml,
                "text/html",
                "utf-8",
                null
            )
        }
    )
}

/**
 * 构建适配移动端的样式模板
 */
private fun buildStyledHtml(rawHtml: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=3.0, user-scalable=yes">
            <style>
                * {
                    box-sizing: border-box;
                    margin: 0;
                    padding: 0;
                }
                body {
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
                    font-size: 14px;
                    line-height: 1.75;
                    color: #1E293B;
                    background-color: transparent;
                    padding: 8px 4px 32px 4px;
                    word-wrap: break-word;
                    word-break: break-word;
                }
                p {
                    margin-bottom: 12px;
                }
                img {
                    display: block;
                    max-width: 100% !important;
                    height: auto !important;
                    border-radius: 10px;
                    margin: 8px auto;
                    box-shadow: 0 2px 8px rgba(0,0,0,0.06);
                }
                .article-video, iframe {
                    width: 100% !important;
                    aspect-ratio: 16 / 9;
                    border: none;
                    border-radius: 10px;
                    margin: 12px 0;
                }
                h1, h2, h3, h4 {
                    color: #0F172A;
                    margin: 16px 0 8px 0;
                    font-weight: 700;
                }
                h1 { font-size: 18px; }
                h2 { font-size: 16px; }
                h3 { font-size: 15px; }
                strong {
                    color: #0F172A;
                    font-weight: 600;
                }
                a {
                    color: #2563EB;
                    text-decoration: none;
                }
            </style>
        </head>
        <body>
            $rawHtml
        </body>
        </html>
    """.trimIndent()
}
