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

    @Test
    fun `classifyBench accurately assigns ammo, injections, armor and weapons without false positives`() {
        // 1. 验证子弹与箭矢 (必须归为 AMMO / 弹药台，绝不能混入枪械台)
        val ammoCases = listOf(
            Triple("45-70 Govt FMJ", "https://playerhub.df.qq.com/playerhub/60004/object/37290400001.png", "37290400001"),
            Triple("45-70 Govt FTX", "https://playerhub.df.qq.com/playerhub/60004/object/37290500001.png", "37290500001"),
            Triple("碳纤维刺骨箭矢", "https://playerhub.df.qq.com/playerhub/60004/object/37270400001.png", "37270400001"),
            Triple("玻纤柳叶箭矢", "https://playerhub.df.qq.com/playerhub/60004/object/37270300001.png", "37270300001"),
            Triple(".357 Magnum FMJ", "https://playerhub.df.qq.com/playerhub/60004/object/gun/ammo/.357.png", "37220400001"),
            Triple("5.45x39mm BT", "https://playerhub.df.qq.com/playerhub/60004/object/gun/ammo/5.45x39.png", "37120400001"),
            Triple("12 Gauge 独头 AP-20", "https://playerhub.df.qq.com/playerhub/60004/object/gun/ammo/12-Gauge.png", "37250400002")
        )

        for ((name, pic, id) in ammoCases) {
            val (benchType, benchName) = com.delta.tactics.data.repository.ProfitRepository.classifyBench(name, pic, id)
            assertEquals("Item $name should be AMMO bench", CraftBenchType.AMMO, benchType)
            assertEquals("Item $name bench name should be 弹药台", "弹药台", benchName)

            // 测试即使 ID 为空，通过 pic 与 name 兜底也必须正确归类
            val (fallbackType, _) = com.delta.tactics.data.repository.ProfitRepository.classifyBench(name, pic, "")
            assertEquals("Item $name fallback should be AMMO bench", CraftBenchType.AMMO, fallbackType)
        }

        // 2. 验证医疗与针剂 (必须归为 MEDICAL / 医疗台，绝不能混入枪械台)
        val medCases = listOf(
            Triple("OE2战斗兴奋剂", "https://playerhub.df.qq.com/playerhub/60004/object/14070000008.png", "14070000008"),
            Triple("体能激活针", "https://playerhub.df.qq.com/playerhub/60004/object/14070000006.png", "14070000006"),
            Triple("感知激活针", "https://playerhub.df.qq.com/playerhub/60004/object/14070000007.png", "14070000007"),
            Triple("M1肌肉强化针", "https://playerhub.df.qq.com/playerhub/60004/object/14070000004.png", "14070000004"),
            Triple("去甲肾上腺素", "https://playerhub.df.qq.com/playerhub/60004/object/14070000001.png", "14070000001"),
            Triple("战地医疗箱", "https://playerhub.df.qq.com/playerhub/60004/object/14010000005.png", "14010000005"),
            Triple("战地自愈全效注射剂", "https://playerhub.df.qq.com/playerhub/60004/object/14020000006.png", "14020000006")
        )

        for ((name, pic, id) in medCases) {
            val (benchType, benchName) = com.delta.tactics.data.repository.ProfitRepository.classifyBench(name, pic, id)
            assertEquals("Item $name should be MEDICAL bench", CraftBenchType.MEDICAL, benchType)
            assertEquals("Item $name bench name should be 医疗台", "医疗台", benchName)

            val (fallbackType, _) = com.delta.tactics.data.repository.ProfitRepository.classifyBench(name, pic, "")
            assertEquals("Item $name fallback should be MEDICAL bench", CraftBenchType.MEDICAL, fallbackType)
        }

        // 3. 验证防具与头盔背心
        val armorCases = listOf(
            Triple("GN 久战重型夜视头盔", "https://playerhub.df.qq.com/playerhub/60004/object/11010005010.png", "11010005010"),
            Triple("MK-2战术背心", "https://playerhub.df.qq.com/playerhub/60004/object/11050004004.png", "11050004004"),
            Triple("DT-AVS防弹衣", "https://playerhub.df.qq.com/playerhub/60004/object/11050005008.png", "11050005008")
        )

        for ((name, pic, id) in armorCases) {
            val (benchType, _) = com.delta.tactics.data.repository.ProfitRepository.classifyBench(name, pic, id)
            assertEquals("Item $name should be ARMOR bench", CraftBenchType.ARMOR, benchType)
        }

        // 4. 验证枪械与配件 (归为 WEAPON / 枪械台)
        val weaponCases = listOf(
            Triple("M14射手步枪", "https://eo.oss.hengj.cn/one/object/18050000005.png", "18050000005"),
            Triple("AUG突击步枪", "https://eo.oss.hengj.cn/one/object/18010000038.png", "18010000038"),
            Triple("灵眼3/7弹道计算狙击镜", "https://eo.oss.hengj.cn/one/object/13110000091.png", "13110000091"),
            Triple("骨架狙击枪托", "https://eo.oss.hengj.cn/one/object/13040000185.png", "13040000185")
        )

        for ((name, pic, id) in weaponCases) {
            val (benchType, _) = com.delta.tactics.data.repository.ProfitRepository.classifyBench(name, pic, id)
            assertEquals("Item $name should be WEAPON bench", CraftBenchType.WEAPON, benchType)
        }
    }
}
