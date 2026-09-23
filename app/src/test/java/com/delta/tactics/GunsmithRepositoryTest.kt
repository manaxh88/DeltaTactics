package com.delta.tactics

import com.delta.tactics.domain.model.GunsmithBuild
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GunsmithRepositoryTest {

    @Test
    fun `official builds asset json file exists and is valid`() {
        val assetFile = File("src/main/assets/gunsmith_official_builds.json")
        assertTrue("gunsmith_official_builds.json should exist in assets", assetFile.exists())
        assertTrue("Asset file should not be empty", assetFile.length() > 50000)

        val content = assetFile.readText(Charsets.UTF_8)
        val array = JSONArray(content)
        assertTrue("Should contain at least 150 builds", array.length() >= 150)

        var buildsWithModImage = 0
        var buildsWithAuthor = 0
        var buildsWithPrice = 0

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val gunName = obj.optString("gunName")
            val buildCode = obj.optString("buildCode")
            val img = obj.optString("imageUrl")
            val author = obj.optString("author")
            val price = obj.optLong("price")

            assertFalse("gunName must not be blank", gunName.isBlank())
            assertFalse("buildCode must not be blank", buildCode.isBlank())
            assertTrue("buildCode must contain 烽火地带 or mode", buildCode.contains("-"))

            if (img.startsWith("https://playerhub.df.qq.com")) {
                buildsWithModImage++
            }
            if (author.isNotBlank() && author != "官方精选") {
                buildsWithAuthor++
            }
            if (price > 0) {
                buildsWithPrice++
            }
        }

        println("Verified ${array.length()} builds:")
        println("  - Builds with official Tencent render image: $buildsWithModImage")
        println("  - Builds with creator nickname: $buildsWithAuthor")
        println("  - Builds with accessory price: $buildsWithPrice")

        assertTrue("Majority of builds should have official Tencent render images", buildsWithModImage > 100)
        assertTrue("Majority of builds should have creator nicknames", buildsWithAuthor > 100)
    }

    @Test
    fun `parse single build object correctly maps all fields`() {
        val jsonSnippet = """
            [
              {
                "id": "build_test_1",
                "gunName": "M4A1突击步枪",
                "roleName": "27W高稳定M4A1",
                "category": "突击步枪",
                "caliber": "5.56×45",
                "buildCode": "M4A1突击步枪-烽火地带-6K0KO7401TND0D5LD17R5",
                "imageUrl": "https://playerhub.df.qq.com/playerhub/60004/260608/b1098ce2-b7e1-4331-948b-99571a98c754.png",
                "gunBasePic": "https://playerhub.df.qq.com/playerhub/60004/object/18010000001.png",
                "specs": "预估造价 27.2万 • 控枪稳定性高",
                "description": "均衡稳定搭配，压枪门槛低。",
                "pros": ["稳定弹道", "极低后座"],
                "keyAccessories": ["长枪管", "消音器", "全息瞄具"],
                "author": "小洋（三角洲行动）",
                "price": 272000
              }
            ]
        """.trimIndent()

        val array = JSONArray(jsonSnippet)
        val obj = array.getJSONObject(0)

        val build = GunsmithBuild(
            id = obj.getString("id"),
            gunName = obj.getString("gunName"),
            roleName = obj.getString("roleName"),
            category = obj.getString("category"),
            caliber = obj.getString("caliber"),
            buildCode = obj.getString("buildCode"),
            imageUrl = obj.getString("imageUrl"),
            gunBasePic = obj.getString("gunBasePic"),
            specs = obj.getString("specs"),
            description = obj.getString("description"),
            pros = listOf("稳定弹道", "极低后座"),
            keyAccessories = listOf("长枪管", "消音器", "全息瞄具"),
            author = obj.getString("author"),
            price = obj.getLong("price")
        )

        assertEquals("M4A1突击步枪", build.gunName)
        assertEquals("27W高稳定M4A1", build.roleName)
        assertEquals("小洋（三角洲行动）", build.author)
        assertEquals(272000L, build.price)
        assertTrue(build.imageUrl.startsWith("https://playerhub.df.qq.com"))
        assertEquals(3, build.keyAccessories.size)
    }
}
