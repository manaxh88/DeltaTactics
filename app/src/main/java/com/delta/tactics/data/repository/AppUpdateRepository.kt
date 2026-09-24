package com.delta.tactics.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
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

    // Only proxy this project's release assets; already accelerated URLs remain unchanged.
    private fun acceleratedApkUrl(url: String): String =
        if (url.startsWith("https://github.com/manaxh88/DeltaTactics/releases/download/")) {
            "https://ghfast.top/$url"
        } else url

    private val githubApiUrl = "https://api.github.com/repos/manaxh88/DeltaTactics/releases/latest"
    private val ghfastManifestUrl = "https://ghfast.top/https://raw.githubusercontent.com/manaxh88/DeltaTactics/main/version.json"
    private val ghproxyManifestUrl = "https://ghproxy.net/https://raw.githubusercontent.com/manaxh88/DeltaTactics/main/version.json"
    private val cdnManifestUrl = "https://fastly.jsdelivr.net/gh/manaxh88/DeltaTactics@main/version.json"
    private val rawManifestUrl = "https://raw.githubusercontent.com/manaxh88/DeltaTactics/main/version.json"

    /**
     * 动态获取当前已安装 App 的版本代号 (versionCode)
     */
    fun getInstalledVersionCode(): Int {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            PackageInfoCompat.getLongVersionCode(packageInfo).toInt()
        } catch (e: Exception) {
            20
        }
    }

    /**
     * 动态获取当前已安装 App 的版本名称 (versionName)
     */
    fun getInstalledVersionName(): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "2.9.0"
        } catch (e: Exception) {
            "2.9.0"
        }
    }

    /**
     * 语义化版本号比对：判断 remoteVersion 是否严格大于 currentVersion
     * 支持例如 "2.9.0" vs "2.8.9" (true), "v2.9.0" vs "2.9.0" (false), "2.8.9" vs "2.9.0" (false)
     */
    fun isVersionNewer(remoteVersion: String, currentVersion: String): Boolean {
        if (remoteVersion.isBlank() || currentVersion.isBlank()) return false
        val rClean = remoteVersion.trim().removePrefix("v").removePrefix("V").split("-", "_")[0]
        val cClean = currentVersion.trim().removePrefix("v").removePrefix("V").split("-", "_")[0]
        val rParts = rClean.split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
        val cParts = cClean.split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
        val maxLen = maxOf(rParts.size, cParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    /**
     * 检查新版本（多通道：国内直通加速镜像 ghfast -> ghproxy -> jsDelivr CDN -> Raw manifest -> GitHub API）
     */
    suspend fun checkUpdate(
        currentVersionCode: Int = getInstalledVersionCode(),
        currentVersionName: String = getInstalledVersionName()
    ): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        // 1. 优先尝试国内加速直连通道 1 (ghfast.top)
        val ghfastResult = tryFetchFromManifest(ghfastManifestUrl, currentVersionCode, currentVersionName)
        if (ghfastResult.isSuccess) {
            return@withContext ghfastResult
        }

        // 2. 尝试国内加速直连通道 2 (ghproxy.net)
        val ghproxyResult = tryFetchFromManifest(ghproxyManifestUrl, currentVersionCode, currentVersionName)
        if (ghproxyResult.isSuccess) {
            return@withContext ghproxyResult
        }

        // 3. 备选通道：从 jsDelivr CDN 查询
        val cdnResult = tryFetchFromManifest(cdnManifestUrl, currentVersionCode, currentVersionName)
        if (cdnResult.isSuccess) {
            return@withContext cdnResult
        }

        // 4. GitHub raw 直连
        val rawResult = tryFetchFromManifest(rawManifestUrl, currentVersionCode, currentVersionName)
        if (rawResult.isSuccess) {
            return@withContext rawResult
        }

        // 5. 尝试从 GitHub Releases 官方 API 查询 (带频率限制保底)
        val githubResult = tryFetchFromGithubApi(currentVersionCode, currentVersionName)
        if (githubResult.isSuccess) {
            return@withContext githubResult
        }

        // 如果全部请求失败，返回国内加速直连的异常
        ghfastResult
    }

    private fun tryFetchFromGithubApi(currentVersionCode: Int, currentVersionName: String): Result<AppUpdateInfo> {
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

            val tagName = json.optString("tag_name", "").removePrefix("v").removePrefix("V").trim()
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

            // 解析版本号：优先尝试从 body 中提取显式声明的 versionCode: 16
            val codeRegex = Regex("""versionCode[:=]\s*(\d+)""", RegexOption.IGNORE_CASE)
            val parsedCode = codeRegex.find(releaseNotes)?.groupValues?.get(1)?.toIntOrNull()

            // 升级判定：如果显式声明了同维度的 versionCode 则比对，否则基于语义化版本比对
            val hasUpdate = if (parsedCode != null && parsedCode > 0) {
                (parsedCode > currentVersionCode) || isVersionNewer(tagName, currentVersionName)
            } else {
                isVersionNewer(tagName, currentVersionName)
            }

            val finalVersionCode = parsedCode ?: parseVersionNameToCode(tagName)

            Result.success(
                AppUpdateInfo(
                    versionCode = finalVersionCode,
                    versionName = tagName.ifBlank { "最新版" },
                    title = releaseTitle,
                    changelog = releaseNotes,
                    apkUrl = acceleratedApkUrl(apkUrl),
                    browserUrl = browserUrl,
                    forceUpdate = false,
                    hasUpdate = hasUpdate
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun tryFetchFromManifest(manifestUrl: String, currentVersionCode: Int, currentVersionName: String): Result<AppUpdateInfo> {
        return try {
            val separator = if (manifestUrl.contains("?")) "&" else "?"
            val finalUrl = "$manifestUrl${separator}_t=${System.currentTimeMillis()}"
            val url = URL(finalUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "DeltaTactics-Android-Client")
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }

            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                return Result.failure(Exception("Manifest HTTP ${conn.responseCode}"))
            }

            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(body)

            val targetCode = json.optInt("versionCode", currentVersionCode)
            val versionName = json.optString("versionName", "2.9.0")
            val title = json.optString("title", "三角洲战术助手 版本更新")
            val changelog = json.optString("changelog", "暂无更新说明")
            val apkUrl = json.optString("apkUrl", "")
            val browserUrl = json.optString("browserUrl", "https://github.com/manaxh88/DeltaTactics/releases/latest")
            val forceUpdate = json.optBoolean("forceUpdate", false)

            val hasUpdate = (targetCode > currentVersionCode) || isVersionNewer(versionName, currentVersionName)

            Result.success(
                AppUpdateInfo(
                    versionCode = targetCode,
                    versionName = versionName,
                    title = title,
                    changelog = changelog,
                    apkUrl = acceleratedApkUrl(apkUrl),
                    browserUrl = browserUrl,
                    forceUpdate = forceUpdate,
                    hasUpdate = hasUpdate
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 流式下载 APK 并在下载过程中回调百分比进度（支持多通道候选镜像智能降级）
     */
    suspend fun downloadApk(
        apkUrl: String,
        onProgress: (progress: Float, currentBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val candidateUrls = mutableListOf<String>()
        candidateUrls.add(apkUrl)
        if (apkUrl.contains("github.com/")) {
            val rawPath = apkUrl
                .removePrefix("https://ghfast.top/")
                .removePrefix("https://ghproxy.net/")
                .removePrefix("https://hub.gitmirror.com/")
            candidateUrls.add("https://ghproxy.net/$rawPath")
            candidateUrls.add("https://ghfast.top/$rawPath")
            candidateUrls.add(rawPath)
        }

        var lastError: Exception? = null
        for (urlStr in candidateUrls.distinct()) {
            val result = downloadSingleUrl(urlStr, onProgress)
            if (result.isSuccess) {
                return@withContext result
            } else {
                lastError = result.exceptionOrNull() as? Exception
            }
        }
        Result.failure(lastError ?: Exception("下载更新失败，请重试或前往 GitHub 网页下载"))
    }

    private fun downloadSingleUrl(
        urlStr: String,
        onProgress: (progress: Float, currentBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> {
        return try {
            val url = URL(urlStr)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 30000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "DeltaTactics-Android-Client")
                instanceFollowRedirects = true
            }

            var actualConn = conn
            var responseCode = actualConn.responseCode
            var redirects = 0
            while ((responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == 307 || responseCode == 308) && redirects < 5
            ) {
                redirects++
                val newUrl = actualConn.getHeaderField("Location") ?: break
                actualConn = (URL(newUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 30000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "DeltaTactics-Android-Client")
                    instanceFollowRedirects = true
                }
                responseCode = actualConn.responseCode
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                return Result.failure(Exception("Download failed with HTTP $responseCode"))
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
     * 调起系统安装器（支持 Android 8.0+ 未知应用安装权限智能引导）
     */
    fun installApk(apkFile: File): Result<Unit> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val permissionIntent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(permissionIntent)
                    return Result.failure(Exception("请先在设置中允许本应用「安装未知应用」权限后重试"))
                }
            }

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
