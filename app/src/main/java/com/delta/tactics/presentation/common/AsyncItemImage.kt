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
import androidx.compose.ui.unit.dp
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
 * 战术装备/武器透明高清图片异步加载组件
 * 纯原生 Compose + Coroutines + 内存 LRU 缓存，无外部第三方依赖，低开销、高性能
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
    var cachedBitmap by remember(url) { mutableStateOf(ItemImageMemoryCache.get(url)) }
    var isLoading by remember(url) { mutableStateOf(cachedBitmap == null && url.isNotBlank()) }

    LaunchedEffect(url) {
        if (url.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }
        val memoryHit = ItemImageMemoryCache.get(url)
        if (memoryHit != null) {
            cachedBitmap = memoryHit
            isLoading = false
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 6000
                conn.readTimeout = 8000
                conn.instanceFollowRedirects = true
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android DeltaTactics)")
                conn.doInput = true
                conn.connect()

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    conn.inputStream.use { inputStream ->
                        val decoded = BitmapFactory.decodeStream(inputStream)
                        if (decoded != null) {
                            ItemImageMemoryCache.put(url, decoded)
                            cachedBitmap = decoded
                        }
                    }
                }
            } catch (_: Throwable) {
                // 网络异常或加载失败，fallback 接管
            } finally {
                isLoading = false
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
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.5.dp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                else -> {
                    if (fallback != null) {
                        fallback()
                    }
                }
            }
        }
    }
}
