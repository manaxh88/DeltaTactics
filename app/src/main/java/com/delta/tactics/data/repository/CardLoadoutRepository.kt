package com.delta.tactics.data.repository

import android.content.Context
import com.delta.tactics.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class CardLoadoutRepository(private val context: Context) {

    private val prefs by lazy {
        context.getSharedPreferences("card_loadout_cache", Context.MODE_PRIVATE)
    }

    private var cachedData: CardLoadoutData? = null
    private var lastFetchTime: Long = 0L

    companion object {
        private const val CACHE_DURATION_MS = 60 * 60 * 1000L // 1 小时自动失效
        private const val KEY_CACHE_JSON = "cached_kzb_json"
        private const val KEY_CACHE_TIME = "cached_kzb_time"
    }

    /**
     * 同步获取卡战备数据（优先内存 -> 本地持久化缓存 -> Assets）
     */
    fun getCardLoadoutData(): CardLoadoutData {
        cachedData?.let { return it }

        // 1. 尝试读取本地持久化缓存
        val savedJson = prefs.getString(KEY_CACHE_JSON, null)
        val savedTime = prefs.getLong(KEY_CACHE_TIME, 0L)
        if (!savedJson.isNullOrEmpty()) {
            val parsed = parseCardLoadoutJson(savedJson)
            if (parsed.tiers.isNotEmpty()) {
                cachedData = parsed
                lastFetchTime = savedTime
                return parsed
            }
        }

        // 2. 降级读取本地 Assets 种子
        val parsed = loadFromAssets()
        cachedData = parsed
        return parsed
    }

    /**
     * 判断当前缓存是否已过期（超过1小时或未曾加载过）
     */
    fun isCacheExpired(): Boolean {
        val savedTime = prefs.getLong(KEY_CACHE_TIME, 0L)
        return (System.currentTimeMillis() - savedTime) >= CACHE_DURATION_MS
    }

    /**
     * 在线异步同步最新卡战备方案 (自动根据 1 小时策略或强制刷新)
     */
    suspend fun fetchCardLoadoutData(force: Boolean = false): Result<CardLoadoutData> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val savedTime = prefs.getLong(KEY_CACHE_TIME, 0L)

        // 若非强制刷新且缓存未过期，直接返回本地缓存
        if (!force && (now - savedTime) < CACHE_DURATION_MS && cachedData != null && cachedData!!.tiers.isNotEmpty()) {
            return@withContext Result.success(cachedData!!)
        }

        try {
            val url = URL("https://www.shushu.fan/kzb")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                setRequestProperty("Referer", "https://www.shushu.fan/")
            }

            if (conn.responseCode == 200) {
                val html = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val parsed = parseKzbHtml(html)
                if (parsed != null && parsed.tiers.isNotEmpty()) {
                    // 持久化到本地缓存
                    saveToCache(parsed)
                    cachedData = parsed
                    lastFetchTime = now
                    return@withContext Result.success(parsed)
                }
            }
            if (force) {
                Result.failure(Exception("Failed to fetch or parse card loadout data: HTTP ${conn.responseCode}"))
            } else {
                Result.success(getCardLoadoutData())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (force) {
                Result.failure(e)
            } else {
                Result.success(getCardLoadoutData())
            }
        }
    }

    /**
     * 解析 shushu.fan/kzb 服务端流数据
     */
    private fun parseKzbHtml(html: String): CardLoadoutData? {
        return try {
            val marker = "{\\\"allData\\\":"
            val startIdx = html.indexOf(marker)
            if (startIdx == -1) return null

            var endIdx = -1
            val timeIdx = html.indexOf("\\\"time\\\":", startIdx)
            if (timeIdx != -1) {
                val closeBrace = html.indexOf("}", timeIdx)
                if (closeBrace != -1) {
                    endIdx = closeBrace
                }
            }
            if (endIdx == -1) {
                val endPattern = "}]\\n\"]"
                val pIdx = html.indexOf(endPattern, startIdx)
                if (pIdx != -1) {
                    endIdx = pIdx
                }
            }
            if (endIdx == -1) {
                // Bracket matching as fallback
                var depth = 0
                for (i in startIdx until html.length) {
                    val c = html[i]
                    if (c == '{') depth++
                    else if (c == '}') {
                        depth--
                        if (depth == 0) {
                            endIdx = i
                            break
                        }
                    }
                }
            }
            if (endIdx == -1) return null

            val rawSub = html.substring(startIdx, endIdx + 1)
            val unescaped = rawSub.replace("\\\"", "\"").replace("\\\\", "\\")

            val json = JSONObject(unescaped)
            val updateTime = json.optString("time", "")
            val allDataArray = json.optJSONArray("allData") ?: return null

            val tierConfigs = listOf(
                Triple("11W", "大坝、长弓—机密", 110000L),
                Triple("18W", "航天、巴克什—机密", 180000L),
                Triple("55W", "巴克什—绝密", 550000L),
                Triple("60W", "航天基地—绝密", 600000L),
                Triple("78W", "潮汐监狱—绝密", 780000L)
            )

            val tiersList = mutableListOf<CardLoadoutTier>()

            for (t in 0 until minOf(allDataArray.length(), tierConfigs.size)) {
                val plansArray = allDataArray.getJSONArray(t)
                val config = tierConfigs[t]
                val plansList = mutableListOf<CardLoadoutPlan>()

                for (p in 0 until plansArray.length()) {
                    val planJson = plansArray.getJSONObject(p)
                    val planName = planJson.optString("name", "推荐方案")
                    val price = planJson.optLong("price", 0L)
                    val jz = planJson.optLong("jz", 0L)
                    val cz = planJson.optLong("cz", 0L)

                    val dataArray = planJson.optJSONArray("data") ?: JSONArray()
                    val itemsList = mutableListOf<CardLoadoutItem>()

                    for (d in 0 until dataArray.length()) {
                        val itemJson = dataArray.getJSONObject(d)
                        itemsList.add(
                            CardLoadoutItem(
                                id = itemJson.optLong("id", 0L),
                                name = itemJson.optString("name", ""),
                                grade = itemJson.optInt("grade", 1),
                                price = itemJson.optLong("price", 0L),
                                jz = itemJson.optLong("jz", 0L),
                                type = itemJson.optString("type", ""),
                                pic = itemJson.optString("pic", ""),
                                bl = itemJson.optInt("bl", 0),
                                jiazhang = itemJson.optInt("jiazhang", 0)
                            )
                        )
                    }

                    plansList.add(
                        CardLoadoutPlan(
                            name = planName,
                            price = price,
                            jz = jz,
                            cz = cz,
                            data = itemsList
                        )
                    )
                }

                tiersList.add(
                    CardLoadoutTier(
                        id = config.first,
                        name = config.first,
                        maps = config.second,
                        thresholdValue = config.third,
                        plans = plansList
                    )
                )
            }

            CardLoadoutData(
                updateTime = updateTime,
                tiers = tiersList
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun loadFromAssets(): CardLoadoutData {
        return try {
            val content = context.assets.open("card_loadout.json").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, "UTF-8")).readText()
            }
            parseCardLoadoutJson(content)
        } catch (e: Exception) {
            e.printStackTrace()
            CardLoadoutData()
        }
    }

    fun parseCardLoadoutJson(jsonString: String): CardLoadoutData {
        return try {
            val json = JSONObject(jsonString)
            val updateTime = json.optString("updateTime", "")
            val tiersArray = json.optJSONArray("tiers") ?: return CardLoadoutData()
            val tiersList = mutableListOf<CardLoadoutTier>()

            for (t in 0 until tiersArray.length()) {
                val tierJson = tiersArray.getJSONObject(t)
                val id = tierJson.optString("id", "")
                val name = tierJson.optString("name", "")
                val maps = tierJson.optString("maps", "")
                val thresholdValue = tierJson.optLong("thresholdValue", 0L)

                val plansArray = tierJson.optJSONArray("plans") ?: JSONArray()
                val plansList = mutableListOf<CardLoadoutPlan>()

                for (p in 0 until plansArray.length()) {
                    val planJson = plansArray.getJSONObject(p)
                    val planName = planJson.optString("name", "")
                    val price = planJson.optLong("price", 0L)
                    val jz = planJson.optLong("jz", 0L)
                    val cz = planJson.optLong("cz", 0L)

                    val dataArray = planJson.optJSONArray("data") ?: JSONArray()
                    val itemsList = mutableListOf<CardLoadoutItem>()

                    for (d in 0 until dataArray.length()) {
                        val itemJson = dataArray.getJSONObject(d)
                        itemsList.add(
                            CardLoadoutItem(
                                id = itemJson.optLong("id", 0L),
                                name = itemJson.optString("name", ""),
                                grade = itemJson.optInt("grade", 1),
                                price = itemJson.optLong("price", 0L),
                                jz = itemJson.optLong("jz", 0L),
                                type = itemJson.optString("type", ""),
                                pic = itemJson.optString("pic", ""),
                                bl = itemJson.optInt("bl", 0),
                                jiazhang = itemJson.optInt("jiazhang", 0)
                            )
                        )
                    }

                    plansList.add(
                        CardLoadoutPlan(
                            name = planName,
                            price = price,
                            jz = jz,
                            cz = cz,
                            data = itemsList
                        )
                    )
                }

                tiersList.add(
                    CardLoadoutTier(
                        id = id,
                        name = name,
                        maps = maps,
                        thresholdValue = thresholdValue,
                        plans = plansList
                    )
                )
            }

            CardLoadoutData(
                updateTime = updateTime,
                tiers = tiersList
            )
        } catch (e: Exception) {
            e.printStackTrace()
            CardLoadoutData()
        }
    }

    private fun saveToCache(data: CardLoadoutData) {
        try {
            val root = JSONObject()
            root.put("updateTime", data.updateTime)
            val tiersArray = JSONArray()

            for (t in data.tiers) {
                val tierObj = JSONObject()
                tierObj.put("id", t.id)
                tierObj.put("name", t.name)
                tierObj.put("maps", t.maps)
                tierObj.put("thresholdValue", t.thresholdValue)

                val plansArray = JSONArray()
                for (p in t.plans) {
                    val planObj = JSONObject()
                    planObj.put("name", p.name)
                    planObj.put("price", p.price)
                    planObj.put("jz", p.jz)
                    planObj.put("cz", p.cz)

                    val dataArray = JSONArray()
                    for (item in p.data) {
                        val itemObj = JSONObject()
                        itemObj.put("id", item.id)
                        itemObj.put("name", item.name)
                        itemObj.put("grade", item.grade)
                        itemObj.put("price", item.price)
                        itemObj.put("jz", item.jz)
                        itemObj.put("type", item.type)
                        itemObj.put("pic", item.pic)
                        itemObj.put("bl", item.bl)
                        itemObj.put("jiazhang", item.jiazhang)
                        dataArray.put(itemObj)
                    }
                    planObj.put("data", dataArray)
                    plansArray.put(planObj)
                }
                tierObj.put("plans", plansArray)
                tiersArray.put(tierObj)
            }
            root.put("tiers", tiersArray)

            prefs.edit()
                .putString(KEY_CACHE_JSON, root.toString())
                .putLong(KEY_CACHE_TIME, System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
