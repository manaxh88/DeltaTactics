package com.delta.tactics.data.repository

import android.content.Context
import com.delta.tactics.domain.model.GunsmithBuild
import com.delta.tactics.domain.model.GunsmithBuildRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 枪械改枪配装仓库（抄作业数据包）
 *
 * 采用三级缓存策略：
 * 1. 内存运行时缓存 (Memory Cache)
 * 2. 本地持久化缓存 (SharedPreferences Disk Cache)
 * 3. Assets 离线底包种子 (Assets Seed, 71种武器 188+ 套方案)
 * 4. 腾讯社区/鼠鼠官方 API 动态同步 (Remote Official API)
 */
class GunsmithRepository(private val context: Context) {

    private val prefs by lazy {
        context.getSharedPreferences("gunsmith_builds_cache", Context.MODE_PRIVATE)
    }

    private var memoryCache: List<GunsmithBuild>? = null

    companion object {
        private const val CACHE_DURATION_MS = 2 * 60 * 60 * 1000L // 2 小时缓存失效
        private const val KEY_CACHE_JSON = "cached_gunsmith_json"
        private const val KEY_CACHE_TIME = "cached_gunsmith_time"
        private const val OFFICIAL_API_URL = "https://www.shushu.fan/api/guns-code/official"

        fun normalizeCategory(gunName: String, rawCategory: String): String {
            if (gunName.contains("狙击步枪") || gunName.contains("AWM") || gunName.contains("M700") || gunName.contains("SV-98")) {
                return "狙击步枪"
            }
            if (gunName.contains("射手步枪") || gunName.contains("VSS") || gunName.contains("SVD") || 
                gunName.contains("M14") || gunName.contains("Mini-14") || gunName.contains("SR-25")) {
                return "射手步枪"
            }
            if (gunName.contains("轻机枪") || gunName.contains("机枪") || gunName.contains("霰弹枪") || 
                gunName.contains("M249") || gunName.contains("PKM") || gunName.contains("725")) {
                return "轻机枪/霰弹"
            }
            if (gunName.contains("冲锋枪") || gunName.contains("SMG") || gunName.contains("UZI") || 
                gunName.contains("MP5") || gunName.contains("Vector") || gunName.contains("P90") || 
                gunName.contains("勇士") || gunName.contains("野牛") || gunName.contains("MK4")) {
                return "冲锋枪"
            }
            if (gunName.contains("突击步枪") || gunName.contains("战斗步枪") || 
                gunName.contains("CAR-15") || gunName.contains("AKS-74U") || 
                gunName.contains("AR57") || gunName.contains("M4A1") ||
                gunName.contains("K416") || gunName.contains("M7") || 
                gunName.contains("AKM") || gunName.contains("AS Val") ||
                gunName.contains("SG552") || gunName.contains("QBZ") ||
                gunName.contains("AUG") || gunName.contains("G3") ||
                gunName.contains("K437") || gunName.contains("PTR-32") ||
                gunName.contains("MCX") || gunName.contains("MDR") ||
                gunName.contains("ASh-12") || gunName.contains("AK-12")) {
                return "突击步枪"
            }
            if (rawCategory.isNotBlank() && rawCategory != "手枪/特种") {
                return rawCategory
            }
            return if (gunName.contains("手枪") || gunName.contains("93R") || gunName.contains("G18") || gunName.contains("沙漠之鹰") || gunName.contains(".357")) "手枪/特种" else "突击步枪"
        }
    }

    /**
     * 同步获取当前可用改枪方案（内存 -> 本地持久化缓存 -> Assets 种子 -> 兜底硬编码）
     */
    fun getBuilds(): List<GunsmithBuild> {
        memoryCache?.let { if (it.isNotEmpty()) return it }

        val assetList = loadFromAssets()

        // 1. 尝试从本地 SharedPreferences 读取 (仅当缓存数不低于离线底包时采纳)
        val savedJson = prefs.getString(KEY_CACHE_JSON, null)
        if (!savedJson.isNullOrEmpty()) {
            val list = parseBuildsJson(savedJson)
            if (list.size >= assetList.size && list.isNotEmpty()) {
                memoryCache = list
                return list
            }
        }

        // 2. 优先采用更新更大的 Assets 离线底包
        if (assetList.isNotEmpty()) {
            memoryCache = assetList
            return assetList
        }

        // 3. 兜底硬编码
        val fallback = GunsmithBuildRepository.POPULAR_BUILDS
        memoryCache = fallback
        return fallback
    }

    /**
     * 判断缓存是否已过期
     */
    fun isCacheExpired(): Boolean {
        val savedTime = prefs.getLong(KEY_CACHE_TIME, 0L)
        return (System.currentTimeMillis() - savedTime) >= CACHE_DURATION_MS
    }

