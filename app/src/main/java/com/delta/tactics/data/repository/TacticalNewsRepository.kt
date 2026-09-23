package com.delta.tactics.data.repository

import android.content.Context
import android.util.Log
import com.delta.tactics.domain.model.TacticalNewsDetail
import com.delta.tactics.domain.model.TacticalNewsItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class TacticalNewsRepository(private val context: Context? = null) {

    private val baseUrl = "https://www.shushu.fan/api/articles"
    private val detailCache = mutableMapOf<Long, TacticalNewsDetail>()
    private var cachedNewsList: List<TacticalNewsItem>? = null
    private var lastFetchTime: Long = 0L
    private val cacheDurationMs = 60 * 60 * 1000L // 1 小时

    private val newsDir: File? by lazy {
        context?.filesDir?.resolve("news_cache")?.also {
            if (!it.exists()) it.mkdirs()
        }
    }

    private val detailsDir: File? by lazy {
        newsDir?.resolve("details")?.also {
            if (!it.exists()) it.mkdirs()
        }
    }

    private val listFile: File? by lazy {
        newsDir?.resolve("news_list.json")
    }

    /**
     * 规范化图片与资源 URL (例如补齐 //static.gametalk.qq.com 前的 https:)
     */
    fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("//") -> "https:$trimmed"
            trimmed.startsWith("http://") -> "https://" + trimmed.removePrefix("http://")
            else -> trimmed
        }
    }

    /**
     * 同步/快速获取本地磁盘已持久化的资讯列表（用于冷启动秒开与断网离线展现）
     */
    fun getDiskCachedNewsList(): List<TacticalNewsItem> {
        cachedNewsList?.let { return it }

        val file = listFile
        if (file != null && file.exists() && file.length() > 0) {
            try {
                val json = file.readText(Charsets.UTF_8)
                val items = parseArticlesJson(json)
                if (items.isNotEmpty()) {
                    cachedNewsList = items
                    return items
                }
            } catch (e: Exception) {
                Log.w("TacticalNewsRepo", "Failed to read disk news list: ${e.message}")
            }
        }
        return getFallbackNewsList()
    }

    /**
     * 分页拉取战术资讯列表 (支持本地磁盘持久化、增量合并与 1 小时缓存策略)
     */
    suspend fun fetchArticles(page: Int = 1, limit: Int = 20, force: Boolean = false): Result<List<TacticalNewsItem>> = withContext(Dispatchers.IO) {
        if (!force && page == 1 && cachedNewsList != null && (System.currentTimeMillis() - lastFetchTime < cacheDurationMs)) {
            return@withContext Result.success(cachedNewsList!!)
        }

        try {
            val endpoint = "$baseUrl?page=$page&limit=$limit"
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                setRequestProperty("Referer", "https://www.shushu.fan/")
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                if (page == 1) {
                    val fallback = getDiskCachedNewsList()
                    return@withContext Result.success(fallback)
                }
                return@withContext Result.failure(Exception("HTTP ${conn.responseCode}"))
            }

            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val parsedList = parseArticlesJson(body)

            if (page == 1) {
                // 增量合并策略：保留本地历史老新闻，将最新拉取到的新闻合并并去重
                val mergedList = mergeNewsList(parsedList)
                cachedNewsList = mergedList
                lastFetchTime = System.currentTimeMillis()
                // 写入磁盘持久化
                saveNewsListToDisk(mergedList)

                // 预加载前 3 篇最新文章详情至本地，点击秒开
                preloadTopArticles(mergedList.take(3))
                return@withContext Result.success(mergedList)
            }

            Result.success(parsedList)
        } catch (e: Exception) {
            // 网络异常时，如果是第一页直接返回本地磁盘持久化数据
            if (page == 1) {
                val fallback = getDiskCachedNewsList()
                return@withContext Result.success(fallback)
            }
            Result.failure(e)
        }
    }

    /**
     * 增量合并新旧资讯列表，去重并保持最新时间排序
     */
    private fun mergeNewsList(newItems: List<TacticalNewsItem>): List<TacticalNewsItem> {
        val existing = getDiskCachedNewsList()
        val map = LinkedHashMap<Long, TacticalNewsItem>()
        // 先放入最新拉取的数据
        for (item in newItems) {
            map[item.threadId] = item
        }
        // 再补充本地已有但新列表中未包含的老新闻
        for (item in existing) {
            if (!map.containsKey(item.threadId)) {
                map[item.threadId] = item
            }
        }
        return map.values.toList()
    }

    /**
     * 将资讯列表持久化到内部磁盘
     */
    private fun saveNewsListToDisk(items: List<TacticalNewsItem>) {
        val file = listFile ?: return
        try {
            val root = JSONObject()
            root.put("success", true)
            val dataObj = JSONObject()
            val itemsArr = JSONArray()
            for (item in items) {
                val obj = JSONObject().apply {
                    put("threadID", item.threadId)
                    put("dataID", item.dataId)
                    put("title", item.title)
                    put("cover", item.coverUrl)
                    put("author", item.author)
                    put("avatar", item.avatarUrl)
                    put("createdAt", item.createdAt)
                    put("viewCount", item.viewCount)
                    put("likedCount", item.likedCount)
                }
                itemsArr.put(obj)
            }
            dataObj.put("items", itemsArr)
            root.put("data", dataObj)

            val tmp = File(file.parentFile, "${file.name}.tmp")
            FileOutputStream(tmp).use { fos ->
                fos.write(root.toString().toByteArray(Charsets.UTF_8))
                fos.flush()
            }
            tmp.renameTo(file)
        } catch (e: Exception) {
            Log.w("TacticalNewsRepo", "Failed to save news list to disk: ${e.message}")
        }
    }

    /**
     * 拉取指定资讯的文章详情（老新闻永久磁盘缓存，零重复网络消耗）
     */
    suspend fun fetchArticleDetail(threadId: Long): Result<TacticalNewsDetail> = withContext(Dispatchers.IO) {
        // 1. 内存缓存命中
        detailCache[threadId]?.let { return@withContext Result.success(it) }

        // 2. 本地磁盘持久化缓存命中（已发布的老新闻不会做改变，直接秒开）
        val detailFile = detailsDir?.let { File(it, "$threadId.json") }
        if (detailFile != null && detailFile.exists() && detailFile.length() > 0) {
            try {
                val json = detailFile.readText(Charsets.UTF_8)
                val detail = parseArticleDetailJson(threadId, json)
                if (detail != null) {
                    detailCache[threadId] = detail
                    return@withContext Result.success(detail)
                }
            } catch (e: Exception) {
                Log.w("TacticalNewsRepo", "Failed to read disk detail for $threadId: ${e.message}")
            }
        }

        // 3. 本地无缓存时请求网络（新发布的新闻）
        try {
            val endpoint = "$baseUrl/$threadId"
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                setRequestProperty("Referer", "https://www.shushu.fan/")
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("HTTP ${conn.responseCode}"))
            }

            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val detail = parseArticleDetailJson(threadId, body)
            if (detail != null) {
                detailCache[threadId] = detail

                // 原子写入本地磁盘永久保存
                saveArticleDetailToDisk(threadId, body)

                Result.success(detail)
            } else {
                Result.failure(Exception("未能解析到文章内容"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 将文章详情持久化到磁盘
     */
    private fun saveArticleDetailToDisk(threadId: Long, jsonContent: String) {
        val dir = detailsDir ?: return
        try {
            val target = File(dir, "$threadId.json")
            val tmp = File(dir, "$threadId.json.tmp")
            FileOutputStream(tmp).use { fos ->
                fos.write(jsonContent.toByteArray(Charsets.UTF_8))
                fos.flush()
            }
            tmp.renameTo(target)
        } catch (e: Exception) {
            Log.w("TacticalNewsRepo", "Failed to save article detail to disk: ${e.message}")
        }
    }

    /**
     * 后台静默预加载最新几篇资讯详情
     */
    private fun preloadTopArticles(items: List<TacticalNewsItem>) {
        CoroutineScope(Dispatchers.IO).launch {
            for (item in items) {
                val detailFile = detailsDir?.let { File(it, "${item.threadId}.json") }
                if (detailFile == null || !detailFile.exists()) {
                    fetchArticleDetail(item.threadId)
                }
            }
        }
    }

    /**
     * 解析资讯列表 JSON
     */
    fun parseArticlesJson(jsonStr: String): List<TacticalNewsItem> {
        val list = mutableListOf<TacticalNewsItem>()
        val root = JSONObject(jsonStr)
        if (!root.optBoolean("success", true)) return list

        val dataObj = root.optJSONObject("data") ?: return list
        val itemsArray = dataObj.optJSONArray("items") ?: return list

        for (i in 0 until itemsArray.length()) {
            val itemObj = itemsArray.getJSONObject(i)
            val threadId = itemObj.optLong("threadID", 0L)
            val dataId = itemObj.optString("dataID", "")
            val title = itemObj.optString("title", "").trim()
            val rawCover = itemObj.optString("cover", "")
            val author = itemObj.optString("author", "三角洲行动")
            val avatar = itemObj.optString("avatar", "")
            val createdAt = itemObj.optString("createdAt", "")
            val viewCount = itemObj.optInt("viewCount", 0)
            val likedCount = itemObj.optInt("likedCount", 0)

            if (threadId != 0L && title.isNotBlank()) {
                list.add(
                    TacticalNewsItem(
                        threadId = threadId,
                        dataId = dataId,
                        title = title,
                        coverUrl = normalizeUrl(rawCover),
                        author = author,
                        avatarUrl = normalizeUrl(avatar),
                        createdAt = createdAt,
                        viewCount = viewCount,
                        likedCount = likedCount
                    )
                )
            }
        }
        return list
    }

    /**
     * 解析资讯详情 JSON
     */
    fun parseArticleDetailJson(threadId: Long, jsonStr: String): TacticalNewsDetail? {
        return try {
            val root = JSONObject(jsonStr)
            if (!root.optBoolean("success", true)) return null

            val dataObj = root.optJSONObject("data") ?: return null
            val title = dataObj.optString("title", "资讯详情")
            val contentHtml = dataObj.optString("contentHtml", "")

            val authorObj = dataObj.optJSONObject("author")
            val authorName = authorObj?.optString("nickname", "三角洲行动") ?: "三角洲行动"
            val authorAvatar = normalizeUrl(authorObj?.optString("avatar", "") ?: "")
            val isOfficial = authorObj?.optBoolean("isOfficial", false) ?: false

            TacticalNewsDetail(
                threadId = threadId,
                title = title,
                authorName = authorName,
                authorAvatar = authorAvatar,
                isOfficial = isOfficial,
                contentHtml = contentHtml
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取离线兜底资讯种子（同步自 shushu.fan 官方数据源）
     */
    fun getFallbackNewsList(): List<TacticalNewsItem> {
        return listOf(
            TacticalNewsItem(
                threadId = 18863L,
                dataId = "11675625343806188297",
                title = "9月22日更新公告 | 洲年庆典开启！",
                coverUrl = "https://static.gametalk.qq.com/image/423/1789984166_2a6e81d564d46fb939b1dd92a63df819.png",
                author = "三角洲行动",
                avatarUrl = "https://static.gametalk.qq.com/image/423/1787644740_9ac4caee349c21e8e6591155b20d4960.jpg",
                createdAt = "2026-09-21 17:50:50",
                viewCount = 57607,
                likedCount = 490
            ),
            TacticalNewsItem(
                threadId = 18785L,
                dataId = "733401632717104567",
                title = "9月17日更新公告丨洲年空投活动开启＆干员平衡性调整！",
                coverUrl = "https://static.gametalk.qq.com/image/423/1789552579_f08532a6bbd2eda610bf3709fc15eff9.png",
                author = "三角洲行动",
                avatarUrl = "https://static.gametalk.qq.com/image/423/1787644740_9ac4caee349c21e8e6591155b20d4960.jpg",
                createdAt = "2026-09-16 17:58:31",
                viewCount = 193545,
                likedCount = 1294
            ),
            TacticalNewsItem(
                threadId = 18736L,
                dataId = "5291885084737396274",
                title = "我来同你玩丨二洲年快乐",
                coverUrl = "https://static.gametalk.qq.com/image/423/1789020363_4a47a0db6e60853dedfcfdf08a5ca249.png",
                author = "三角洲行动",
                avatarUrl = "https://static.gametalk.qq.com/image/423/1787644740_9ac4caee349c21e8e6591155b20d4960.jpg",
                createdAt = "2026-09-10 14:08:27",
                viewCount = 445599,
                likedCount = 15961
            ),
            TacticalNewsItem(
                threadId = 18735L,
                dataId = "993969890443046084",
                title = "9月10日更新公告丨彦祖回归＆咱俩练练玩法上线！",
                coverUrl = "https://static.gametalk.qq.com/image/423/1788945423_2a6e81d564d46fb939b1dd92a63df819.png",
                author = "三角洲行动",
                avatarUrl = "https://static.gametalk.qq.com/image/423/1787644740_9ac4caee349c21e8e6591155b20d4960.jpg",
                createdAt = "2026-09-09 18:00:00",
                viewCount = 167868,
                likedCount = 1649
            ),
            TacticalNewsItem(
                threadId = 18732L,
                dataId = "2174838248350224322",
                title = "9月4日更新公告丨新赛季【群星】即将开启！",
                coverUrl = "https://static.gametalk.qq.com/image/423/1788322302_092186185d99604c4bde6408118c2685.png",
                author = "三角洲行动",
                avatarUrl = "https://static.gametalk.qq.com/image/423/1787644740_9ac4caee349c21e8e6591155b20d4960.jpg",
                createdAt = "2026-09-02 12:12:14",
                viewCount = 380839,
                likedCount = 2947
            )
        )
    }
}
