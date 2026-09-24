package com.delta.tactics

import com.delta.tactics.domain.model.BulletPack
import com.delta.tactics.domain.model.BulletPackItem
import com.delta.tactics.domain.model.CraftBenchType
import com.delta.tactics.domain.model.CraftRecipe
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProfitRepositoryTest {

    @Test
    fun `craft_profit asset json exists, valid and all items have official images and grade`() {
        val assetFile = File("src/main/assets/craft_profit.json")
        assertTrue("craft_profit.json should exist in assets", assetFile.exists())

        val content = assetFile.readText(Charsets.UTF_8)
        val root = JSONObject(content)
        assertTrue(root.has("updateTime"))
        val array = root.getJSONArray("recipes")
        assertTrue("craft_profit should contain recipes", array.length() >= 15)

        var withImage = 0
        var withGrade = 0

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val name = obj.getString("name")
            val benchType = obj.getString("benchType")
            val totalProfit = obj.getLong("totalProfit")
            val hourlyProfit = obj.getLong("hourlyProfit")
            val imageUrl = obj.optString("imageUrl")
            val grade = obj.optInt("grade", 0)

            assertFalse("Recipe name should not be blank", name.isBlank())
            assertFalse("benchType should not be blank", benchType.isBlank())
            assertTrue("totalProfit should be positive", totalProfit > 0)
            assertTrue("hourlyProfit should be positive", hourlyProfit > 0)

            if (imageUrl.startsWith("https://playerhub.df.qq.com")) {
                withImage++
            }
            if (grade in 3..6) {
                withGrade++
            }
        }

        println("Verified ${array.length()} craft recipes:")
        println("  - Recipes with official Tencent CDN image: $withImage")
        println("  - Recipes with valid grade (3..6): $withGrade")

        assertEquals("All recipes must have official Tencent CDN images", array.length(), withImage)
        assertEquals("All recipes must have valid quality grade", array.length(), withGrade)
    }

    @Test
    fun `bullet_pack asset json exists, valid and all items have 3D ammo box images and grade`() {
        val assetFile = File("src/main/assets/bullet_pack.json")
        assertTrue("bullet_pack.json should exist in assets", assetFile.exists())

        val content = assetFile.readText(Charsets.UTF_8)
        val root = JSONObject(content)
        assertTrue(root.has("updateTime"))
        val array = root.getJSONArray("packs")
        assertEquals("bullet_pack should contain 4 tiers of packs", 4, array.length())

        var totalBullets = 0
        var bulletsWithImage = 0
        var bulletsWithGrade = 0

        for (i in 0 until array.length()) {
            val packObj = array.getJSONObject(i)
            val packName = packObj.getString("packName")
            val recommendedBullet = packObj.getString("recommendedBullet")
            val recommendedValue = packObj.getLong("recommendedValue")
            val packImageUrl = packObj.optString("imageUrl")
            val bulletsArray = packObj.getJSONArray("bullets")

            assertFalse("packName should not be blank", packName.isBlank())
            assertFalse("recommendedBullet should not be blank", recommendedBullet.isBlank())
            assertTrue("recommendedValue should be positive", recommendedValue > 0)
            assertTrue("packImageUrl should be Tencent CDN chest", packImageUrl.startsWith("https://playerhub.df.qq.com"))

            var foundBest = false
            for (j in 0 until bulletsArray.length()) {
                totalBullets++
                val bObj = bulletsArray.getJSONObject(j)
                val rank = bObj.getInt("rank")
                val bName = bObj.getString("name")
                val unitPrice = bObj.getLong("unitPrice")
                val count = bObj.getInt("count")
                val totalValue = bObj.getLong("totalValue")
                val isBest = bObj.getBoolean("isBest")
                val imageUrl = bObj.optString("imageUrl")
                val grade = bObj.optInt("grade", 0)

                assertEquals(unitPrice * count, totalValue)
                if (isBest) {
                    foundBest = true
                    assertEquals(recommendedBullet, bName)
                    assertEquals(recommendedValue, totalValue)
                }
                if (imageUrl.startsWith("https://playerhub.df.qq.com/playerhub/60004/object/gun/ammo/")) {
                    bulletsWithImage++
                }
                if (grade in 3..6) {
                    bulletsWithGrade++
                }
            }
            assertTrue("Each pack must have exactly one isBest bullet", foundBest)
        }

        println("Verified $totalBullets bullets across 4 packs:")
        println("  - Bullets with official 3D caliber image: $bulletsWithImage")
        println("  - Bullets with grade: $bulletsWithGrade")

        assertEquals("All bullets must have 3D ammo box images", totalBullets, bulletsWithImage)
        assertEquals("All bullets must have quality grade", totalBullets, bulletsWithGrade)
    }
}
