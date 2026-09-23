package com.delta.tactics.presentation.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.delta.tactics.core.cache.ItemImageDiskCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * 内存位图缓存 (LRU Cache)，基于可用应用内存的 1/8
 */
object ItemImageMemoryCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 8).coerceAtLeast(1024)

    private val lruCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return (bitmap.byteCount / 1024).coerceAtLeast(1)
        }
    }

    fun get(url: String): Bitmap? = lruCache.get(url)

    fun put(url: String, bitmap: Bitmap) {
        if (get(url) == null) {
            lruCache.put(url, bitmap)
        }
    }
}

/**
 * 战术装备/武器/房卡/资讯图片异步加载组件
 * 采用【内存 LRU -> 本地磁盘持久化 -> 网络流式下载】三级缓存架构
 * 离线秒开、零重复网络开销
 */
@Composable
fun AsyncItemImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    placeholder: (@Composable () -> Unit)? = null,
    fallback: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current.applicationContext
    var cachedBitmap by remember(url) { mutableStateOf(ItemImageMemoryCache.get(url)) }
    var isLoading by remember(url) { mutableStateOf(cachedBitmap == null && url.isNotBlank()) }

    LaunchedEffect(url) {
        if (url.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }
        // 1. 内存命中，直接使用
        val memoryHit = ItemImageMemoryCache.get(url)
        if (memoryHit != null) {
            cachedBitmap = memoryHit
            isLoading = false
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            try {
                // 2. 检查本地磁盘持久化缓存（房卡、武器、已浏览封面等）
                val diskHit = ItemImageDiskCache.get(context, url)
                if (diskHit != null) {
                    ItemImageMemoryCache.put(url, diskHit)
                    withContext(Dispatchers.Main) {
                        cachedBitmap = diskHit
                        isLoading = false
                    }
                    return@withContext
                }

                // 3. 磁盘未命中，从网络下载并自动写入磁盘持久化
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6000
                    readTimeout = 8000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android DeltaTactics)")
                    doInput = true
                }

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val bytes = conn.inputStream.use { it.readBytes() }
                    if (bytes.isNotEmpty()) {
                        // 异步持久化到磁盘
                        ItemImageDiskCache.put(context, url, bytes)
                        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (decoded != null) {
                            ItemImageMemoryCache.put(url, decoded)
                            withContext(Dispatchers.Main) {
                                cachedBitmap = decoded
                            }
                        }
                    }
                }
            } catch (_: Throwable) {
                // 网络异常或加载失败，由 fallback 接管
            } finally {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Crossfade(
            targetState = Triple(cachedBitmap, isLoading, url),
            animationSpec = tween(durationMillis = 200),
            label = "async_image_crossfade"
        ) { (bmp, loading, _) ->
            when {
                bmp != null -> {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = contentDescription,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = contentScale
                    )
                }
                loading -> {
                    if (placeholder != null) {
                        placeholder()
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFF94A3B8),
                            strokeWidth = 2.dp
                        )
                    }
                }
                else -> {
                    if (fallback != null) {
                        fallback()
                    } else {
                        // 默认占位占空
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}
