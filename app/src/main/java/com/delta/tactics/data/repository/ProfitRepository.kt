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
        private const val KEY_CRAFT_JSON = "cached_craft_profit_json_v4"
        private const val KEY_CRAFT_TIME = "cached_craft_profit_time_v4"
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

        /**
         * 工作台精准归类逻辑:
         * 1. 腾讯三角洲官方物品编码前缀判定:
         *    - 11xxx: 防具台 (头盔、防弹衣、胸挂等)
         *    - 14xxx: 医疗台 (战斗兴奋剂、体能激活针、强化针、止血药、医疗箱等)
         *    - 37xxx: 弹药台 (全口径子弹、各类箭矢、独头弹等)
         *    - 18xxx: 枪械台 (整枪制造)
         *    - 13xxx: 枪械台 (枪械机匣、消音器、瞄准镜、枪托、导轨等配件)
         * 2. 官方图片 URL 路由特征判定
         * 3. 语义与关键词智能兜底判定
         */
        fun classifyBench(name: String, pic: String, id: String = ""): Pair<CraftBenchType, String> {
            val cleanId = id.trim()
            if (cleanId.startsWith("11")) return CraftBenchType.ARMOR to "防具台"
            if (cleanId.startsWith("14")) return CraftBenchType.MEDICAL to "医疗台"
            if (cleanId.startsWith("37")) return CraftBenchType.AMMO to "弹药台"
            if (cleanId.startsWith("18") || cleanId.startsWith("13")) return CraftBenchType.WEAPON to "枪械台"

            if (pic.contains("/110") || pic.contains("/111") || pic.contains("/object/11")) return CraftBenchType.ARMOR to "防具台"
            if (pic.contains("/140") || pic.contains("/147") || pic.contains("/object/14")) return CraftBenchType.MEDICAL to "医疗台"
            if (pic.contains("/37") || pic.contains("/object/37") || pic.contains("/gun/ammo")) return CraftBenchType.AMMO to "弹药台"
            if (pic.contains("/18") || pic.contains("/13") || pic.contains("/object/18") || pic.contains("/object/13")) return CraftBenchType.WEAPON to "枪械台"

            val lower = name.lowercase()
            return when {
                // 1. 弹药 / 箭矢
                name.contains("弹") || name.contains("箭") || name.contains("矢") ||
                lower.contains("mm") || lower.contains("gauge") || lower.contains("acp") ||
                lower.contains("blk") || lower.contains("fmj") || lower.contains("ftx") ||
                lower.contains("rip") || lower.contains("ap") || lower.contains("hp") ||
                lower.contains("sp") || lower.contains("bt") || lower.contains("lps") ||
                lower.contains("m80") || lower.contains("m61") || lower.contains("m62") ||
                lower.contains("m995") || lower.contains("45-70") || lower.contains(".357") ||
                lower.contains(".50") || name.contains("×") || (lower.contains("x") && (lower.contains("7.62") || lower.contains("5.56") || lower.contains("5.45") || lower.contains("9x") || lower.contains("5.7") || lower.contains("5.8") || lower.contains("4.6") || lower.contains("12.7"))) ->
                    CraftBenchType.AMMO to "弹药台"

                // 2. 医疗 / 针剂 / 药剂
                name.contains("针") || name.contains("剂") || name.contains("药") ||
                name.contains("素") || name.contains("包") || name.contains("医") ||
                name.contains("痛") || name.contains("手术") || name.contains("急救") ||
                name.contains("绷带") || name.contains("血清") || name.contains("激活") ->
                    CraftBenchType.MEDICAL to "医疗台"

                // 3. 防具 / 头盔 / 背心 / 胸挂
                name.contains("甲") || name.contains("头盔") || name.contains("背心") ||
                name.contains("胸挂") || name.contains("防弹") || name.contains("面罩") ||
                name.contains("插板") || name.contains("夜视") ->
                    CraftBenchType.ARMOR to "防具台"

                // 4. 枪械与配件
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
            val itemRegexWithId = Regex("""\\"object_id\\":(\d+),\\"game_name\\":\\"([^\\"]+)\\",\\"grade\\":(\d+),\\"pic\\":\\"([^\\"]+)\\",\\"profit\\":(-?\d+),\\"profit_per_hour\\":(-?\d+),\\"duration_seconds\\":(\d+)""")
            val itemRegexFallback = Regex("""\\"game_name\\":\\"([^\\"]+)\\",\\"grade\\":(\d+),\\"pic\\":\\"([^\\"]+)\\",\\"profit\\":(-?\d+),\\"profit_per_hour\\":(-?\d+),\\"duration_seconds\\":(\d+)""")

            val list = mutableListOf<CraftRecipe>()
            var idx = 0

            val matchesWithId = itemRegexWithId.findAll(html).toList()
            if (matchesWithId.isNotEmpty()) {
                for (m in matchesWithId) {
                    val idStr = m.groupValues[1]
                    val name = m.groupValues[2]
                    val grade = m.groupValues[3].toIntOrNull() ?: 4
                    val pic = m.groupValues[4]
                    val profit = m.groupValues[5].toLongOrNull() ?: 0L
                    val hourlyProfit = m.groupValues[6].toLongOrNull() ?: 0L
                    val durationSec = m.groupValues[7].toIntOrNull() ?: 3600
                    val durationHours = Math.max(1, Math.round(durationSec / 3600.0).toInt())

                    val (benchType, benchName) = classifyBench(name, pic, idStr)

                    list.add(
                        CraftRecipe(
                            id = "craft_live_$idStr",
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
            } else {
                for (m in itemRegexFallback.findAll(html)) {
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
                val recipeId = obj.optString("id", "recipe_$i")

                // 进行精准校验与纠偏，杜绝子弹/针剂误归入枪械台
                val (classifiedType, classifiedName) = classifyBench(recipeName, recipeImg, recipeId)
                val finalBenchType = if (benchType == CraftBenchType.ALL || (benchType == CraftBenchType.WEAPON && classifiedType != CraftBenchType.WEAPON)) {
                    classifiedType
                } else benchType

                val finalBenchName = if (benchType == CraftBenchType.ALL || (benchType == CraftBenchType.WEAPON && classifiedType != CraftBenchType.WEAPON)) {
                    classifiedName
                } else obj.optString("benchName", classifiedName)

                list.add(
                    CraftRecipe(
                        id = recipeId,
                        name = recipeName,
                        benchType = finalBenchType,
                        benchName = finalBenchName,
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
