package com.delta.tactics.data.repository

import android.content.Context
import com.delta.tactics.domain.model.BulletPack
import com.delta.tactics.domain.model.BulletPackItem
import com.delta.tactics.domain.model.CraftBenchType
import com.delta.tactics.domain.model.CraftRecipe
import com.delta.tactics.domain.model.GunsmithBuildRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class ProfitRepository(private val context: Context) {

    private var cachedRecipes: List<CraftRecipe>? = null
    private var cachedBulletPacks: List<BulletPack>? = null

    companion object {
        private const val PREFS_NAME = "delta_profit_prefs"
        private const val KEY_CRAFT_JSON = "cached_craft_profit_json_v3"
        private const val KEY_CRAFT_TIME = "cached_craft_profit_time_v3"
        private const val KEY_BULLET_JSON = "cached_bullet_pack_json_v3"
        private const val KEY_BULLET_TIME = "cached_bullet_pack_time_v3"
        private const val CACHE_DURATION_MS = 2 * 60 * 60 * 1000L // 2 小时有效

        private const val CRAFT_LIVE_URL = "https://www.shushu.fan/chi/profitRank"
        private const val CRAFT_CDN_URL = "https://fastly.jsdelivr.net/gh/manaxh88/DeltaTactics@main/app/src/main/assets/craft_profit.json"
        private const val BULLET_CDN_URL = "https://fastly.jsdelivr.net/gh/manaxh88/DeltaTactics@main/app/src/main/assets/bullet_pack.json"
        private const val BULLET_MIRROR_URL = "https://ghfast.top/https://raw.githubusercontent.com/manaxh88/DeltaTactics/main/app/src/main/assets/bullet_pack.json"

        fun getAmmoImageUrl(name: String, caliber: String): String {
            val text = "$name $caliber".lowercase()
            val file = when {
                text.contains("300") && text.contains("blk") -> "300BLK.png"
                text.contains("5.56") -> "5.56x45mm.png"
                text.contains("7.62") && (text.contains("51") || text.contains("m80") || text.contains("m61") || text.contains("m62")) -> "7.62x51mm.png"
                text.contains("7.62") && (text.contains("39") || text.contains("bp") || text.contains("ps")) -> "7.62x39.png"
                text.contains("7.62") && (text.contains("54") || text.contains("lps") || text.contains("bt") || text.contains("t46")) -> "7.62x54mm.png"
                text.contains("9x19") || text.contains("9×19") -> "9x19.png"
                text.contains("4.6x30") || text.contains("4.6×30") -> "4.6x30.png"
                text.contains("5.8x42") || text.contains("5.8×42") -> "5.8x42.png"
                text.contains("5.7x28") || text.contains("5.7×28") -> "5.7x28.png"
                text.contains("5.45x39") || text.contains("5.45×39") -> "5.45x39.png"
                text.contains("45") && text.contains("acp") -> "45-ACP.png"
                text.contains("357") -> ".357.png"
                text.contains("12") && (text.contains("gauge") || text.contains("口径")) -> "12-Gauge.png"
                text.contains("9x39") || text.contains("9×39") -> "9x39.png"
                text.contains("12.7") -> "12.7x55.png"
                text.contains("6.8") -> "6.8x51.png"
                else -> ""
            }
            return if (file.isNotBlank()) "https://playerhub.df.qq.com/playerhub/60004/object/gun/ammo/$file" else ""
        }

        fun getAmmoGrade(name: String, caliber: String): Int {
            val text = "$name $caliber".lowercase()
            return when {
                text.contains("m61") || text.contains("m995") || text.contains("v-max") || (text.contains("bt") && text.contains("54r")) || text.contains("rip+穿甲") -> 5
                text.contains("m80") || text.contains("ap") || text.contains("a1") || text.contains("7n31") || text.contains("dvp03") || text.contains("bp") || text.contains("lps") -> 4
                else -> 3
            }
        }

        fun classifyBench(name: String, pic: String): Pair<CraftBenchType, String> {
            return when {
                pic.contains("/1105") || pic.contains("/1101") || pic.contains("/1107") || pic.contains("/1108") ||
                name.contains("甲") || name.contains("头盔") || name.contains("背心") || name.contains("胸挂") || name.contains("防弹") ->
                    CraftBenchType.ARMOR to "防具台"
                pic.contains("/gun/ammo") || name.contains("BLK") || name.contains("×") || name.contains("x") ||
                name.contains("弹") || name.contains("mm") || name.contains("Gauge") || name.contains("ACP") ->
                    CraftBenchType.AMMO to "弹药台"
                pic.contains("/1401") || pic.contains("/1402") || pic.contains("/1403") ||
                name.contains("药") || name.contains("注射") || name.contains("包") || name.contains("医") ||
                name.contains("痛") || name.contains("手术") || name.contains("急救") ->
                    CraftBenchType.MEDICAL to "医疗台"
                else ->
                    CraftBenchType.WEAPON to "枪械台"
            }
        }
    }

    /**
     * 获取制造配方 (内存 -> 磁盘持久化缓存 -> 内置 Assets)
     */
    fun getCraftRecipes(): List<CraftRecipe> {
        cachedRecipes?.let { return it }

        // 1. 尝试从 SharedPreferences 加载
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedJson = prefs.getString(KEY_CRAFT_JSON, null)
        if (!cachedJson.isNullOrBlank()) {
            val list = parseRecipesFromJson(cachedJson)
            val hasImages = list.isNotEmpty() && list.all { it.imageUrl.isNotBlank() }
            if (hasImages) {
                cachedRecipes = list
                return list
            }
        }

        // 2. 加载本地 assets
        val loaded = loadRecipesFromAssets()
        cachedRecipes = loaded
        return loaded
    }

    /**
     * 获取子弹自选包 (内存 -> 磁盘持久化缓存 -> 内置 Assets)
     */
    fun getBulletPacks(): List<BulletPack> {
        cachedBulletPacks?.let { return it }

        // 1. 尝试从 SharedPreferences 加载
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedJson = prefs.getString(KEY_BULLET_JSON, null)
        if (!cachedJson.isNullOrBlank()) {
            val list = parseBulletPacksFromJson(cachedJson)
            val hasImages = list.isNotEmpty() && list.all { it.bullets.all { b -> b.imageUrl.isNotBlank() } }
            if (hasImages) {
                cachedBulletPacks = list
                return list
            }
        }

        // 2. 加载本地 assets
        val loaded = loadBulletPacksFromAssets()
        cachedBulletPacks = loaded
        return loaded
    }

    /**
     * 获取制造物价更新时间描述
     */
    fun getCraftUpdateTime(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val time = prefs.getLong(KEY_CRAFT_TIME, 0L)
        return if (time > 0L) {
            val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            "更新于 ${sdf.format(Date(time))} · 实时物价"
        } else {
            "特勤处制造利润倒排"
        }
    }

    /**
     * 获取子弹物价更新时间描述
     */
    fun getBulletUpdateTime(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val time = prefs.getLong(KEY_BULLET_TIME, 0L)
        return if (time > 0L) {
            val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            "更新于 ${sdf.format(Date(time))} · 实时单价"
        } else {
            "拍卖行实时变现榜"
        }
    }

    /**
     * 云端联网更新制造利润排行
     */
    suspend fun fetchCraftRecipes(force: Boolean = false): List<CraftRecipe> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedTime = prefs.getLong(KEY_CRAFT_TIME, 0L)
        val now = System.currentTimeMillis()

        if (!force && (now - cachedTime < CACHE_DURATION_MS)) {
            val cachedJson = prefs.getString(KEY_CRAFT_JSON, null)
            if (!cachedJson.isNullOrBlank()) {
                val parsed = parseRecipesFromJson(cachedJson)
                if (parsed.isNotEmpty()) {
                    cachedRecipes = parsed
                    return@withContext parsed
                }
            }
        }

        // 1. 尝试从 shushu.fan 实时拉取最新物价与时薪
        try {
            val conn = (URL(CRAFT_LIVE_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            }
            if (conn.responseCode == 200) {
                val html = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val parsed = parseCraftFromHtml(html)
                if (!parsed.isNullOrEmpty()) {
                    saveRecipesToCache(parsed)
                    cachedRecipes = parsed
                    return@withContext parsed
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. 降级尝试 CDN / GitHub Raw
        try {
            val conn = (URL(CRAFT_CDN_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
            }
            if (conn.responseCode == 200) {
                val json = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val parsed = parseRecipesFromJson(json)
                if (parsed.isNotEmpty()) {
                    saveRecipesToCache(parsed)
                    cachedRecipes = parsed
                    return@withContext parsed
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. 网络皆不可用时回退现有数据
        getCraftRecipes()
    }

    /**
     * 云端联网更新子弹自选包收益
     */
    suspend fun fetchBulletPacks(force: Boolean = false): List<BulletPack> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedTime = prefs.getLong(KEY_BULLET_TIME, 0L)
        val now = System.currentTimeMillis()

        if (!force && (now - cachedTime < CACHE_DURATION_MS)) {
            val cachedJson = prefs.getString(KEY_BULLET_JSON, null)
            if (!cachedJson.isNullOrBlank()) {
                val parsed = parseBulletPacksFromJson(cachedJson)
                if (parsed.isNotEmpty()) {
                    cachedBulletPacks = parsed
                    return@withContext parsed
                }
            }
        }

        // 尝试从 CDN 与 GitHub 镜像拉取
        val endpoints = listOf(BULLET_CDN_URL, BULLET_MIRROR_URL)
        for (ep in endpoints) {
            try {
                val conn = (URL(ep).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6000
                    readTimeout = 6000
                    requestMethod = "GET"
                }
                if (conn.responseCode == 200) {
                    val json = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val parsed = parseBulletPacksFromJson(json)
                    if (parsed.isNotEmpty()) {
                        saveBulletPacksToCache(parsed)
                        cachedBulletPacks = parsed
                        return@withContext parsed
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 若网络失败，回退到本地
        val loaded = getBulletPacks()
        saveBulletPacksToCache(loaded)
        loaded
    }

    private fun saveRecipesToCache(list: List<CraftRecipe>) {
        try {
            val root = JSONObject()
            root.put("updateTime", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            val array = JSONArray()
            for (r in list) {
                val obj = JSONObject().apply {
                    put("id", r.id)
                    put("name", r.name)
                    put("benchType", r.benchType.name)
                    put("benchName", r.benchName)
                    put("durationHours", r.durationHours)
                    put("totalProfit", r.totalProfit)
                    put("hourlyProfit", r.hourlyProfit)
                    put("cost", r.cost)
                    put("revenue", r.revenue)
                    put("recommendedLevel", r.recommendedLevel)
                    put("imageUrl", r.imageUrl)
                    put("grade", r.grade)
                    val mats = JSONArray()
                    r.materials.forEach { mats.put(it) }
                    put("materials", mats)
                }
                array.put(obj)
            }
            root.put("recipes", array)

            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_CRAFT_JSON, root.toString())
                .putLong(KEY_CRAFT_TIME, System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveBulletPacksToCache(list: List<BulletPack>) {
        try {
            val root = JSONObject()
            root.put("updateTime", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            val array = JSONArray()
            for (p in list) {
                val pObj = JSONObject().apply {
                    put("id", p.id)
                    put("packName", p.packName)
                    put("recommendedBullet", p.recommendedBullet)
                    put("recommendedValue", p.recommendedValue)
                    put("imageUrl", p.imageUrl)
                    val bArr = JSONArray()
                    for (b in p.bullets) {
                        val bObj = JSONObject().apply {
                            put("rank", b.rank)
                            put("name", b.name)
                            put("caliber", b.caliber)
                            put("count", b.count)
                            put("unitPrice", b.unitPrice)
                            put("totalValue", b.totalValue)
                            put("isBest", b.isBest)
                            put("imageUrl", b.imageUrl)
                            put("grade", b.grade)
                        }
                        bArr.put(bObj)
                    }
                    put("bullets", bArr)
                }
                array.put(pObj)
            }
            root.put("packs", array)

            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_BULLET_JSON, root.toString())
                .putLong(KEY_BULLET_TIME, System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 解析 HTML / 流式数据中的实时配方
     */
    private fun parseCraftFromHtml(html: String): List<CraftRecipe>? {
        return try {
            val itemRegex = Regex("""\\"game_name\\":\\"([^\\"]+)\\",\\"grade\\":(\d+),\\"pic\\":\\"([^\\"]+)\\",\\"profit\\":(-?\d+),\\"profit_per_hour\\":(-?\d+),\\"duration_seconds\\":(\d+)""")
            val matches = itemRegex.findAll(html)
            val list = mutableListOf<CraftRecipe>()
            var idx = 0

            for (m in matches) {
                val name = m.groupValues[1]
                val grade = m.groupValues[2].toIntOrNull() ?: 4
                val pic = m.groupValues[3]
                val profit = m.groupValues[4].toLongOrNull() ?: 0L
                val hourlyProfit = m.groupValues[5].toLongOrNull() ?: 0L
                val durationSec = m.groupValues[6].toIntOrNull() ?: 3600
                val durationHours = Math.max(1, Math.round(durationSec / 3600.0).toInt())

                val (benchType, benchName) = classifyBench(name, pic)

                list.add(
                    CraftRecipe(
                        id = "craft_live_${idx++}",
                        name = name,
                        benchType = benchType,
                        benchName = benchName,
                        durationHours = durationHours,
                        totalProfit = profit,
                        hourlyProfit = hourlyProfit,
                        cost = 0L,
                        revenue = profit,
                        materials = emptyList(),
                        recommendedLevel = Math.max(1, grade - 2),
                        imageUrl = pic,
                        grade = grade
                    )
                )
            }

            if (list.isNotEmpty()) {
                list.distinctBy { it.name }
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseRecipesFromJson(jsonStr: String): List<CraftRecipe> {
        val list = mutableListOf<CraftRecipe>()
        try {
            val json = JSONObject(jsonStr)
            val array = json.optJSONArray("recipes") ?: return emptyList()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val typeStr = obj.optString("benchType", "ALL")
                val benchType = try {
                    CraftBenchType.valueOf(typeStr)
                } catch (e: Exception) {
                    CraftBenchType.ALL
                }

                val matsArray = obj.optJSONArray("materials")
                val materials = mutableListOf<String>()
                if (matsArray != null) {
                    for (m in 0 until matsArray.length()) {
                        materials.add(matsArray.optString(m))
                    }
                }

                val recipeName = obj.optString("name", "未命名配方")
                val rawRecipeImg = obj.optString("imageUrl", "")
                val recipeImg = if (rawRecipeImg.isNotBlank()) rawRecipeImg else GunsmithBuildRepository.getWeaponImageUrl(recipeName)

                list.add(
                    CraftRecipe(
                        id = obj.optString("id", "recipe_$i"),
                        name = recipeName,
                        benchType = benchType,
                        benchName = obj.optString("benchName", "工作台"),
                        durationHours = obj.optInt("durationHours", 1),
                        totalProfit = obj.optLong("totalProfit", 0L),
                        hourlyProfit = obj.optLong("hourlyProfit", 0L),
                        cost = obj.optLong("cost", 0L),
                        revenue = obj.optLong("revenue", 0L),
                        materials = materials,
                        recommendedLevel = obj.optInt("recommendedLevel", 1),
                        imageUrl = recipeImg,
                        grade = obj.optInt("grade", 4)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseBulletPacksFromJson(jsonStr: String): List<BulletPack> {
        val list = mutableListOf<BulletPack>()
        try {
            val json = JSONObject(jsonStr)
            val array = json.optJSONArray("packs") ?: return emptyList()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val bulletsArray = obj.optJSONArray("bullets")
                val bullets = mutableListOf<BulletPackItem>()

                if (bulletsArray != null) {
                    for (b in 0 until bulletsArray.length()) {
                        val bObj = bulletsArray.getJSONObject(b)
                        val bName = bObj.optString("name", "子弹")
                        val bCaliber = bObj.optString("caliber", "")
                        val rawImg = bObj.optString("imageUrl", "")
                        val finalImg = if (rawImg.isNotBlank()) rawImg else getAmmoImageUrl(bName, bCaliber)
                        val grade = if (bObj.has("grade")) bObj.optInt("grade", 4) else getAmmoGrade(bName, bCaliber)

                        bullets.add(
                            BulletPackItem(
                                rank = bObj.optInt("rank", b + 1),
                                name = bName,
                                caliber = bCaliber,
                                count = bObj.optInt("count", 0),
                                unitPrice = bObj.optLong("unitPrice", 0L),
                                totalValue = bObj.optLong("totalValue", 0L),
                                isBest = bObj.optBoolean("isBest", false),
                                imageUrl = finalImg,
                                grade = grade
                            )
                        )
                    }
                }

                val packName = obj.optString("packName", "自选包")
                val rawPackImg = obj.optString("imageUrl", "")
                val finalPackImg = if (rawPackImg.isNotBlank()) rawPackImg else "https://playerhub.df.qq.com/playerhub/60004/object/32230000010.png"

                list.add(
                    BulletPack(
                        id = obj.optString("id", "pack_$i"),
                        packName = packName,
                        recommendedBullet = obj.optString("recommendedBullet", ""),
                        recommendedValue = obj.optLong("recommendedValue", 0L),
                        bullets = bullets,
                        imageUrl = finalPackImg
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun loadRecipesFromAssets(): List<CraftRecipe> {
        return try {
            val content = context.assets.open("craft_profit.json").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, "UTF-8")).readText()
            }
            parseRecipesFromJson(content)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun loadBulletPacksFromAssets(): List<BulletPack> {
        return try {
            val content = context.assets.open("bullet_pack.json").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, "UTF-8")).readText()
            }
            parseBulletPacksFromJson(content)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
