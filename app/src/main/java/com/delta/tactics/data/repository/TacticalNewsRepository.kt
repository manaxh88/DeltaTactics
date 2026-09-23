package com.delta.tactics.data.repository

import com.delta.tactics.domain.model.TacticalNewsDetail
import com.delta.tactics.domain.model.TacticalNewsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class TacticalNewsRepository {

    private val baseUrl = "https://www.shushu.fan/api/articles"
    private val detailCache = mutableMapOf<Long, TacticalNewsDetail>()
    private var cachedNewsList: List<TacticalNewsItem>? = null
    private var lastFetchTime: Long = 0L
    private val cacheDurationMs = 60 * 60 * 1000L // 1 小时

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
     * 分页拉取战术资讯列表 (支持 1 小时缓存与跨天自动刷新)
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
                // 如果是第一页且有本地缓存，则优雅降级返回缓存/兜底数据
                if (page == 1) {
                    val fallback = cachedNewsList ?: getFallbackNewsList()
                    return@withContext Result.success(fallback)
                }
                return@withContext Result.failure(Exception("HTTP ${conn.responseCode}"))
            }

            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val parsedList = parseArticlesJson(body)

            if (page == 1) {
                cachedNewsList = parsedList
                lastFetchTime = System.currentTimeMillis()
            }

            Result.success(parsedList)
        } catch (e: Exception) {
            // 网络异常时，如果是第一页直接返回缓存或离线种子数据
            if (page == 1) {
                val fallback = cachedNewsList ?: getFallbackNewsList()
                return@withContext Result.success(fallback)
            }
            Result.failure(e)
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
     * 拉取指定资讯的文章详情
     */
    suspend fun fetchArticleDetail(threadId: Long): Result<TacticalNewsDetail> = withContext(Dispatchers.IO) {
        detailCache[threadId]?.let { return@withContext Result.success(it) }

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
                Result.success(detail)
            } else {
                Result.failure(Exception("未能解析到文章内容"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