    /**
     * 从远程官方 API 增量/全量拉取最新热门改枪方案
     */
    suspend fun fetchOfficialBuilds(force: Boolean = false): List<GunsmithBuild> = withContext(Dispatchers.IO) {
        if (!force && !isCacheExpired() && memoryCache?.isNotEmpty() == true) {
            return@withContext memoryCache ?: emptyList()
        }

        try {
            // 拉取第 1 页 50 套最新方案
            val url = URL("$OFFICIAL_API_URL?page=1&limit=50")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "DeltaTacticsApp/3.0")
            }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val remoteBuilds = parseRemoteOfficialApiResponse(response)
                if (remoteBuilds.isNotEmpty()) {
                    // 与本地现有方案合并去重（以 code 为 key）
                    val currentList = getBuilds()
                    val mergedMap = LinkedHashMap<String, GunsmithBuild>()
                    remoteBuilds.forEach { mergedMap[it.buildCode] = it }
                    currentList.forEach { if (!mergedMap.containsKey(it.buildCode)) mergedMap[it.buildCode] = it }

                    val finalMerged = mergedMap.values.toList()
                    saveToCache(finalMerged)
                    memoryCache = finalMerged
                    return@withContext finalMerged
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext getBuilds()
    }

    /**
     * 按枪械名称定向搜索/拉取该武器的大神改枪方案
     */
    suspend fun searchBuildsByWeapon(weaponName: String): List<GunsmithBuild> = withContext(Dispatchers.IO) {
        val cleanName = weaponName.trim()
        if (cleanName.isBlank()) return@withContext getBuilds()

        // 先在本地过滤
        val localMatches = getBuilds().filter {
            it.gunName.contains(cleanName, ignoreCase = true) ||
            it.roleName.contains(cleanName, ignoreCase = true) ||
            it.category.contains(cleanName, ignoreCase = true)
        }

        // 如果本地找到了不少方案，或者武器名不是完整标准名，先返回本地结果
        if (localMatches.size >= 3) {
            return@withContext localMatches
        }

        // 尝试从远程查该武器
        try {
            val encoded = URLEncoder.encode(cleanName, "UTF-8")
            val url = URL("$OFFICIAL_API_URL?weaponNames=$encoded&limit=10")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "DeltaTacticsApp/3.0")
            }
            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val remoteList = parseRemoteOfficialApiResponse(response)
                if (remoteList.isNotEmpty()) {
                    return@withContext remoteList
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext localMatches
    }

