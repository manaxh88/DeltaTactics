package com.delta.tactics.data.repository

import android.content.Context
import com.delta.tactics.domain.model.CipherRoom
import com.delta.tactics.domain.model.DailyMapPassword
import com.delta.tactics.domain.model.TacticalMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CipherRoomRepository(private val context: Context? = null) {

    private val prefs by lazy {
        context?.getSharedPreferences("daily_passwords_cache", Context.MODE_PRIVATE)
    }

    /**
     * 获取当前手机系统本地日期字符串 (yyyy-MM-dd)
     */
    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    /**
     * 读取本地缓存。当且仅当缓存存在、日期与当前手机系统日期一致且非空时返回。
     * 若未存储或已跨天，返回 null。
     */
    fun loadCachedPasswordsIfValid(): List<DailyMapPassword>? {
        val sp = prefs ?: return null
        val cachedDate = sp.getString("cached_date", null) ?: return null
        val today = getTodayDateString()
        if (cachedDate != today) {
            return null // 跨天失效，需要重新从网络拉取
        }
        val jsonStr = sp.getString("cached_passwords_json", null) ?: return null
        return parsePasswordsFromJson(jsonStr)
    }

    /**
     * 保存每日密码到本地持久化缓存并记录当前手机系统日期
     */
    fun saveDailyPasswordsToCache(list: List<DailyMapPassword>) {
        if (list.isEmpty()) return
        val sp = prefs ?: return
        try {
            val jsonArray = JSONArray()
            list.forEach { item ->
                val obj = JSONObject().apply {
                    put("mapId", item.mapId)
                    put("mapName", item.mapName)
                    put("code", item.code)
                    put("locationDesc", item.locationDesc)
                    put("refreshTime", item.refreshTime)
                }
                jsonArray.put(obj)
            }
            sp.edit()
                .putString("cached_date", getTodayDateString())
                .putString("cached_passwords_json", jsonArray.toString())
                .apply()

            context?.let { ctx ->
                com.delta.tactics.presentation.widget.DailyPasswordWidgetProvider.updateAllWidgets(ctx)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parsePasswordsFromJson(jsonStr: String): List<DailyMapPassword>? {
        return try {
            val jsonArray = JSONArray(jsonStr)
            val result = mutableListOf<DailyMapPassword>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                result.add(
                    DailyMapPassword(
                        mapId = obj.optString("mapId"),
                        mapName = obj.optString("mapName"),
                        code = obj.optString("code"),
                        locationDesc = obj.optString("locationDesc"),
                        refreshTime = obj.optString("refreshTime", "今日 00:00 实时同步")
                    )
                )
            }
            if (result.isNotEmpty()) result else null
        } catch (e: Exception) {
            null
        }
    }

    // 摩斯电码标准字典（对照鼠鼠工具 /fun/morseCode 破译标准）
    val morseCodeDict = mapOf(
        '0' to "-----",
        '1' to ".----",
        '2' to "..---",
        '3' to "...--",
        '4' to "....-",
        '5' to ".....",
        '6' to "-....",
        '7' to "--...",
        '8' to "---..",
        '9' to "----."
    )

    // 默认种子数据
    private val defaultSeedPasswords = listOf(
        DailyMapPassword(
            mapId = "zero_dam",
            mapName = "零号大坝",
            code = "1392",
            locationDesc = "水泥厂大仓二楼 / 变电站地下每日轮换门"
        ),
        DailyMapPassword(
            mapId = "longbow_valley",
            mapName = "长弓溪谷",
            code = "5097",
            locationDesc = "储油站二楼主控 / 皇后酒店每日轮换门"
        ),
        DailyMapPassword(
            mapId = "barkash",
            mapName = "巴克什",
            code = "3144",
            locationDesc = "皇家浴场水池北侧每日轮换门"
        ),
        DailyMapPassword(
            mapId = "space_city",
            mapName = "航天基地",
            code = "9646",
            locationDesc = "研发无尘核心净化区每日轮换门"
        ),
        DailyMapPassword(
            mapId = "tide_prison",
            mapName = "潮汐监狱",
            code = "4885",
            locationDesc = "重刑监区回廊典狱长每日轮换门"
        ),
        DailyMapPassword(
            mapId = "az3_nuclear",
            mapName = "AZ3",
            code = "2525",
            locationDesc = "反应堆冷却泵房应急每日轮换门"
        )
    )

    // 6 大核心地图每日专属单一密码（优先从当天本地有效缓存恢复，无缓存时使用默认种子）
    private val _dailyPasswords = MutableStateFlow(
        loadCachedPasswordsIfValid() ?: defaultSeedPasswords
    )
    val dailyPasswords: Flow<List<DailyMapPassword>> = _dailyPasswords.asStateFlow()

    fun getDefaultDailyPasswords(): List<DailyMapPassword> = _dailyPasswords.value

    // 详细房间列表
    private val cipherRooms = MutableStateFlow(
        listOf(
            CipherRoom(
                id = "zd_daily_main",
                map = TacticalMap.ZERO_DAM,
                roomName = "零号大坝 • 每日密码门",
                code = "1392",
                isDailyDynamic = true,
                dynamicHint = "今日 00:00 已刷新",
                locationDesc = "水泥厂生产车间二楼钢架大仓尽头密码门",
                lootHighlights = listOf("高阶保密箱", "大型武器箱", "医疗包"),
                tacticalTips = "过钢架走廊注意外围狙击，开门前由队友架枪掩护",
                dangerLevel = 4
            ),
            CipherRoom(
                id = "lv_daily_main",
                map = TacticalMap.LONGBOW_VALLEY,
                roomName = "长弓溪谷 • 每日密码门",
                code = "5097",
                isDailyDynamic = true,
                dynamicHint = "今日 00:00 已刷新",
                locationDesc = "储油站主罐区北侧红砖楼二层控制台后方",
                lootHighlights = listOf("重型武器箱", "保密柜", "高级燃料"),
                tacticalTips = "先清除油站巡逻首领与守卫再输入密码",
                dangerLevel = 5
            ),
            CipherRoom(
                id = "bk_daily_main",
                map = TacticalMap.BARKASH,
                roomName = "巴克什 • 每日密码门",
                code = "3144",
                isDailyDynamic = true,
                dynamicHint = "今日 00:00 已刷新",
                locationDesc = "皇家浴场主水池北侧暗道，推开挂毯后密码锁",
                lootHighlights = listOf("黄金首饰", "机密保密箱", "战术背心"),
                tacticalTips = "水声会掩盖脚步声，佩戴降噪耳麦进入",
                dangerLevel = 4
            ),
            CipherRoom(
                id = "sc_daily_main",
                map = TacticalMap.SPACE_CITY,
                roomName = "航天基地 • 每日密码门",
                code = "9646",
                isDailyDynamic = true,
                dynamicHint = "今日 00:00 已刷新",
                locationDesc = "科研楼 3 楼无尘洁净室通道尽头气密密码门",
                lootHighlights = listOf("核心数据芯片", "金卡高爆点", "电子保险箱"),
                tacticalTips = "全图最高危险交火区，推进前先投掷烟闪",
                dangerLevel = 5
            ),
            CipherRoom(
                id = "tp_daily_main",
                map = TacticalMap.TIDE_PRISON,
                roomName = "潮汐监狱 • 每日密码门",
                code = "4885",
                isDailyDynamic = true,
                dynamicHint = "今日 00:00 已刷新",
                locationDesc = "重刑犯监区二楼回廊东侧尽头控制室铁门",
                lootHighlights = listOf("特勤机密箱", "战术武器箱", "全效针剂"),
                tacticalTips = "回廊易被包夹，封烟快速破门搜刮",
                dangerLevel = 5
            ),
            CipherRoom(
                id = "az_daily_main",
                map = TacticalMap.AZ3_NUCLEAR,
                roomName = "AZ3 • 每日密码门",
                code = "2525",
                isDailyDynamic = true,
                dynamicHint = "今日 00:00 已刷新",
                locationDesc = "反应堆环形走廊黄色高危区泵房防爆门",
                lootHighlights = listOf("防辐射特种装备", "能源芯片", "密码机箱"),
                tacticalTips = "注意辐射剂量监控，速拿速离",
                dangerLevel = 5
            )
        )
    )

    fun getCipherRooms(mapFilter: TacticalMap, searchQuery: String): Flow<List<CipherRoom>> {
        return cipherRooms.map { list ->
            list.filter { room ->
                val matchesMap = (mapFilter == TacticalMap.ALL || room.map == mapFilter)
                val matchesQuery = if (searchQuery.isBlank()) {
                    true
                } else {
                    room.roomName.contains(searchQuery, ignoreCase = true) ||
                    room.map.displayName.contains(searchQuery, ignoreCase = true) ||
                    room.code.contains(searchQuery, ignoreCase = true) ||
                    room.locationDesc.contains(searchQuery, ignoreCase = true) ||
                    room.lootHighlights.any { it.contains(searchQuery, ignoreCase = true) }
                }
                matchesMap && matchesQuery
            }
        }
    }

    fun getAllRoomsSync(): List<CipherRoom> {
        return cipherRooms.value
    }

    /**
     * 在线同步三角洲鼠鼠工具 (shushu.fan) 首页实时 4 位密码
     * @param force 为 false 时优先检查本地缓存是否为当天，若当天已缓存则直接返回本地数据，零网络请求；
     *              为 true 时强制向网络发起请求刷新，成功后更新本地缓存和日期。
     */
    suspend fun syncDailyPasswordsFromWeb(force: Boolean = false): Result<List<DailyMapPassword>> = withContext(Dispatchers.IO) {
        // 1. 若非强制刷新，优先检查本地缓存是否属于当天
        if (!force) {
            val cached = loadCachedPasswordsIfValid()
            if (cached != null && cached.isNotEmpty()) {
                _dailyPasswords.value = cached
                return@withContext Result.success(cached)
            }
        }

        // 2. 本地缓存不存在、已跨天或用户手动强制刷新，发起网络请求
        try {
            val url = URL("https://www.shushu.fan")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                connectTimeout = 6000
                readTimeout = 6000
            }

            if (conn.responseCode == 200) {
                val html = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val regex = Regex("""text-shadow:[^>]*>([^<]+)</span>\s*<span[^>]*text-shadow:[^>]*>(\d{4})</span>""")
                val matches = regex.findAll(html)

                val mapCodeList = mutableListOf<DailyMapPassword>()
                val seenMaps = mutableSetOf<String>()

                for (m in matches) {
                    val mapName = m.groupValues[1].trim()
                    val code = m.groupValues[2].trim()
                    if (seenMaps.add(mapName)) {
                        val mapId = when {
                            mapName.contains("大坝") -> "zero_dam"
                            mapName.contains("溪谷") -> "longbow_valley"
                            mapName.contains("巴克什") -> "barkash"
                            mapName.contains("航天") -> "space_city"
                            mapName.contains("监狱") -> "tide_prison"
                            mapName.contains("AZ3") || mapName.contains("核电") -> "az3_nuclear"
                            else -> mapName
                        }
                        val desc = when (mapId) {
                            "zero_dam" -> "水泥厂大仓二楼 / 变电站地下每日轮换门"
                            "longbow_valley" -> "储油站二楼主控 / 皇后酒店每日轮换门"
                            "barkash" -> "皇家浴场水池北侧每日轮换门"
                            "space_city" -> "研发无尘核心净化区每日轮换门"
                            "tide_prison" -> "重刑监区回廊典狱长每日轮换门"
                            "az3_nuclear" -> "反应堆冷却泵房应急每日轮换门"
                            else -> "每日轮换密码门"
                        }
                        mapCodeList.add(
                            DailyMapPassword(
                                mapId = mapId,
                                mapName = mapName,
                                code = code,
                                locationDesc = desc,
                                refreshTime = "今日 00:00 实时同步"
                            )
                        )
                    }
                }

                if (mapCodeList.isNotEmpty()) {
                    _dailyPasswords.value = mapCodeList
                    // 关键：将抓取到的最新密码持久化存入本地，并记录手机当前日期
                    saveDailyPasswordsToCache(mapCodeList)
                    return@withContext Result.success(mapCodeList)
                }
            }
            if (force) {
                Result.failure(Exception("网络响应异常 (HTTP ${conn.responseCode})"))
            } else {
                Result.success(_dailyPasswords.value)
            }
        } catch (e: Exception) {
            // 发生异常时: 若为强制刷新则返回失败通知UI，同时保留本地现有可用数据
            if (force) {
                Result.failure(e)
            } else {
                Result.success(_dailyPasswords.value)
            }
        }
    }
}
