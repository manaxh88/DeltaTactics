package com.delta.tactics.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.delta.tactics.domain.model.AppUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class AppUpdateRepository(private val context: Context) {

    private val githubApiUrl = "https://api.github.com/repos/manaxh88/DeltaTactics/releases/latest"
    private val cdnManifestUrl = "https://fastly.jsdelivr.net/gh/manaxh88/DeltaTactics@main/version.json"
    private val rawManifestUrl = "https://raw.githubusercontent.com/manaxh88/DeltaTactics/main/version.json"

    /**
     * 检查新版本（双通道：优先 GitHub Releases API，优雅降级至 CDN/Raw manifest）
     */
    suspend fun checkUpdate(currentVersionCode: Int): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        // 1. 尝试从 GitHub Releases API 查询
        val githubResult = tryFetchFromGithubApi(currentVersionCode)
        if (githubResult.isSuccess) {
            return@withContext githubResult
        }

        // 2. 备选通道：从 CDN / Raw version.json 查询
        val cdnResult = tryFetchFromManifest(cdnManifestUrl, currentVersionCode)
        if (cdnResult.isSuccess) {
            return@withContext cdnResult
        }

        val rawResult = tryFetchFromManifest(rawManifestUrl, currentVersionCode)
        if (rawResult.isSuccess) {
            return@withContext rawResult
        }

        // 如果全部请求失败，返回最近的异常
        githubResult
    }

    private fun tryFetchFromGithubApi(currentVersionCode: Int): Result<AppUpdateInfo> {
        return try {
            val url = URL(githubApiUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "DeltaTactics-Android-Client")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
            }

            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                return Result.failure(Exception("GitHub API HTTP ${conn.responseCode}"))
            }

            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(body)

            val tagName = json.optString("tag_name", "").removePrefix("v").trim()
            val releaseTitle = json.optString("name", "新版本发布")
            val releaseNotes = json.optString("body", "暂无更新说明")
            val browserUrl = json.optString("html_url", "https://github.com/manaxh88/DeltaTactics/releases")

            // 解析 APK 下载链接
            var apkUrl = browserUrl
            val assetsArray = json.optJSONArray("assets")
            if (assetsArray != null) {
                for (i in 0 until assetsArray.length()) {
                    val asset = assetsArray.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", browserUrl)
                        break
                    }
                }
            }

            // 解析版本号：优先尝试从 body 中提取 versionCode: 16，若无则根据 tag 估算
            val codeRegex = Regex("""versionCode[:=]\s*(\d+)""", RegexOption.IGNORE_CASE)
            val parsedCode = codeRegex.find(releaseNotes)?.groupValues?.get(1)?.toIntOrNull()
                ?: parseVersionNameToCode(tagName)

            val hasUpdate = parsedCode > currentVersionCode

            Result.success(
                AppUpdateInfo(
                    versionCode = parsedCode,
                    versionName = tagName.ifBlank { "最新版" },
                    title = releaseTitle,
                    changelog = releaseNotes,
                    apkUrl = apkUrl,
                    browserUrl = browserUrl,
                    forceUpdate = false,
                    hasUpdate = hasUpdate
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun tryFetchFromManifest(manifestUrl: String, currentVersionCode: Int): Result<AppUpdateInfo> {
        return try {
            val url = URL(manifestUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "DeltaTactics-Android-Client")
            }

            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                return Result.failure(Exception("Manifest HTTP ${conn.responseCode}"))
            }

            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(body)

            val targetCode = json.optInt("versionCode", currentVersionCode)
            val versionName = json.optString("versionName", "2.8.7")
            val title = json.optString("title", "三角洲助手 版本更新")
            val changelog = json.optString("changelog", "暂无更新说明")
            val apkUrl = json.optString("apkUrl", "")
            val browserUrl = json.optString("browserUrl", "https://github.com/manaxh88/DeltaTactics/releases/latest")
            val forceUpdate = json.optBoolean("forceUpdate", false)

            Result.success(
                AppUpdateInfo(
                    versionCode = targetCode,
                    versionName = versionName,
                    title = title,
                    changelog = changelog,
                    apkUrl = apkUrl,
                    browserUrl = browserUrl,
                    forceUpdate = forceUpdate,
                    hasUpdate = targetCode > currentVersionCode
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 流式下载 APK 并在下载过程中回调百分比进度
     */
    suspend fun downloadApk(
        apkUrl: String,
        onProgress: (progress: Float, currentBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val url = URL(apkUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 30000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "DeltaTactics-Android-Client")
                // 支持重定向 (GitHub releases 最终会 302 重定向到 AWS S3 / objects.githubusercontent.com)
                instanceFollowRedirects = true
            }

            var actualConn = conn
            var responseCode = actualConn.responseCode
            // 处理显式 301/302 重定向
            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                responseCode == 307 || responseCode == 308
            ) {
                val newUrl = actualConn.getHeaderField("Location")
                actualConn = (URL(newUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 30000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "DeltaTactics-Android-Client")
                }
                responseCode = actualConn.responseCode
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("Download failed with HTTP $responseCode"))
            }

            val totalBytes = actualConn.contentLengthLong
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
            val outputFile = File(downloadDir, "DeltaTactics_update.apk")
            if (outputFile.exists()) {
                outputFile.delete()
            }

            var downloadedBytes = 0L
            actualConn.inputStream.use { input: InputStream ->
                FileOutputStream(outputFile).use { output: FileOutputStream ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var lastNotifiedProgress = 0f

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val progress = if (totalBytes > 0) {
                            (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        } else {
                            0f
                        }

                        // 降低主线程通知频率，每增加 1% 通知一次
                        if (progress - lastNotifiedProgress >= 0.01f || downloadedBytes == totalBytes) {
                            lastNotifiedProgress = progress
                            onProgress(progress, downloadedBytes, totalBytes)
                        }
                    }
                    output.flush()
                }
            }

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 调起系统安装器
     */
    fun installApk(apkFile: File): Result<Unit> {
        return try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 辅助转换如 "2.8.5" -> 20805 整数版本代号
     */
    private fun parseVersionNameToCode(versionName: String): Int {
        return try {
            val parts = versionName.split(".")
            var code = 0
            for (p in parts) {
                code = code * 100 + (p.filter { it.isDigit() }.toIntOrNull() ?: 0)
            }
            code
        } catch (e: Exception) {
            0
        }
    }
}
