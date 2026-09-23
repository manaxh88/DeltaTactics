package com.delta.tactics.core.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * 全局图片文件级磁盘持久化缓存
 * 支持 SHA-256 散列命名、临时文件原子写入、后台批量预加载与 LRU 容量保护
 */
object ItemImageDiskCache {

    private const val TAG = "ItemImageDiskCache"
    private const val CACHE_DIR_NAME = "image_disk_cache"
    private const val MAX_CACHE_SIZE_BYTES = 120 * 1024 * 1024L // 120MB 容量上限

    private fun getCacheDir(context: Context): File {
        val dir = File(context.cacheDir, CACHE_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * 将 URL 转为 64 位 SHA-256 安全文件名
     */
    fun hashUrl(url: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(url.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            // 降级使用 hashCode
            url.hashCode().toString()
        }
    }

    /**
     * 判断指定 URL 是否已被持久化在磁盘
     */
    fun has(context: Context, url: String): Boolean {
        if (url.isBlank()) return false
        val file = File(getCacheDir(context), hashUrl(url))
        return file.exists() && file.length() > 0
    }

    /**
     * 从磁盘读取并解码 Bitmap
     */
    fun get(context: Context, url: String): Bitmap? {
        if (url.isBlank()) return null
        return try {
            val file = File(getCacheDir(context), hashUrl(url))
            if (file.exists() && file.length() > 0) {
                // 更新访问时间以支持 LRU 清理
                file.setLastModified(System.currentTimeMillis())
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode cached bitmap for $url: ${e.message}")
            null
        }
    }

    /**
     * 将二进制图片数据原子写入磁盘
     */
    fun put(context: Context, url: String, data: ByteArray): Boolean {
        if (url.isBlank() || data.isEmpty()) return false
        return try {
            val dir = getCacheDir(context)
            val hash = hashUrl(url)
            val targetFile = File(dir, hash)
            val tmpFile = File(dir, "$hash.tmp")

            FileOutputStream(tmpFile).use { fos ->
                fos.write(data)
                fos.flush()
            }

            if (tmpFile.renameTo(targetFile)) {
                trimCacheIfNeeded(context)
                true
            } else {
                tmpFile.delete()
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write disk cache for $url: ${e.message}")
            false
        }
    }

    /**
     * 将 Bitmap 压缩并原子保存到磁盘
     */
    fun put(context: Context, url: String, bitmap: Bitmap): Boolean {
        if (url.isBlank()) return false
        return try {
            val dir = getCacheDir(context)
            val hash = hashUrl(url)
            val targetFile = File(dir, hash)
            val tmpFile = File(dir, "$hash.tmp")

            FileOutputStream(tmpFile).use { fos ->
                val format = if (bitmap.hasAlpha()) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                bitmap.compress(format, 92, fos)
                fos.flush()
            }

            if (tmpFile.renameTo(targetFile)) {
                trimCacheIfNeeded(context)
                true
            } else {
                tmpFile.delete()
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save bitmap to disk cache for $url: ${e.message}")
            false
        }
    }

    /**
     * 后台静默预热指定图片到本地磁盘
     */
    suspend fun preload(context: Context, url: String): Boolean = withContext(Dispatchers.IO) {
        if (url.isBlank() || has(context, url)) return@withContext true
        try {
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
                    return@withContext put(context, url, bytes)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Preload failed for $url: ${e.message}")
        }
        false
    }

    /**
     * 超过容量上限时自动清除最久未访问的文件
     */
    private fun trimCacheIfNeeded(context: Context) {
        try {
            val dir = getCacheDir(context)
            val files = dir.listFiles { file -> file.isFile && !file.name.endsWith(".tmp") } ?: return
            var totalSize = files.sumOf { it.length() }
            if (totalSize > MAX_CACHE_SIZE_BYTES) {
                // 按修改时间升序排列，淘汰最老的文件
                val sortedFiles = files.sortedBy { it.lastModified() }
                for (file in sortedFiles) {
                    val len = file.length()
                    if (file.delete()) {
                        totalSize -= len
                        if (totalSize <= MAX_CACHE_SIZE_BYTES * 0.8) {
                            break // 缩减到 80% 容量停止
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error trimming cache: ${e.message}")
        }
    }
}
