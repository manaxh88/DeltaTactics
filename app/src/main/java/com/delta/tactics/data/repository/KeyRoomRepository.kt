package com.delta.tactics.data.repository

import android.content.Context
import com.delta.tactics.domain.model.KeyRoomCardItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

class KeyRoomRepository(private val context: Context) {

    private val prefs by lazy {
        context.getSharedPreferences("key_rooms_cache", Context.MODE_PRIVATE)
    }

    private var cachedKeys: List<KeyRoomCardItem>? = null
    private var lastFetchTime: Long = 0L

    companion object {
        private const val CACHE_DURATION_MS = 60 * 60 * 1000L // 1 小时缓存有效期
        private const val KEY_CACHE_DATA = "cached_keys_json"
        private const val KEY_CACHE_TIME = "cached_keys_timestamp"
    }

    /**
     * 获取全量或筛选后的钥匙房数据 (带 1 小时本地/内存缓存)
     */
    fun getKeyRooms(mapId: Int = 0, query: String = ""): List<KeyRoomCardItem> {
        val all = getAllKeys()
        return all.filter { item ->
            val matchMap = (mapId == 0 || item.mapId == mapId)
            val matchQuery = if (query.isBlank()) true else {
                item.name.contains(query, ignoreCase = true) ||
                        item.mapName.contains(query, ignoreCase = true) ||
                        item.lootDesc.contains(query, ignoreCase = true)
            }
            matchMap && matchQuery
        }
    }

    /**
     * 获取所有钥匙房卡列表（优先内存 -> 本地持久化缓存 -> Assets 离线种子包）
     */
    fun getAllKeys(): List<KeyRoomCardItem> {
        cachedKeys?.let { return it }

        // 1. 尝试从 SharedPreferences 读取
        val cachedJson = prefs.getString(KEY_CACHE_DATA, null)
        val cacheTime = prefs.getLong(KEY_CACHE_TIME, 0L)
        if (!cachedJson.isNullOrEmpty()) {
            val parsed = parseKeysJson(cachedJson)
            if (parsed.isNotEmpty()) {
                cachedKeys = parsed
                lastFetchTime = cacheTime
                return parsed
            }
        }

        // 2. 从 assets/key_rooms.json 加载离线种子包
        val fromAssets = loadFromAssets()
        cachedKeys = fromAssets
        return fromAssets
    }

    /**
     * 在线同步最新行情钥匙市价 (支持 1 小时过期检查与强制刷新)
     */
    suspend fun syncKeyRoomsFromWeb(force: Boolean = false): Result<List<KeyRoomCardItem>> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val savedTime = prefs.getLong(KEY_CACHE_TIME, 0L)

        if (!force && (now - savedTime) < CACHE_DURATION_MS && cachedKeys != null) {
            return@withContext Result.success(cachedKeys!!)
        }

        try {
            val fetchedList = mutableListOf<KeyRoomCardItem>()
            // 抓取前几页高价值钥匙最新市价
            val endpoint = "https://orzice.com/v/keys?top=2-2&p=1"
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            }

