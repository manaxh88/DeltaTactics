package com.delta.tactics.data.repository

import android.content.Context
import com.delta.tactics.domain.model.BulletPack
import com.delta.tactics.domain.model.BulletPackItem
import com.delta.tactics.domain.model.CraftBenchType
import com.delta.tactics.domain.model.CraftRecipe
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class ProfitRepository(private val context: Context) {

    private var cachedRecipes: List<CraftRecipe>? = null
    private var cachedBulletPacks: List<BulletPack>? = null

    fun getCraftRecipes(): List<CraftRecipe> {
        cachedRecipes?.let { return it }
        val loaded = loadRecipesFromAssets()
        cachedRecipes = loaded
        return loaded
    }

    fun getBulletPacks(): List<BulletPack> {
        cachedBulletPacks?.let { return it }
        val loaded = loadBulletPacksFromAssets()
        cachedBulletPacks = loaded
        return loaded
    }

    private fun loadRecipesFromAssets(): List<CraftRecipe> {
        return try {
            val content = context.assets.open("craft_profit.json").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, "UTF-8")).readText()
            }
            val json = JSONObject(content)
            val array = json.optJSONArray("recipes") ?: return emptyList()
            val list = mutableListOf<CraftRecipe>()

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

                list.add(
                    CraftRecipe(
                        id = obj.optString("id", "recipe_$i"),
                        name = obj.optString("name", "未命名配方"),
                        benchType = benchType,
                        benchName = obj.optString("benchName", "工作台"),
                        durationHours = obj.optInt("durationHours", 1),
                        totalProfit = obj.optLong("totalProfit", 0L),
                        hourlyProfit = obj.optLong("hourlyProfit", 0L),
                        cost = obj.optLong("cost", 0L),
                        revenue = obj.optLong("revenue", 0L),
                        materials = materials,
                        recommendedLevel = obj.optInt("recommendedLevel", 1)
                    )
                )
            }
            list
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
            val json = JSONObject(content)
            val array = json.optJSONArray("packs") ?: return emptyList()
            val list = mutableListOf<BulletPack>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val bulletsArray = obj.optJSONArray("bullets")
                val bullets = mutableListOf<BulletPackItem>()

                if (bulletsArray != null) {
                    for (b in 0 until bulletsArray.length()) {
                        val bObj = bulletsArray.getJSONObject(b)
                        bullets.add(
                            BulletPackItem(
                                rank = bObj.optInt("rank", b + 1),
                                name = bObj.optString("name", "子弹"),
                                caliber = bObj.optString("caliber", ""),
                                count = bObj.optInt("count", 0),
                                unitPrice = bObj.optLong("unitPrice", 0L),
                                totalValue = bObj.optLong("totalValue", 0L),
                                isBest = bObj.optBoolean("isBest", false)
                            )
                        )
                    }
                }

                list.add(
                    BulletPack(
                        id = obj.optString("id", "pack_$i"),
                        packName = obj.optString("packName", "自选包"),
                        recommendedBullet = obj.optString("recommendedBullet", ""),
                        recommendedValue = obj.optLong("recommendedValue", 0L),
                        bullets = bullets
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