    private fun loadFromAssets(): List<GunsmithBuild> {
        return try {
            val content = context.assets.open("gunsmith_official_builds.json").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, "UTF-8")).readText()
            }
            parseBuildsJson(content)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun saveToCache(builds: List<GunsmithBuild>) {
        try {
            val jsonArray = JSONArray()
            for (b in builds) {
                val obj = JSONObject().apply {
                    put("id", b.id)
                    put("gunName", b.gunName)
                    put("roleName", b.roleName)
                    put("category", b.category)
                    put("caliber", b.caliber)
                    put("buildCode", b.buildCode)
                    put("imageUrl", b.imageUrl)
                    put("gunBasePic", b.gunBasePic)
                    put("specs", b.specs)
                    put("description", b.description)
                    put("author", b.author)
                    put("price", b.price)

                    val prosArr = JSONArray()
                    b.pros.forEach { prosArr.put(it) }
                    put("pros", prosArr)

                    val accArr = JSONArray()
                    b.keyAccessories.forEach { accArr.put(it) }
                    put("keyAccessories", accArr)
                }
                jsonArray.put(obj)
            }
            prefs.edit()
                .putString(KEY_CACHE_JSON, jsonArray.toString())
                .putLong(KEY_CACHE_TIME, System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 解析内置 JSON 格式
     */
    fun parseBuildsJson(jsonString: String): List<GunsmithBuild> {
        val result = mutableListOf<GunsmithBuild>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val prosList = mutableListOf<String>()
                val prosArr = obj.optJSONArray("pros")
                if (prosArr != null) {
                    for (p in 0 until prosArr.length()) {
                        prosList.add(prosArr.getString(p))
                    }
                }

                val accList = mutableListOf<String>()
                val accArr = obj.optJSONArray("keyAccessories")
                if (accArr != null) {
                    for (a in 0 until accArr.length()) {
                        accList.add(accArr.getString(a))
                    }
                }

                val gunName = obj.optString("gunName", "未知武器")
                val rawBuildCode = obj.optString("buildCode", "").trim()
                val buildCode = if (rawBuildCode.contains("-")) rawBuildCode else "$gunName-烽火地带-$rawBuildCode"

                val build = GunsmithBuild(
                    id = obj.optString("id", "build_$i"),
                    gunName = gunName,
                    roleName = obj.optString("roleName", "官方精选方案"),
                    category = normalizeCategory(gunName, obj.optString("category", "突击步枪")),
                    caliber = obj.optString("caliber", "通用"),
                    buildCode = buildCode,
                    imageUrl = obj.optString("imageUrl", ""),
                    gunBasePic = obj.optString("gunBasePic", ""),
                    specs = obj.optString("specs", ""),
                    description = obj.optString("description", ""),
                    pros = prosList,
                    keyAccessories = accList,
                    author = obj.optString("author", "官方精选"),
                    price = obj.optLong("price", 0L)
                )
                if (build.buildCode.isNotBlank()) {
                    result.add(build)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    /**
     * 解析远程 API 返回的 JSON 列表
     */
    private fun parseRemoteOfficialApiResponse(jsonStr: String): List<GunsmithBuild> {
        val result = mutableListOf<GunsmithBuild>()
        try {
            val root = JSONObject(jsonStr)
            if (!root.optBoolean("success", false)) return emptyList()
            val data = root.optJSONObject("data") ?: return emptyList()
            val list = data.optJSONArray("list") ?: return emptyList()

            for (i in 0 until list.length()) {
                val item = list.getJSONObject(i)
                val rawCode = item.optString("solutionCode", "").trim()
                if (rawCode.isBlank()) continue

                val armsDetail = item.optJSONObject("armsDetail") ?: JSONObject()
                val gunDetail = armsDetail.optJSONObject("gunDetail") ?: JSONObject()
                val gunName = armsDetail.optString("objectName", item.optString("name", "精选武器"))
                val code = if (rawCode.contains("-")) rawCode else "$gunName-烽火地带-$rawCode"
                val secClass = armsDetail.optString("secondClassCN", "")

                val rawCategory = when {
                    secClass.contains("突击步枪") || secClass.contains("战斗步枪") || secClass.contains("步枪") -> "突击步枪"
                    secClass.contains("冲锋枪") -> "冲锋枪"
                    secClass.contains("狙击步枪") -> "狙击步枪"
                    secClass.contains("射手步枪") -> "射手步枪"
                    secClass.contains("轻机枪") || secClass.contains("通用机枪") || secClass.contains("霰弹枪") -> "轻机枪/霰弹"
                    else -> "手枪/特种"
                }
                val category = normalizeCategory(gunName, rawCategory)

                val previewPic = item.optString("previewPic").ifBlank {
                    item.optString("prePreviewPic").ifBlank {
                        armsDetail.optString("pic", "")
                    }
                }

                val author = item.optString("authorNickname", "官方精选")
                val price = item.optLong("price", 0L)
                val buildTitle = item.optString("name", "$gunName 改装方案")

                val accList = mutableListOf<String>()
                val accArr = item.optJSONArray("accessoryDetail")
                if (accArr != null) {
                    for (a in 0 until accArr.length()) {
                        val accObj = accArr.optJSONObject(a)
                        val aName = accObj?.optString("name")
                        if (!aName.isNullOrBlank() && !accList.contains(aName)) {
                            accList.add(aName)
                        }
                    }
                }

                val tagsList = mutableListOf<String>()
                val tagsArr = item.optJSONArray("tagDetail")
                if (tagsArr != null) {
                    for (t in 0 until tagsArr.length()) {
                        val tagObj = tagsArr.optJSONObject(t)
                        val tName = tagObj?.optString("name")
                        if (!tName.isNullOrBlank()) {
                            tagsList.add(tName)
                        }
                    }
                }
                if (tagsList.isEmpty()) {
                    tagsList.addAll(listOf("官方精选", "实战推荐"))
                }

                val rawDesc = item.optString("authorComment", "")
                val cleanDesc = rawDesc.replace(Regex("<[^>]+>"), "").trim()

                val priceWan = price / 10000
                val priceQian = (price % 10000) / 1000
                val specs = if (price > 0) "预估造价 $priceWan.${priceQian}万 • 控枪稳定性高" else "官方赛事调校"

                result.add(
                    GunsmithBuild(
                        id = "remote_${item.optLong("id", i.toLong())}",
                        gunName = gunName,
                        roleName = buildTitle,
                        category = category,
                        caliber = gunDetail.optString("caliber", "通用").replace("ammo", ""),
                        buildCode = code,
                        imageUrl = previewPic,
                        gunBasePic = armsDetail.optString("pic", ""),
                        specs = specs,
                        description = cleanDesc.ifBlank { "由知名创作者【$author】调校打造的战术配装方案。" },
                        pros = tagsList.take(4),
                        keyAccessories = if (accList.isNotEmpty()) accList.take(6) else listOf("战术消音", "光学瞄具", "竞技握把", "扩容弹匣"),
                        author = author,
                        price = price
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }
}