            if (conn.responseCode == 200) {
                val html = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val rowPattern = Pattern.compile("<tr>(.*?)</tr>", Pattern.DOTALL)
                val matcher = rowPattern.matcher(html)

                val namePattern = Pattern.compile("class=\"ui-tname\">([^<]+)</div>")
                val imgPattern = Pattern.compile("src=\"([^\"]+)\"")
                val gradePattern = Pattern.compile("data-grade=\"([^\"]+)\"")
                val idPattern = Pattern.compile("/v/info/(\\d+)")
                val tdPattern = Pattern.compile("<td[^>]*>(.*?)</td>", Pattern.DOTALL)

                var isHeader = true
                while (matcher.find()) {
                    if (isHeader) {
                        isHeader = false
                        continue
                    }
                    val rowHtml = matcher.group(1) ?: continue
                    val tdMatcher = tdPattern.matcher(rowHtml)
                    val cols = mutableListOf<String>()
                    while (tdMatcher.find()) {
                        cols.add(tdMatcher.group(1) ?: "")
                    }

                    if (cols.size >= 4) {
                        val nameM = namePattern.matcher(cols[0])
                        val imgM = imgPattern.matcher(cols[0])
                        val gradeM = gradePattern.matcher(cols[0])
                        val idM = idPattern.matcher(cols[0])

                        if (nameM.find()) {
                            val name = nameM.group(1)?.trim() ?: ""
                            val img = if (imgM.find()) imgM.group(1) ?: "" else ""
                            val grade = if (gradeM.find()) gradeM.group(1)?.toIntOrNull() ?: 5 else 5
                            val id = if (idM.find()) idM.group(1) ?: "" else ""
                            val priceStr = cols[2].replace(Regex("<[^>]+>"), "").replace(",", "").trim()
                            val price = priceStr.toLongOrNull() ?: 0L
                            val changeToday = cols[3].replace(Regex("<[^>]+>"), "").trim()

                            val (mapId, mapName) = guessMapFromNameOrImg(name, img)

                            fetchedList.add(
                                KeyRoomCardItem(
                                    id = id,
                                    name = name,
                                    mapId = mapId,
                                    mapName = mapName,
                                    imgUrl = img,
                                    grade = grade,
                                    price = price,
                                    changeToday = changeToday,
                                    lootDesc = getLootDescForRoom(name, mapName)
                                )
                            )
                        }
                    }
                }

                if (fetchedList.isNotEmpty()) {
                    // 与本地基础数据融合，保留全部 80 把并更新有最新市价的钥匙
                    val currentAll = getAllKeys().toMutableList()
                    val mapByName = currentAll.associateBy { it.name }.toMutableMap()
                    for (item in fetchedList) {
                        mapByName[item.name] = item
                    }
                    val merged = mapByName.values.sortedByDescending { it.price }
                    saveToCache(merged)
                    cachedKeys = merged
                    lastFetchTime = now
                    return@withContext Result.success(merged)
                }
            }
            Result.success(getAllKeys())
        } catch (e: Exception) {
            e.printStackTrace()
            Result.success(getAllKeys())
        }
    }

    private fun guessMapFromNameOrImg(name: String, img: String): Pair<Int, String> {
        return when {
            img.contains("%E9%9B%B6%E5%8F%B7%E5%A4%A7%E5%9D%9D") || name.contains("大坝") || name.contains("水泥厂") || name.contains("变电站") -> 1 to "零号大坝"
            img.contains("%E8%88%AA%E5%A4%A9%E5%9F%BA%E5%9C%B0") || name.contains("航天") || name.contains("离心机") || name.contains("总裁") -> 2 to "航天基地"
            img.contains("%E9%95%BF%E5%BC%93%E6%BA%AA%E8%B0%B7") || name.contains("长弓") || name.contains("溪谷") || name.contains("雷达") || name.contains("酒店") -> 3 to "长弓溪谷"
            img.contains("%E5%B7%B4%E5%85%8B%E4%BB%80") || name.contains("巴克什") || name.contains("浴场") || name.contains("集市") -> 4 to "巴克什"
            img.contains("150505") || name.contains("监狱") || name.contains("审讯") || name.contains("典狱长") -> 5 to "潮汐监狱"
            img.contains("150506") || name.contains("反应堆") || name.contains("核电") || name.contains("AZ3") -> 6 to "AZ3核电站"
            else -> 1 to "零号大坝"
        }
    }

    private fun getLootDescForRoom(name: String, mapName: String): String {
        return when {
            name.contains("总控") || name.contains("核心") -> "核心产出: 反应堆密匙、曼德尔砖、绝密军工芯片"
            name.contains("典狱长") || name.contains("收藏") -> "核心产出: 绝密蓝图、机密文档、军工大金条"
            name.contains("总裁") || name.contains("蓝室") -> "核心产出: 绝密航天数据、红卡、曼德尔砖"
            name.contains("国王") || name.contains("王子") -> "核心产出: 军用热成像仪、高级电子终端、大金条"
            name.contains("雷达") || name.contains("数据") -> "核心产出: CPU芯片、高级战术装备、保密机箱"
            name.contains("金库") || name.contains("贵宾") -> "核心产出: 黄金金条、高价值珠宝、机密密函"
            else -> "核心产出: 高级保险箱、军工电脑箱、战术武器箱"
        }
    }

    private fun loadFromAssets(): List<KeyRoomCardItem> {
        return try {
            val content = context.assets.open("key_rooms.json").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, "UTF-8")).readText()
            }
            parseKeysJson(content)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseKeysJson(jsonStr: String): List<KeyRoomCardItem> {
        val list = mutableListOf<KeyRoomCardItem>()
        try {
            val root = JSONObject(jsonStr)
            val array = root.optJSONArray("keys") ?: return emptyList()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val name = obj.optString("name", "")
                val mapName = obj.optString("mapName", "未知地图")
                list.add(
                    KeyRoomCardItem(
                        id = obj.optString("id", "k_$i"),
                        name = name,
                        mapId = obj.optInt("mapId", 1),
                        mapName = mapName,
                        imgUrl = obj.optString("img", ""),
                        grade = obj.optInt("grade", 5),
                        price = obj.optLong("price", 0L),
                        changeToday = obj.optString("changeToday", "0%"),
                        lootDesc = getLootDescForRoom(name, mapName)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun saveToCache(list: List<KeyRoomCardItem>) {
        try {
            val jsonArray = JSONArray()
            list.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("mapId", item.mapId)
                    put("mapName", item.mapName)
                    put("img", item.imgUrl)
                    put("grade", item.grade)
                    put("price", item.price)
                    put("changeToday", item.changeToday)
                }
                jsonArray.put(obj)
            }
            val root = JSONObject().apply {
                put("updateTime", System.currentTimeMillis())
                put("keys", jsonArray)
            }
            prefs.edit()
                .putString(KEY_CACHE_DATA, root.toString())
                .putLong(KEY_CACHE_TIME, System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
